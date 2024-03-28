package com.youlai.monitor.agent;

import com.youlai.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentConfigMapper extends BaseQueryMapper<AgentConfig, AgentConfig> {

    @Select(" select * from agent_config where service_id =#{serviceId} and hostname=#{hostname}")
    AgentConfig getByServiceIdAndHost(Integer serviceId, String hostname);

    @Select(" select a.id,a.hostname from agent_config a left join docker_container d on a.service_id=d.service_id and a.hostname=d.hostname where a.is_monitor=1 and d.is_monitor=1 and a.service_id=#{serviceId} and d.`names` = #{dockerName} ")
    List<AgentConfig> getByServiceId(@Param("serviceId") Integer serviceId, @Param("dockerName") String dockerName);

    @Select(" select id,hostname from agent_config where service_id =#{serviceId} and is_monitor=1 ")
    List<AgentConfig> getListByServiceId(Integer serviceId);
}
