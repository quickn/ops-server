package com.cloud.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.system.common.constant.SystemConstants;
import com.cloud.system.common.model.Option;
import com.cloud.system.converter.RoleConverter;
import com.cloud.system.mapper.SysRoleMapper;
import com.cloud.system.model.entity.SysRole;
import com.cloud.system.model.entity.SysRoleMenu;
import com.cloud.system.model.entity.SysUserRole;
import com.cloud.system.model.form.RoleForm;
import com.cloud.system.model.query.RolePageQuery;
import com.cloud.system.model.vo.RolePageVO;
import com.cloud.system.service.SysRoleMenuService;
import com.cloud.system.service.SysRoleService;
import com.cloud.system.service.SysUserRoleService;
import com.cloud.system.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色业务实现类
 *
 * @author haoxr
 * @since 2022/6/3
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMenuService sysRoleMenuService;
    private final SysUserRoleService sysUserRoleService;
    private final RoleConverter roleConverter;

    /**
     * 角色分页列表
     * <p>
     * 非超级管理员不显示超级管理员角色(id=1 / code=ROOT)
     *
     * @param queryParams
     * @return
     */
    @Override
    public Page<RolePageVO> getRolePage(RolePageQuery queryParams) {
        // 查询参数
        int pageNum = queryParams.getPageNum();
        int pageSize = queryParams.getPageSize();
        String keywords = queryParams.getKeywords();
        boolean isRoot = SecurityUtils.isRoot();

        // 查询数据
        Page<SysRole> rolePage = this.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysRole>()
                        .and(StrUtil.isNotBlank(keywords),
                                wrapper ->
                                        wrapper.like(StrUtil.isNotBlank(keywords), SysRole::getName, keywords)
                                                .or()
                                                .like(StrUtil.isNotBlank(keywords), SysRole::getCode, keywords)
                        )
                        // 非超级管理员不显示超级管理员角色(双重防御：同时按 id 和 code 过滤)
                        .ne(!isRoot, SysRole::getId, SystemConstants.ROOT_ROLE_ID)
                        .ne(!isRoot, SysRole::getCode, SystemConstants.ROOT_ROLE_CODE)
        );

        // 实体转换
        Page<RolePageVO> pageResult = roleConverter.entity2Page(rolePage);
        return pageResult;
    }

    /**
     * 角色下拉列表
     * <p>
     * 多租户数据隔离规则:
     * <ul>
     *     <li>超级管理员(role_id=1)可看到所有角色</li>
     *     <li>其他用户不可看到超级管理员角色(id=1 / code=ROOT)，防止越权分配</li>
     * </ul>
     *
     * @return
     */
    @Override
    public List<Option> listRoleOptions() {
        boolean isRoot = SecurityUtils.isRoot();
        // 查询数据
        List<SysRole> roleList = this.list(new LambdaQueryWrapper<SysRole>()
                // 非超级管理员：排除 id=1 的系统管理员角色（双重防御：同时按 id 和 code 过滤）
                .ne(!isRoot, SysRole::getId, SystemConstants.ROOT_ROLE_ID)
                .ne(!isRoot, SysRole::getCode, SystemConstants.ROOT_ROLE_CODE)
                .ne(!isRoot, SysRole::getCode, SystemConstants.ADMIN_ROLE_CODE)
                .select(SysRole::getId, SysRole::getName)
                .orderByAsc(SysRole::getSort)
        );

        // 实体转换
        List<Option> list = roleConverter.entities2Options(roleList);
        return list;
    }

    /**
     * 保存角色
     *
     * @param roleForm
     * @return
     */
    @Override
    public boolean saveRole(RoleForm roleForm) {

        Long roleId = roleForm.getId();
        String roleCode = roleForm.getCode();

        long count = this.count(new LambdaQueryWrapper<SysRole>()
                .ne(roleId != null, SysRole::getId, roleId)
                .and(wrapper ->
                        wrapper.eq(SysRole::getCode, roleCode).or().eq(SysRole::getName, roleCode)
                ));
        Assert.isTrue(count == 0, "角色名称或角色编码重复，请检查！");

        // 实体转换
        SysRole role = roleConverter.form2Entity(roleForm);

        boolean result = this.saveOrUpdate(role);
        return result;
    }

    /**
     * 获取角色表单数据
     *
     * @param roleId 角色ID
     * @return  {@link RoleForm} – 角色表单数据
     */
    @Override
    public RoleForm getRoleForm(Long roleId) {
        SysRole entity = this.getById(roleId);
        RoleForm roleForm = roleConverter.entity2Form(entity);
        return roleForm;
    }

    /**
     * 修改角色状态
     *
     * @param roleId 角色ID
     * @param status 角色状态(1:启用；0:禁用)
     * @return {@link Boolean}
     */
    @Override
    public boolean updateRoleStatus(Long roleId, Integer status) {
        boolean result = this.update(new LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getId, roleId)
                .set(SysRole::getStatus, status));
        return result;
    }

    /**
     * 批量删除角色
     *
     * @param ids 角色ID，多个使用英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteRoles(String ids) {
        List<Long> roleIds = Arrays.asList(ids.split(",")).stream().map(id -> Long.parseLong(id)).collect(Collectors.toList());
        Optional.ofNullable(roleIds)
                .orElse(new ArrayList<>())
                .forEach(id -> {
                    long count = sysUserRoleService.count(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
                    Assert.isTrue(count <= 0, "该角色已分配用户，无法删除");
                    sysRoleMenuService.remove(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
                });


        boolean result = this.removeByIds(roleIds);
        return result;
    }

    /**
     * 获取角色的菜单ID集合
     *
     * @param roleId 角色ID
     * @return 菜单ID集合(包括按钮权限ID)
     */
    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        List<Long> menuIds = sysRoleMenuService.listMenuIdsByRoleId(roleId);
        return menuIds;
    }

    /**
     * 修改角色的资源权限
     *
     * @param roleId
     * @param menuIds
     * @return
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "system", key = "'routes'")
    public boolean updateRoleMenus(Long roleId, List<Long> menuIds) {
        // 删除角色菜单
        sysRoleMenuService.remove(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        // 新增角色菜单
        if (CollectionUtil.isNotEmpty(menuIds)) {
            List<SysRoleMenu> roleMenus = menuIds.stream()
                    .map(menuId -> new SysRoleMenu(roleId, menuId))
                    .collect(Collectors.toList());
            sysRoleMenuService.saveBatch(roleMenus);
        }
        return true;
    }

    /**
     * 获取最大范围的数据权限
     *
     * @param roles
     * @return
     */
    @Override
    public Integer getMaximumDataScope(Set<String> roles) {
        Integer dataScope = this.baseMapper.getMaximumDataScope(roles);
        return dataScope;
    }

}
