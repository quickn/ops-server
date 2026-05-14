package com.bszn.monitor.agent;

import com.bszn.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AgentMapper extends BaseQueryMapper<Agent, Agent> {

    @Select(" select * from agent where service_id =#{serviceId} and hostname=#{hostname}")
    Agent getByServiceIdAndHost(Integer serviceId, String hostname);

    @Select(" select a.id,a.hostname from agent a left join docker_container d on a.service_id=d.service_id and a.hostname=d.hostname where a.is_monitor=1 and d.is_monitor=1 and a.service_id=#{serviceId} and d.`names` = #{dockerName} ")
    List<Agent> getByServiceId(@Param("serviceId") Integer serviceId, @Param("dockerName") String dockerName);

    @Select(" select id,hostname from agent where service_id =#{serviceId} and is_monitor=1 ")
    List<Agent> getListByServiceId(Integer serviceId);
}
