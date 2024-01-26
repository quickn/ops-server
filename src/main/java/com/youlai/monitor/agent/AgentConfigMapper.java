package com.youlai.monitor.agent;

import com.youlai.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgentConfigMapper extends BaseQueryMapper<AgentConfig, AgentConfig> {

    @Select(" select * from agent_config where service_id =#{serviceId} and hostname=#{hostname}")
    AgentConfig getByServiceIdAndHost(Integer serviceId, String hostname);
}
