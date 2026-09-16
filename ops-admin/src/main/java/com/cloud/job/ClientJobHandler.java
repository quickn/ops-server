package com.cloud.job;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cloud.ops.agent.Agent;
import com.cloud.ops.agent.AgentService;
import com.cloud.ops.cmdlog.CmdLogInfo;
import com.cloud.ops.docker.DockerContainer;
import com.cloud.ops.docker.IDockerContainerService;
import com.cloud.ops.msg.IMsgService;
import com.cloud.ops.job.JobConfig;
import com.cloud.ops.job.JobConfigMapper;
import com.cloud.system.common.exception.BusinessException;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
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
    AgentService agentService;

    @Value("${python.path}")
    String pythonPath;
    @Value("${python.script}")
    String pythonScript;
    @Value("${python.url:}")
    String pythonUrl;

    @Resource
    JobConfigMapper jobConfigMapper;

    @SneakyThrows
    @XxlJob("cmdJobHandler")
    public void cmdJobHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        Long jobId = XxlJobHelper.getJobId();
        log.info("cmdJobHandler {}", jobParam);
        JSONObject jsonObject = null;
        if (StringUtils.isNotEmpty(jobParam) && jobParam.startsWith("{")) {
            jsonObject = JSONObject.parseObject(jobParam);
        }
        JobConfig jobConfig = jobConfigMapper.selectById(jobId);
        if (jobConfig == null) {
            log.warn("jobConfig 不存在 jobId:{}", jobId);
        }
        if (jsonObject == null && jobConfig != null) {
            jsonObject = (JSONObject) JSON.toJSON(jobConfig);
        }
        if (jsonObject != null && jobConfig != null) {
            JSONObject temp = (JSONObject) JSON.toJSON(jobConfig);
            temp.putAll(jsonObject);
            jsonObject = temp;
        }
        if (jsonObject == null) {
            throw new BusinessException("jobParam 和 jobConfig 都为空");
        }
        Long agentId = jsonObject.getLong("agentId");
        String command = jsonObject.getString("command");
        String script = jsonObject.getString("script");
        Integer serviceId = jsonObject.getInteger("serviceId");
        String hostname = jsonObject.getString("hostname");
        Integer timeout = (Integer) jsonObject.getOrDefault("timeout", 10);
        Integer interval = (Integer) jsonObject.getOrDefault("interval", 0);
        String msgType = (String) jsonObject.getOrDefault("type", "cmd");

        CmdLogInfo cmdLogInfo = CmdLogInfo.builder().agentId(agentId).command
                (command).script(script).msgType(msgType).timeout(timeout).jobId(jobId).build();

        if (agentId != null) {
            iMsgService.sendMsgAndResponse(cmdLogInfo);
            return;
        }
        if (StringUtils.isNotEmpty(hostname) && serviceId != null) {
            Agent agentConfig = agentService.getByServiceIdAndHost(serviceId, hostname);
            if (agentConfig == null) {
                log.warn("agent 不存在 hostname:{}", hostname);
                return;
            }
            iMsgService.sendMsgAndResponse(cmdLogInfo);
            return;
        }
        String dockerName = jsonObject.getString("dockerName");
        if (StringUtils.isNotEmpty(dockerName) && serviceId != null) {
            List<DockerContainer> list = iDockerContainerService.getByServiceIdAndDockerName(serviceId, dockerName);
            for (DockerContainer dockerContainer : list) {
                cmdLogInfo.setAgentId(dockerContainer.getAgentId());
                iMsgService.sendMsgAndResponse(cmdLogInfo);
                if (interval > 0 && list.size() > 1) {
                    Thread.sleep(interval * 1000L);
                }
            }
            return;
        }
        if (StringUtils.isEmpty(dockerName) && serviceId != null && StringUtils.isEmpty(hostname)) {
            List<Agent> agentList = agentService.getListByServiceId(serviceId);
            for (Agent agent : agentList) {
                cmdLogInfo.setAgentId(agent.getId());
                iMsgService.sendMsgAndResponse(cmdLogInfo);
                if (interval > 0 && agentList.size() > 1) {
                    Thread.sleep(interval * 1000L);
                }
            }
            return;
        }
    }

}
