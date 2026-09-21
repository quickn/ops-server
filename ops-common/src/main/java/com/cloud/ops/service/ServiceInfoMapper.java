package com.cloud.ops.service;

import com.cloud.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ServiceInfoMapper extends BaseQueryMapper<ServiceInfo, ServiceInfo> {

    @Select(" select id from service_info ")
    List<Integer> listServiceIdsByDeptId();
}
