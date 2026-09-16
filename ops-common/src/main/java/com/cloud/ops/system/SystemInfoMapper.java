package com.cloud.ops.system;

import com.cloud.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SystemInfoMapper extends BaseQueryMapper<SystemInfo, SystemInfo> {

    /**
     * 查询所有已开启监控的服务下的主机
     */
    @Select(" select sys.* from system_info sys left join service_info s on sys.service_id=s.id where s.is_monitor=1 ")
    List<SystemInfo> listAll();
}
