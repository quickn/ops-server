package com.bszn.job;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bszn.monitor.agent.AgentService;
import com.bszn.monitor.cmdlog.CmdLogInfo;
import com.bszn.monitor.cmdlog.ICmdLogInfoService;
import com.bszn.monitor.docker.DockerContainer;
import com.bszn.monitor.docker.IDockerContainerService;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.monitor.project.DeployRequest;
import com.bszn.monitor.project.IProjectService;
import com.bszn.monitor.project.SyncRequest;
import com.bszn.ops.job.JobConfig;
import com.bszn.ops.job.JobConfigMapper;
import com.bszn.system.common.exception.BusinessException;
import com.bszn.utils.CmdLogInfoSessionUtil;
import com.google.common.collect.Lists;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
@Slf4j
public class ProjectJobHandler {

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

    @Resource
    IProjectService iProjectService;

    @Resource
    ICmdLogInfoService iCmdLogInfoService;

    @SneakyThrows
    @XxlJob("syncJarJobHandler")
    public void syncJarJobHandler() {
        JobContext ctx = buildJobContext("syncJarJobHandler");
        if (ctx == null)
            return;
        try {
            CmdLogInfoSessionUtil.set(ctx.cmdLogInfo);
            String[] projectNames = ctx.script.split(",");
            for (String projectName : projectNames) {
                SyncRequest syncRequest = new SyncRequest();
                syncRequest.setServiceId(ctx.serviceId);
                syncRequest.setType(1);
                syncRequest.setSourceAgentId(ctx.agentId);
                syncRequest.setProjectName(projectName);
                iProjectService.sync(syncRequest);
                if (ctx.interval > 0 && projectNames.length > 1) {
                    Thread.sleep(ctx.interval * 1000L);
                }
            }
        } catch (Exception e) {
            log.error("执行异常", e);
        } finally {
            CmdLogInfoSessionUtil.remove();
        }

    }

    /**
     * 发布job
     */
    @XxlJob("deployJobHandler")
    public void deployJobHandler() throws Exception {
        JobContext ctx = buildJobContext("deployJobHandler");
        if (ctx == null)
            return;
        try {
            CmdLogInfoSessionUtil.set(ctx.cmdLogInfo);
            String[] projectNames = ctx.script.split(",");
            CmdLogInfo cmdLogInfo = ctx.cmdLogInfo;
            for (String projectName : projectNames) {
                DeployRequest deployRequest = new DeployRequest();
                deployRequest.setDeployType(2);
                deployRequest.setProjectName(projectName);
                List<DockerContainer> list = iDockerContainerService.getByServiceIdAndDockerName(ctx.serviceId, projectName);
                if (CollectionUtils.isEmpty(list)) {
                    log.warn("docker 不存在 projectName:{}", projectName);
                    cmdLogInfo.setIsSuccess(false);
                    cmdLogInfo.setResult("docker 不存在 projectName:" + projectName);
                    iCmdLogInfoService.save(cmdLogInfo);
                    cmdLogInfo.setId(cmdLogInfo.getId() + 1);
                    continue;
                }
                List<Long> agentIds = Lists.newArrayList();
                for (DockerContainer dockerContainer : list) {
                    agentIds.add(dockerContainer.getAgentId());
                }
                deployRequest.setAgentIds(agentIds);
                iProjectService.deploy(deployRequest);
                if (ctx.interval > 0 && projectNames.length > 1) {
                    Thread.sleep(ctx.interval * 1000L);
                }
            }
        } catch (Exception e) {
            log.error("执行异常", e);
        } finally {
            CmdLogInfoSessionUtil.remove();
        }
    }

    /**
     * 解析任务参数与配置，构建统一的执行上下文
     */
    private JobContext buildJobContext(String handlerName) {
        String jobParam = XxlJobHelper.getJobParam();
        Long jobId = XxlJobHelper.getJobId();
        log.info("{} {}", handlerName, jobParam);
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
        Integer timeout = (Integer) jsonObject.getOrDefault("timeout", 10);
        Integer interval = (Integer) jsonObject.getOrDefault("interval", 0);
        String msgType = (String) jsonObject.getOrDefault("type", "cmd");

        JobContext ctx = new JobContext();
        ctx.agentId = agentId;
        ctx.command = command;
        ctx.script = script;
        ctx.serviceId = serviceId;
        ctx.timeout = timeout;
        ctx.interval = interval;
        ctx.msgType = msgType;
        ctx.cmdLogInfo = CmdLogInfo.builder().agentId(agentId).command(command)
                .script(script).msgType(msgType).timeout(timeout).jobId(jobId).serviceName(jobConfig.getServiceName()).build();
        if (serviceId == null) {
            ctx.cmdLogInfo.setIsSuccess(false);
            ctx.cmdLogInfo.setResult("serviceId 为空");
            iCmdLogInfoService.save(ctx.cmdLogInfo);
            return null;
        }
        return ctx;
    }

    /**
     * 任务执行上下文
     */
    private static class JobContext {
        private Long agentId;
        private String command;
        private String script;
        private Integer serviceId;
        private Integer timeout;
        private Integer interval;
        private String msgType;
        private CmdLogInfo cmdLogInfo;
    }

}
