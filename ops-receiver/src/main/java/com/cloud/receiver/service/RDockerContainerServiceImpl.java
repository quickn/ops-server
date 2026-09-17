package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.ops.agent.Agent;
import com.cloud.ops.docker.DockerContainer;
import com.cloud.ops.docker.DockerContainerMapper;
import com.cloud.receiver.dto.AgentData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
@Service
@Slf4j
public class RDockerContainerServiceImpl extends ServiceImpl<DockerContainerMapper,
        DockerContainer> {


    public void saveStr(AgentData agentData) {
        if (agentData.getHostname() == null) {
            return;
        }
        if (!agentData.getData().startsWith("[")) {
            return;
        }
        // 收集到的全部docker服务
        JSONArray jsonArray = JSON.parseArray(agentData.getData());
        // 获取主机下全部的docker服务
        List<DockerContainer> listOld =
                lambdaQuery().eq(DockerContainer::getHostname, agentData.getHostname())
                        .eq(DockerContainer::getServiceId, agentData.getServiceId()).list();
        Map<String, DockerContainer> containerMapOld = new HashMap<>();
        if (!listOld.isEmpty()) {
            listOld.forEach(data -> containerMapOld.put(data.getContainerId(), data));
        }
        Set<String> noRemoveKey = new HashSet<>();
        for (Object dataObj : jsonArray) {
            JSONObject jsonObject = (JSONObject) dataObj;
            DockerContainer dockerContainer = jsonObject.toJavaObject(DockerContainer.class);
            if (StringUtils.isEmpty(dockerContainer.getContainerId())) {
                log.warn("容器id为空 {} ", jsonObject);
                continue;
            }
            jsonObject.clear();
            if (containerMapOld.containsKey(dockerContainer.getContainerId())) {
                DockerContainer container = containerMapOld.get(dockerContainer.getContainerId());
                // 状态不相等 修改状态
                if (!Objects.equals(container.getStatus(), dockerContainer.getStatus())) {
                    dockerContainer.setId(container.getId());
                    dockerContainer.setUpdateTime(LocalDateTime.now());
                    this.baseMapper.updateById(dockerContainer);
                }
            } else {
                saveOrUpdateAgent(dockerContainer, agentData);
            }
            noRemoveKey.add(dockerContainer.getContainerId());
        }
        // 删除没采集到 但数据库存在的记录
        List<Long> removeIds = listOld.stream().filter(data -> !noRemoveKey.contains(data.getContainerId())).
                map(DockerContainer::getId).collect(Collectors.toList());
        if (!removeIds.isEmpty()) {
            this.baseMapper.deleteBatchIds(removeIds);
        }
    }

    void saveOrUpdateAgent(DockerContainer dockerContainer, AgentData agentData) {
        DockerContainer dockerContainerTemp = baseMapper.selectOne
                (new LambdaQueryWrapper<DockerContainer>().eq(DockerContainer::getAgentId, agentData.getAgentId())
                        .eq(DockerContainer::getNames, dockerContainer.getNames()));
        if (dockerContainerTemp != null) {
            dockerContainer.setId(dockerContainerTemp.getId());
            dockerContainer.setUpdateTime(LocalDateTime.now());
            this.baseMapper.updateById(dockerContainer);
        } else {
            dockerContainer.setHostname(agentData.getHostname());
            dockerContainer.setServiceName(agentData.getServiceName());
            dockerContainer.setServiceId(agentData.getServiceId());
            dockerContainer.setAgentId(agentData.getAgentId());
            this.baseMapper.insert(dockerContainer);
        }

    }


    public void saveAgentJsonObject(JSONObject agentJsonObject, Agent agentConfig) {
        String dockerContainerList = agentJsonObject.getString("dockerContainerList");
        if (StringUtils.isEmpty(dockerContainerList)) {
            return;
        }
        AgentData agentData = new AgentData();
        agentData.setData(dockerContainerList);
        agentData.setHostname(agentConfig.getHostname());
        agentData.setServiceId(agentConfig.getServiceId());
        agentData.setServiceName(agentConfig.getServiceName());
        agentData.setAgentId(agentConfig.getId());
        this.saveStr(agentData);
    }

    public void checkDocker() {
        List<DockerContainer> list = this.baseMapper.selectList(Wrappers.<DockerContainer>lambdaQuery()
                .eq(DockerContainer::getIsMonitor, 1));
    }

}
