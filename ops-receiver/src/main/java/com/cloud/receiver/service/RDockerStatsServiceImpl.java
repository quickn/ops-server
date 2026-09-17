package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.ops.agent.Agent;
import com.cloud.ops.docker.DockerStats;
import com.cloud.ops.docker.DockerStatsMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
@Service
@Slf4j
public class RDockerStatsServiceImpl extends ServiceImpl<DockerStatsMapper,
        DockerStats> {

    @Resource
    RWarnLogInfoService logInfoService;


    public String saveAgentJsonObject(JSONObject agentJsonObject, Agent agentConfig) {
        Object object = agentJsonObject.get("dockerStats");
        if (object == null) return null;
        return saveAgentJsonArray((JSONArray) object, agentConfig);
    }

    private String saveAgentJsonArray(JSONArray object, Agent agentConfig) {
        for (int i = 0; i < object.size(); i++) {
            JSONObject stats = object.getJSONObject(i);
            DockerStats dockerStats = stats.toJavaObject(DockerStats.class);
            dockerStats.setHostname(agentConfig.getHostname());
            dockerStats.setServiceId(agentConfig.getServiceId());
            dockerStats.setAgentId(agentConfig.getId());
            dockerStats.setServiceName(agentConfig.getServiceName());
            this.baseMapper.insert(dockerStats);
            String title = null;
            String commContent = null;
            String threshold = null;
            if (agentConfig.getCpuWarnValDocker() != null && dockerStats.getCpu() > agentConfig.getCpuWarnValDocker()) {
                //发送采集指令
                title = "docker容器CPU告警";
                commContent = "服务器:" + dockerStats.getHostname() + " 容器:" + dockerStats.getNames() +
                        " CPU使用率为" + dockerStats.getCpu() + ",阈值:" + agentConfig.getCpuWarnValDocker() + "%";
                threshold = agentConfig.getCpuWarnValDocker() + "%";
            }
            if (agentConfig.getMemWarnVal() != null && dockerStats.getMem() > agentConfig.getMemWarnVal()) {
                title = "docker容器内存告警";
                commContent = "服务器:" + dockerStats.getHostname() +
                        " 容器:" + dockerStats.getNames() + " 内存使用率为" + dockerStats.getMem() + ",阈值:" + agentConfig.getMemWarnVal() + "%";
                threshold = agentConfig.getMemWarnVal() + "%";
            }
            logInfoService.saveErrorLog(title, commContent, commContent, agentConfig, threshold);
        }
        return null;
    }

}
