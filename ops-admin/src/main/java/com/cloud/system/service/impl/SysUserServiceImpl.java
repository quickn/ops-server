package com.cloud.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.system.common.constant.SecurityConstants;
import com.cloud.system.common.constant.SystemConstants;
import com.cloud.system.converter.UserConverter;
import com.cloud.system.common.util.LoginMaskUtils;
import com.cloud.system.common.util.SecurityUtils;
import com.cloud.system.mapper.SysUserMapper;
import com.cloud.system.model.dto.UserAuthInfo;
import com.cloud.system.model.bo.UserBO;
import com.cloud.system.model.bo.UserFormBO;
import com.cloud.system.model.entity.SysUser;
import com.cloud.system.model.form.UserForm;
import com.cloud.system.model.query.UserPageQuery;
import com.cloud.system.model.vo.UserExportVO;
import com.cloud.system.model.vo.UserInfoVO;
import com.cloud.system.model.vo.UserPageVO;
import com.cloud.system.service.SysMenuService;
import com.cloud.system.service.SysRoleService;
import com.cloud.system.service.SysUserRoleService;
import com.cloud.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户业务实现类
 *
 * @author haoxr
 * @since 2022/1/14
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;

    private final SysUserRoleService userRoleService;

    private final UserConverter userConverter;

    private final SysMenuService menuService;

    private final SysRoleService roleService;

    private final RedisTemplate redisTemplate;

    /**
     * 获取用户分页列表
     *
     * @param queryParams
     * @return
     */
    @Override
    public IPage<UserPageVO> getUserPage(UserPageQuery queryParams) {

        // 多租户数据隔离: 非超级管理员只能查看自己及自己创建的用户
        Long currentUserId = SecurityUtils.isRoot() ? null : SecurityUtils.getUserId();
        queryParams.setCurrentUserId(currentUserId);

        // 参数构建
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        Page<UserBO> page = new Page<>(pageNum, pageSize);

        // 查询数据
        Page<UserBO> userBoPage = this.baseMapper.getUserPage(page, queryParams);

        // 实体转换
        Page<UserPageVO> userVoPage = userConverter.bo2Vo(userBoPage);

        return userVoPage;
    }

    /**
     * 获取用户详情
     *
     * @param userId
     * @return
     */
    @Override
    public UserForm getUserFormData(Long userId) {
        // 多租户数据隔离: 非超级管理员只能查看自己及自己创建的用户
        Long currentUserId = SecurityUtils.isRoot() ? null : SecurityUtils.getUserId();
        UserFormBO userFormBO = this.baseMapper.getUserDetail(userId, currentUserId);
        // 实体转换po->form
        UserForm userForm = userConverter.bo2Form(userFormBO);
        return userForm;
    }

    /**
     * 校验当前登录用户对目标用户是否具有访问权限(多租户数据隔离)
     *
     * @param targetUserId 目标用户ID
     * @return true-有权限  false-无权限
     */
    @Override
    public boolean hasAccessPermission(Long targetUserId) {
        // 超级管理员(role_id=1)对所有用户有访问权限
        if (SecurityUtils.isRoot()) {
            return true;
        }
        // 目标用户ID为空，拒绝访问
        if (targetUserId == null) {
            return false;
        }
        // 自己对自己有访问权限
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId == null) {
            return false;
        }
        if (currentUserId.equals(targetUserId)) {
            return true;
        }
        // 自己创建的用户对自己可见(create_by = 当前用户ID)
        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, targetUserId)
                .eq(SysUser::getCreateBy, currentUserId));
        return count != null && count > 0;
    }

    /**
     * 新增用户
     *
     * @param userForm 用户表单对象
     * @return
     */
    @Override
    public boolean saveUser(UserForm userForm) {

        String username = userForm.getUsername();

        long count = this.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        Assert.isTrue(count == 0, "用户名已存在");

        // 校验登录掩码格式
        Assert.isTrue(LoginMaskUtils.isValid(userForm.getLoginMask()), "登录掩码格式不正确");

        // 多租户防御性校验: 非超级管理员不能给用户分配系统管理员角色(id=1)
        validateRootRoleAssignment(userForm.getRoleIds());

        // 实体转换 form->entity
        SysUser entity = userConverter.form2Entity(userForm);

        // 设置默认加密密码
        String defaultEncryptPwd = passwordEncoder.encode(SystemConstants.DEFAULT_PASSWORD);
        entity.setPassword(defaultEncryptPwd);

        // 新增用户
        boolean result = this.save(entity);

        if (result) {
            // 保存用户角色
            userRoleService.saveUserRoles(entity.getId(), userForm.getRoleIds());
        }
        return result;
    }

    /**
     * 更新用户
     *
     * @param userId   用户ID
     * @param userForm 用户表单对象
     * @return
     */
    @Override
    @Transactional
    public boolean updateUser(Long userId, UserForm userForm) {

        String username = userForm.getUsername();

        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .ne(SysUser::getId, userId)
        );
        Assert.isTrue(count == 0, "用户名已存在");

        // 校验登录掩码格式
        Assert.isTrue(LoginMaskUtils.isValid(userForm.getLoginMask()), "登录掩码格式不正确");

        // 多租户防御性校验: 非超级管理员不能给用户分配系统管理员角色(id=1)
        validateRootRoleAssignment(userForm.getRoleIds());

        // form -> entity
        SysUser entity = userConverter.form2Entity(userForm);

        // 修改用户
        boolean result = this.updateById(entity);

        if (result) {
            // 保存用户角色
            userRoleService.saveUserRoles(entity.getId(), userForm.getRoleIds());
        }
        return result;
    }

    /**
     * 校验非超级管理员不能给用户分配系统管理员角色(多租户防御性校验)
     *
     * @param roleIds 待分配的角色ID集合
     */
    private void validateRootRoleAssignment(List<Long> roleIds) {
        if (SecurityUtils.isRoot() || CollectionUtil.isEmpty(roleIds)) {
            return;
        }
        Assert.isTrue(!roleIds.contains(SystemConstants.ROOT_ROLE_ID),
                "无权分配系统管理员角色(角色ID=1)");
    }

    /**
     * 删除用户
     *
     * @param idsStr 用户ID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteUsers(String idsStr) {
        Assert.isTrue(StrUtil.isNotBlank(idsStr), "删除的用户数据为空");
        // 多租户数据隔离: 非超级管理员只能删除自己及自己创建的用户
        List<Long> ids = Arrays.asList(idsStr.split(",")).stream()
                .map(idStr -> Long.parseLong(idStr)).collect(Collectors.toList());
        if (!SecurityUtils.isRoot()) {
            Long currentUserId = SecurityUtils.getUserId();
            // 查询待删除用户中不属于当前租户范围的用户
            Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .in(SysUser::getId, ids)
                    .and(wrapper -> wrapper.ne(SysUser::getId, currentUserId)
                            .ne(SysUser::getCreateBy, currentUserId)));
            Assert.isTrue(count == null || count == 0, "您只能删除自己或自己创建的用户，无权删除其他用户");
        }
        // 逻辑删除
        boolean result = this.removeByIds(ids);
        return result;

    }

    /**
     * 修改用户密码
     *
     * @param userId   用户ID
     * @param password 用户密码
     * @return true|false
     */
    @Override
    public boolean updatePassword(Long userId, String password) {
        return this.update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getPassword, passwordEncoder.encode(password))
        );
    }

    /**
     * 根据用户名获取认证信息
     *
     * @param username 用户名
     * @return 用户认证信息 {@link UserAuthInfo}
     */
    @Override
    public UserAuthInfo getUserAuthInfo(String username) {
        UserAuthInfo userAuthInfo = this.baseMapper.getUserAuthInfo(username);
        if (userAuthInfo != null) {
            Set<String> roles = userAuthInfo.getRoles();
            if (CollectionUtil.isNotEmpty(roles)) {
                Set<String> perms = menuService.listRolePerms(roles);
                userAuthInfo.setPerms(perms);
            }

            // 获取最大范围的数据权限
            Integer dataScope = roleService.getMaximumDataScope(roles);
            userAuthInfo.setDataScope(dataScope);
        }
        return userAuthInfo;
    }


    /**
     * 获取导出用户列表
     *
     * @param queryParams
     * @return
     */
    @Override
    public List<UserExportVO> listExportUsers(UserPageQuery queryParams) {
        // 多租户数据隔离: 非超级管理员只能导出自己及自己创建的用户
        Long currentUserId = SecurityUtils.isRoot() ? null : SecurityUtils.getUserId();
        queryParams.setCurrentUserId(currentUserId);
        List<UserExportVO> list = this.baseMapper.listExportUsers(queryParams);
        return list;
    }

    /**
     * 获取登录用户信息
     *
     * @return
     */
    @Override
    public UserInfoVO getUserLoginInfo() {
        // 登录用户entity
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, SecurityUtils.getUser().getUsername())
                .select(
                        SysUser::getId,
                        SysUser::getNickname,
                        SysUser::getAvatar
                )
        );
        // entity->VO
        UserInfoVO userInfoVO = userConverter.entity2UserInfoVo(user);

        // 用户角色集合
        Set<String> roles = SecurityUtils.getRoles();
        userInfoVO.setRoles(roles);

        // 用户权限集合
        Set<String> perms = (Set<String>) redisTemplate.opsForValue().get(SecurityConstants.USER_PERMS_CACHE_PREFIX+ user.getId());
        userInfoVO.setPerms(perms);

        return userInfoVO;
    }


}
