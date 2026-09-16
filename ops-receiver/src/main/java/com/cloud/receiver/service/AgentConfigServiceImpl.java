package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.receiver.cmd.ClientMsgForm;
import com.cloud.receiver.constant.MonitorCmdC;
import com.cloud.receiver.entity.Agent;
import com.cloud.receiver.entity.AgentConfig;
import com.cloud.receiver.mapper.AgentConfigMapper;
import com.cloud.receiver.mapper.AgentMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
@Service
@Slf4j
public class AgentConfigServiceImpl extends ServiceImpl<AgentMapper, Agent> {

    @Resource
    AgentConfigMapper agentConfigMapper;

    private HashMap<String, Date> lastUpdateTimeMap = new HashMap<>();

    public JSONObject getByMac(String mac, String hostname) {
        List<JSONObject> configMap = this.baseMapper.getByMac(mac);
        Agent config = null;
        if (configMap.isEmpty()) {
            config = new Agent();
            config.setMac(mac);
            config.setHostname(hostname);
            this.baseMapper.insert(config);
            return null;
        } else {
            // 不要使用 List#getFirst()，它是 JDK 21 (SequencedCollection) 才有的方法，本模块按 release 17 编译
            config = JSONObject.parseObject(configMap.get(0).toJSONString(), Agent.class);
            if (!hostname.equals(config.getHostname())) {
                Agent agentConfig = new Agent();
                agentConfig.setId(config.getId());
                agentConfig.setHostname(hostname);
                config.setHostname(hostname);
                this.baseMapper.updateById(agentConfig);
            }
        }
        if (config.getServiceId() == null) {
            return configMap.get(0);
        }
        return configMap.get(0);
    }

    public Agent getServiceIdAndHostname(Integer serviceId, String hostname) {
        Agent agent = this.baseMapper.selectOne(Wrappers.<Agent>lambdaQuery().eq(Agent::getServiceId, serviceId)
                .eq(Agent::getHostname, hostname));
        if (agent == null) {
            return null;
        }
        Agent configCommon = this.baseMapper.selectOne(Wrappers.<Agent>lambdaQuery()
                .eq(Agent::getServiceId, agent.getServiceId())
                .eq(Agent::getIsMonitor, true).isNull(Agent::getMac));
        String key = serviceId + hostname;
        if (configCommon == null) {
            configCommon = agent;
        } else {
            BeanUtils.copyProperties(agent, configCommon);
        }
        Date lastUpdateTime = lastUpdateTimeMap.get(key);
        //更新了配置
        if (lastUpdateTime == null || lastUpdateTime.compareTo(agent.getUpdateTime()) != 0) {
            configCommon.setUpdate(true);
        } else {
            configCommon.setUpdate(false);
        }
        lastUpdateTimeMap.put(key, configCommon.getUpdateTime());
        return configCommon;
    }

    public void receiveClientMsg(ClientMsgForm clientMsgForm) {
        if (MonitorCmdC.updateClientVersion.equals(clientMsgForm.getCmd())) {
            Agent agentConfig = new Agent();
            agentConfig.setId(clientMsgForm.getAgentId());
            agentConfig.setClientVersion(clientMsgForm.getData());
            this.baseMapper.updateById(agentConfig);
        }
    }

    public void updateOnline(Long agentId, Boolean online) {
        Agent agentConfig = new Agent();
        agentConfig.setOnline(online);
        agentConfig.setId(agentId);
        this.baseMapper.updateById(agentConfig);
    }

    public void offline(Integer serviceId, String hostname) {
        Agent config = this.baseMapper.selectOne(Wrappers.<Agent>lambdaQuery()
                .eq(Agent::getHostname, hostname).eq(Agent::getServiceId, serviceId));
        if (config == null) {
            return;
        }
        updateOnline(config.getId(), false);
    }

    public void updateVersion(Agent agent, String version) {
        if (StringUtils.isEmpty(version)) return;
        if (!version.equals(agent.getClientVersion())) {
            Agent agentConfig = new Agent();
            agentConfig.setId(agent.getId());
            agentConfig.setClientVersion(version);
            this.baseMapper.updateById(agentConfig);
        }
    }

    public AgentConfig getAgentConfig(Long id) {
        return agentConfigMapper.selectById(id);
    }
}
