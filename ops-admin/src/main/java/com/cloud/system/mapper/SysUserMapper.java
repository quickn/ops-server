package com.cloud.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.system.common.annotation.DataPermission;
import com.cloud.system.model.bo.UserBO;
import com.cloud.system.model.entity.SysUser;
import com.cloud.system.model.dto.UserAuthInfo;
import com.cloud.system.model.bo.UserFormBO;
import com.cloud.system.model.query.UserPageQuery;
import com.cloud.system.model.vo.UserExportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户持久层
 *
 * @author haoxr
 * @since 2022/1/14
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 获取用户分页列表
     *
     * @param page
     * @param queryParams 查询参数
     * @return
     */
    @DataPermission(deptAlias = "u")
    Page<UserBO> getUserPage(Page<UserBO> page, UserPageQuery queryParams);

    /**
     * 获取用户表单详情
     *
     * @param userId        用户ID
     * @param currentUserId 当前登录用户ID(用于多租户数据隔离，为空表示不限制)
     * @return
     */
    UserFormBO getUserDetail(@Param("userId") Long userId, @Param("currentUserId") Long currentUserId);

    /**
     * 根据用户名获取认证信息
     *
     * @param username
     * @return
     */
    UserAuthInfo getUserAuthInfo(String username);

    /**
     * 获取导出用户列表
     *
     * @param queryParams
     * @return
     */
    @DataPermission(deptAlias = "u")
    List<UserExportVO> listExportUsers(UserPageQuery queryParams);
}
