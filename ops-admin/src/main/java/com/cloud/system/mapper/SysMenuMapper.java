package com.cloud.system.mapper;

/**
 * 菜单持久接口层
 *
 * @author haoxr
 * @since 2022/1/24
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.system.model.entity.SysMenu;
import com.cloud.system.model.bo.RouteBO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Set;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    List<RouteBO> listRoutes();

    /**
     * 获取角色权限集合
     *
     * @param roles
     * @return
     */
    Set<String> listRolePerms(Set<String> roles);

    /**
     * 获取用户拥有的菜单ID集合（含其祖先节点，保证树形结构完整）
     *
     * @param userId 用户ID
     * @return 菜单ID集合
     */
    Set<Long> listMenuIdsByUserId(Long userId);
}
