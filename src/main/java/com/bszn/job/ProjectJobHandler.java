package com.bszn.job;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

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

    @Resource(name = "newCachedThreadPool")
    ExecutorService executorService;

    @SneakyThrows
    @XxlJob("syncJarJobHandler")
    public void syncJarJobHandler() {
        JobContext ctx = buildJobContext();
        try {
            CmdLogInfoSessionUtil.set(ctx.cmdLogInfo);
            String[] projectNames = ctx.projectNames;
            for (String projectName : projectNames) {
                SyncRequest syncRequest = new SyncRequest();
                syncRequest.setServiceId(ctx.cmdLogInfo.getServiceId());
                syncRequest.setType(1);
                syncRequest.setSourceAgentId(ctx.jobConfig.getAgentId());
                syncRequest.setProjectName(projectName);
                iProjectService.sync(syncRequest);
                if (ctx.jobConfig.getInterval() > 0 && projectNames.length > 1) {
                    Thread.sleep(ctx.jobConfig.getInterval() * 1000L);
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
        JobContext ctx = buildJobContext();
        try {
            String[] projectNames = ctx.projectNames;
            // 不同 projectName 并行部署
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String projectName : projectNames) {
                futures.add(CompletableFuture.runAsync(() -> deployProject(ctx, projectName), executorService));
            }
            // 等待所有项目部署完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("执行异常", e);
        }
    }

    /**
     * 部署单个项目
     *
     * @param ctx         任务上下文
     * @param projectName 项目名称
     */
    private void deployProject(JobContext ctx, String projectName) {
        try {
            // 每个项目使用独立的 CmdLogInfo 副本，避免并发修改共享对象
            CmdLogInfo cmdLogInfo = ctx.cmdLogInfo;
            CmdLogInfoSessionUtil.set(cmdLogInfo);
            DeployRequest deployRequest = new DeployRequest();
            deployRequest.setDeployType(ctx.deployType);
            deployRequest.setProjectName(projectName);
            deployRequest.setSync(ctx.sync);
            List<DockerContainer> list = iDockerContainerService.getByServiceIdAndDockerName(ctx.cmdLogInfo.getServiceId(), projectName);
            if (CollectionUtils.isEmpty(list)) {
                log.warn("docker 不存在 projectName:{}", projectName);
                cmdLogInfo.setIsSuccess(false);
                cmdLogInfo.setResult("docker 不存在 projectName:" + projectName);
                iCmdLogInfoService.save(cmdLogInfo);
                return;
            }
            List<Long> agentIds = Lists.newArrayList();
            for (DockerContainer dockerContainer : list) {
                agentIds.add(dockerContainer.getAgentId());
            }
            deployRequest.setAgentIds(agentIds);
            if (ctx.jobConfig.getInterval() != null) {
                deployRequest.setInterval(ctx.jobConfig.getInterval());
            }
            iProjectService.deploy(deployRequest);

        } catch (Exception e) {
            log.error("部署项目异常 projectName:{}", projectName, e);
        } finally {
            CmdLogInfoSessionUtil.remove();
        }
    }

    /**
     * 获取项目名称数量
     */
    private int projectNamesLength(JobContext ctx) {
        return ctx.projectNames != null ? ctx.projectNames.length : 0;
    }

    /**
     * 解析任务参数与配置，构建统一的执行上下文
     */
    private JobContext buildJobContext() {
        Long jobId = XxlJobHelper.getJobId();
        JobConfig jobConfig = jobConfigMapper.selectById(jobId);
        if (jobConfig == null) {
            log.warn("jobConfig 不存在 jobId:{}", jobId);
            throw new BusinessException("jobConfig 不存在");
        }
        String jobParam = jobConfig.getScript();
        if (StringUtils.isEmpty(jobParam)) {
            throw new BusinessException("参数不能为空");
        }
        JSONObject jsonObject = null;
        String msgType = "cmd";
        Integer deployType = 2;
        Boolean sync = true;

        String[] projectNames = {};

        if (StringUtils.isNotEmpty(jobParam) && jobParam.startsWith("{")) {
            jsonObject = JSONObject.parseObject(jobParam);
            msgType = (String) jsonObject.getOrDefault("type", "cmd");
            deployType = (Integer) jsonObject.getOrDefault("deployType", 2);
            sync = (Boolean) jsonObject.getOrDefault("sync", true);
            String projectNamesStr = jsonObject.getString("projectNames");
            if (StringUtils.isEmpty(projectNamesStr)) {
                throw new BusinessException("jobConfig 不存在");
            } else {
                projectNames = projectNamesStr.split(",");
            }
        }

        JobContext ctx = new JobContext();
        ctx.jobConfig = jobConfig;
        ctx.deployType = deployType;
        ctx.sync = sync;
        ctx.projectNames = projectNames;
        ctx.cmdLogInfo = CmdLogInfo.builder().agentId(jobConfig.getAgentId()).command(jobConfig.getCommand())
                .script(jobConfig.getScript()).msgType(msgType)
                .timeout(jobConfig.getTimeout()).jobId(jobId).serviceName(jobConfig.getServiceName())
                .serviceId(jobConfig.getServiceId()).build();
        if (jobConfig.getServiceId() == null) {
            ctx.cmdLogInfo.setIsSuccess(false);
            ctx.cmdLogInfo.setResult("serviceId 为空");
            iCmdLogInfoService.save(ctx.cmdLogInfo);
            throw new BusinessException("serviceId 为空");
        }
        return ctx;
    }

    /**
     * 任务执行上下文
     */
    private static class JobContext {
        private JobConfig jobConfig;
        private String[] projectNames;
        private Integer deployType;
        private Boolean sync;
        private CmdLogInfo cmdLogInfo;
    }

}
