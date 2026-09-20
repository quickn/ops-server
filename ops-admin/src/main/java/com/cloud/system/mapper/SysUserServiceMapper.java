package com.cloud.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.ops.service.ServiceInfo;
import com.cloud.system.model.entity.SysUserService;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户服务关联持久层
 *
 * @author liuyun
 * @since 2026-09-18
 */
@Mapper
public interface SysUserServiceMapper extends BaseMapper<SysUserService> {

    /**
     * 根据用户ID获取关联的服务列表
     *
     * @param userId 用户ID
     * @return 服务列表
     */
    List<ServiceInfo> listServiceByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID获取关联的服务ID集合
     *
     * @param userId 用户ID
     * @return 服务ID集合
     */
    List<Integer> listServiceIdsByUserId(@Param("userId") Long userId);
}