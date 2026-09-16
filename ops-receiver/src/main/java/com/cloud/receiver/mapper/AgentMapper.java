package com.cloud.receiver.mapper;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.receiver.entity.Agent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentMapper extends BaseMapper<Agent> {

    @Select(" select * from agent where mac = #{mac}  ")
    List<JSONObject> getByMac(String mac);
}
