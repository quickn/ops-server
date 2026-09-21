package com.cloud.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.ops.service.ServiceInfo;
import com.cloud.system.model.entity.SysDeptService;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门服务关联持久层
 *
 * @author liuyun
 * @since 2026-09-18
 */
@Mapper
public interface SysDeptServiceMapper extends BaseMapper<SysDeptService> {

    /**
     * 根据部门ID获取关联的服务列表
     *
     * @param deptId 部门ID
     * @return 服务列表
     */
    List<ServiceInfo> listServiceByDeptId(@Param("deptId") Long deptId);

    /**
     * 根据部门ID获取关联的服务ID集合
     *
     * @param deptId 部门ID
     * @return 服务ID集合
     */
    List<Integer> listServiceIdsByDeptId(@Param("deptId") Long deptId);
}
