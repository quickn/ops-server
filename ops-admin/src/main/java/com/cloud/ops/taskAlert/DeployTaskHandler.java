package com.cloud.ops.taskAlert;

import com.alibaba.fastjson2.JSONObject;
import com.cloud.ops.docker.DockerContainer;
import com.cloud.ops.docker.IDockerContainerService;
import com.cloud.ops.project.DeployRequest;
import com.cloud.ops.project.IProjectService;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 定时部署任务处理器
 *
 * <p>脚本内容约定为 JSON：
 * <pre>
 * {
 *   "projectNames": "projectA,projectB",
 *   "deployType": 2,
 *   "sync": true,
 *   "interval": 0
 * }
 * </pre></p>
 *
 * @author Liuyun
 */
@Component
@Slf4j
public class DeployTaskHandler implements TaskTypeHandler {

    @Resource
    private IProjectService iProjectService;

    @Resource
    private IDockerContainerService iDockerContainerService;

    @Override
    public String type() {
        return DEPLOY_TYPE;
    }

    @Override
    public void handle(TaskAlert taskAlert, TaskAlertRecord record) {
        if (StringUtils.isBlank(taskAlert.getScript())) {
            record.setState(1);
            record.setResult("部署配置为空");
            return;
        }
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(taskAlert.getScript());
        } catch (Exception e) {
            record.setState(1);
            record.setResult("部署配置解析失败：" + e.getMessage());
            return;
        }

        String projectNamesStr = jsonObject.getString("projectNames");
        if (StringUtils.isBlank(projectNamesStr)) {
            record.setState(1);
            record.setResult("部署配置缺少 projectNames");
            return;
        }
        final Integer deployType = jsonObject.getIntValue("deployType", 2);
        final Boolean sync = jsonObject.getBooleanValue("sync", true);
        final Integer interval = jsonObject.getIntValue("interval", 0);
        String[] projectNames = projectNamesStr.split(",");
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (String projectName : projectNames) {
            String name = projectName.trim();
            if (name.isEmpty()) {
                continue;
            }
            futures.add(CompletableFuture.supplyAsync(
                    () -> this.deployProject(taskAlert, name, deployType, sync, interval)));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long failCount = futures.stream().filter(f -> {
            try {
                return !Boolean.TRUE.equals(f.get());
            } catch (Exception e) {
                return true;
            }
        }).count();
        if (failCount > 0) {
            record.setState(1);
            record.setResult("部署失败，共 " + failCount + " 个项目部署异常");
        } else {
            record.setResult("部署完成，共 " + futures.size() + " 个项目");
        }
    }

    /**
     * 部署单个项目
     */
    private Boolean deployProject(TaskAlert taskAlert, String projectName,
                                  Integer deployType, Boolean sync, Integer interval) {
        try {
            DeployRequest deployRequest = new DeployRequest();
            deployRequest.setDeployType(deployType);
            deployRequest.setProjectName(projectName);
            deployRequest.setSync(sync);
            deployRequest.setInterval(interval);

            List<DockerContainer> list = iDockerContainerService.getByServiceIdAndDockerName(taskAlert.getServiceId(), projectName);
            if (CollectionUtils.isEmpty(list)) {
                log.warn("定时部署：docker 不存在 projectName:{}", projectName);
                return false;
            }
            List<Long> agentIds = Lists.newArrayList();
            for (DockerContainer dockerContainer : list) {
                agentIds.add(dockerContainer.getAgentId());
            }
            deployRequest.setAgentIds(agentIds);
            iProjectService.deploy(deployRequest).join();
            return true;
        } catch (Exception e) {
            log.error("定时部署项目异常 projectName:{}", projectName, e);
            return false;
        }
    }

}
