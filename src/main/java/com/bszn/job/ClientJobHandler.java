package com.bszn.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bszn.monitor.agent.Agent;
import com.bszn.monitor.agent.AgentService;
import com.bszn.monitor.docker.DockerContainer;
import com.bszn.monitor.docker.IDockerContainerService;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.ops.job.JobConfig;
import com.bszn.ops.job.JobConfigMapper;
import com.bszn.system.common.exception.BusinessException;
import com.bszn.utils.CmdUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
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

    @XxlJob("cmdJobHandler")
    public void cmdJobHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        log.info("cmdJobHandler {}", jobParam);
        if (StringUtils.isEmpty(jobParam)) {
            return;
        }
        JSONObject jsonObject = null;
        if (jobParam.startsWith("{")) {
            jsonObject = JSONObject.parseObject(jobParam);
        } else {
            Integer jobId = Integer.parseInt(jobParam);
            JobConfig jobConfig = jobConfigMapper.selectByJobId(jobId);
            jsonObject = (JSONObject) JSON.toJSON(jobConfig);
        }
        Long agentId = jsonObject.getLong("agentId");
        String cmd = jsonObject.getString("cmd");
        Integer serviceId = jsonObject.getInteger("serviceId");
        String hostname = jsonObject.getString("hostname");
        Integer timeout = (Integer) jsonObject.getOrDefault("timeout", 10);
        Object msgType = jsonObject.getOrDefault("msgType", "cmd");
        if (agentId != null) {
            iMsgService.sendMsg(agentId, cmd, msgType.toString(), null);
            return;
        }
        if (StringUtils.isNotEmpty(hostname) && serviceId != null) {
            Agent agentConfig = agentService.getByServiceIdAndHost(serviceId, hostname);
            if (agentConfig == null) {
                log.warn("agent 不存在 hostname:{}", hostname);
                return;
            }
            iMsgService.sendMsg(agentConfig.getId(), cmd, msgType.toString(), timeout);
            return;
        }
        String dockerName = jsonObject.getString("dockerName");
        if (StringUtils.isNotEmpty(dockerName) && serviceId != null) {
            List<DockerContainer> list = iDockerContainerService.getByServiceIdAndDockerName(serviceId, dockerName);
            for (DockerContainer dockerContainer : list) {
                iMsgService.sendMsg(dockerContainer.getAgentId(), cmd, msgType.toString(), timeout);
            }
            return;
        }
        if (StringUtils.isEmpty(dockerName) && serviceId != null && StringUtils.isEmpty(hostname)) {
            List<Agent> agentList = agentService.getListByServiceId(serviceId);
            for (Agent agent : agentList) {
                iMsgService.sendMsgAndResponse(agent.getId(), cmd, msgType.toString(), timeout);
            }
            return;
        }
    }

    /**
     * 发布job
     */
    @XxlJob("deployJobHandler")
    public void deployJobHandler() throws Exception {
        String jobParam = XxlJobHelper.getJobParam();
        log.info("pythonJobHandler jobParam{}", jobParam);
        JSONObject jsonObject = JSONObject.parseObject(jobParam);
        Integer serviceId = jsonObject.getInteger("serviceId");
        JSONArray projectNames = jsonObject.getJSONArray("projectNames");
        if (serviceId == null) {
            throw new BusinessException("serviceId不能为空");
        }
        if (projectNames == null || projectNames.isEmpty()) {
            throw new BusinessException("projectNames不能为空");
        }
        String script = jsonObject.getString("script");
        if (script == null || script.isEmpty()) {
            throw new BusinessException("script不能为空");
        }
        if (StringUtils.isNotEmpty(pythonUrl)) {
            jsonObject.put("url", pythonUrl);
        }
        String encodedParams = Base64.getEncoder().encodeToString(jsonObject.toJSONString().getBytes());
        String[] arr = {pythonPath, pythonScript + "/" + script, encodedParams};
        CmdUtil.exec(false, 120, false, arr);
    }

}
