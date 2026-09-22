package com.cloud.ops.service;

import com.cloud.base.mapper.BaseQueryMapper;
import com.cloud.system.common.annotation.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ServiceInfoMapper extends BaseQueryMapper<ServiceInfo, ServiceInfo> {

    @Select(" select id from service_info ")
    @DataPermission(deptIdColumnName = "")
    List<Integer> listServiceIdsByDeptId();
}
