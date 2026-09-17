package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.ops.agent.Agent;
import com.cloud.ops.agent.AgentMapper;
import com.cloud.ops.agentConfig.AgentConfig;
import com.cloud.ops.agentConfig.AgentConfigMapper;
import com.cloud.receiver.cmd.ClientMsgForm;
import com.cloud.receiver.constant.MonitorCmdC;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
@Service
@Slf4j
public class RAgentConfigServiceImpl extends ServiceImpl<AgentConfigMapper, AgentConfig> {

    @Resource
    AgentConfigMapper agentConfigMapper;

    @Resource
    AgentMapper agentMapper;

    public JSONObject getByMac(String mac, String hostname) {
        List<JSONObject> configMap = agentMapper.getByMac(mac);
        Agent config = null;
        if (configMap.isEmpty()) {
            config = new Agent();
            config.setMac(mac);
            config.setHostname(hostname);
            agentMapper.insert(config);
            return null;
        } else {
            // 不要使用 List#getFirst()，它是 JDK 21 (SequencedCollection) 才有的方法，本模块按 release 17 编译
            config = JSONObject.parseObject(configMap.get(0).toJSONString(), Agent.class);
            if (StringUtils.isNotEmpty(hostname) && !hostname.equals(config.getHostname())) {
                Agent agentConfig = new Agent();
                agentConfig.setId(config.getId());
                agentConfig.setHostname(hostname);
                config.setHostname(hostname);
                agentMapper.updateById(agentConfig);
            }
        }
        if (config.getServiceId() == null) {
            return configMap.get(0);
        }
        return configMap.get(0);
    }

    public Agent getServiceIdAndHostname(Integer serviceId, String hostname) {
        Agent agent = agentMapper.selectOne(Wrappers.<Agent>lambdaQuery().eq(Agent::getServiceId, serviceId)
                .eq(Agent::getHostname, hostname));
        if (agent == null) {
            return null;
        }
        Agent configCommon = agentMapper.selectOne(Wrappers.<Agent>lambdaQuery()
                .eq(Agent::getServiceId, agent.getServiceId())
                .eq(Agent::getIsMonitor, true).isNull(Agent::getMac));
        if (configCommon == null) {
            configCommon = agent;
        } else {
            BeanUtils.copyProperties(agent, configCommon);
        }
        return configCommon;
    }

    public void receiveClientMsg(ClientMsgForm clientMsgForm) {
        if (MonitorCmdC.updateClientVersion.equals(clientMsgForm.getCmd())) {
            Agent agentConfig = new Agent();
            agentConfig.setId(clientMsgForm.getAgentId());
            agentConfig.setClientVersion(clientMsgForm.getData());
            this.agentMapper.updateById(agentConfig);
        }
    }

    public void updateOnline(Long agentId, Boolean online) {
        Agent agentConfig = new Agent();
        agentConfig.setOnline(online);
        agentConfig.setId(agentId);
        this.agentMapper.updateById(agentConfig);
    }

    public void offline(Integer serviceId, String hostname) {
        Agent config = this.agentMapper.selectOne(Wrappers.<Agent>lambdaQuery()
                .eq(Agent::getHostname, hostname).eq(Agent::getServiceId, serviceId));
        if (config == null) {
            return;
        }
        updateOnline(config.getId(), false);
    }


    public AgentConfig getAgentConfig(Long id) {
        return agentConfigMapper.selectById(id);
    }
}
