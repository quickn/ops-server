package com.bszn.job;

import com.alibaba.fastjson.JSONObject;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigService;
import com.bszn.monitor.docker.DockerContainer;
import com.bszn.monitor.docker.IDockerContainerService;
import com.bszn.monitor.msg.IMsgService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ClientJobHandler {

    @Resource
    IMsgService iMsgService;
    @Resource
    IDockerContainerService iDockerContainerService;
    @Resource
    AgentConfigService agentConfigService;


    @XxlJob("cmdJobHandler")
    public void cmdJobHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        log.info("cmdJobHandler {}", jobParam);
        if (StringUtils.isEmpty(jobParam)) {
            return;
        }
        JSONObject jsonObject = JSONObject.parseObject(jobParam);
        Long agentId = jsonObject.getLong("agentId");
        String cmd = jsonObject.getString("cmd");
        Integer serviceId = jsonObject.getInteger("serviceId");
        String hostname = jsonObject.getString("hostname");
        if (agentId != null) {
            iMsgService.sendMsg(null, agentId, cmd, "cmd", null);
            return;
        }
        if (StringUtils.isNotEmpty(hostname) && serviceId != null) {
            AgentConfig agentConfig = agentConfigService.getByServiceIdAndHost(serviceId, hostname);
            if (agentConfig == null) {
                log.warn("agent 不存在 hostname:{}", hostname);
                return;
            }
            iMsgService.sendMsg(null, agentConfig.getId(), cmd, "cmd", null);
            return;
        }
        String dockerName = jsonObject.getString("dockerName");
        if (StringUtils.isNotEmpty(dockerName) && serviceId != null) {
            List<DockerContainer> list = iDockerContainerService.getByServiceIdAndDockerName(serviceId, dockerName);
            for (DockerContainer dockerContainer : list) {
                iMsgService.sendMsg(null, dockerContainer.getAgentId(), cmd, "cmd", null);
            }
            return;
        }
    }

}
