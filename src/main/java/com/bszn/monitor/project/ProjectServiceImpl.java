package com.bszn.monitor.project;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigService;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.system.common.exception.BusinessException;
import com.bszn.utils.ScriptUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author wzh
 * @date 2026/1/14 13:42
 * @description: 项目业务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements IProjectService {

    private final ProjectDeployRecordMapper projectDeployRecordMapper;

    private final AgentConfigService agentConfigService;

    private final IMsgService msgService;

    @Value("${file.upload.file-path}")
    private String filePath;

    @Value("${file.upload.jar-path}")
    private String jarPath;

    @Value("${file.upload.down-path}")
    private String downPath;

    /**
     * 部署
     *
     * @param projectId 项目id
     * @param agentIds  服务器id
     * @param userId    用户id
     * @return 结果
     */
    @SneakyThrows
    @Override
    public CompletableFuture<Boolean> deploy(Long projectId, List<Long> agentIds, Long userId) {
        return deploy(projectId, agentIds, userId, 1);
    }


    /**
     * 重新部署（只替换JAR包）
     *
     * @param projectId 项目id
     * @param agentIds  服务器id
     * @param userId    用户id
     * @return 结果
     */
    @Override
    public CompletableFuture<Boolean> redeploy(Long projectId, List<Long> agentIds, Long userId) {
        return deploy(projectId, agentIds, userId, 2);
    }

    /**
     * 获取部署记录
     *
     * @param projectId 项目id
     * @return 部署记录
     */
    @Override
    public List<ProjectDeployRecord> getDeployRecords(Long projectId) {
        return projectDeployRecordMapper.selectByProjectId(projectId);
    }

    /**
     * 部署
     *
     * @param projectId  项目id
     * @param agentIds   服务器id
     * @param userId     用户id
     * @param deployType 部署类型 1 构建容器 2 替换jar包
     * @return 结果
     */
    private CompletableFuture<Boolean> deploy(Long projectId, List<Long> agentIds, Long userId, Integer deployType) {
        Project project = getById(projectId);
        if (Objects.isNull(project)) {
            throw new BusinessException("项目不存在");
        }
        if (StrUtil.isEmpty(project.getDockerfileContent())) {
            throw new BusinessException("尚未设置Dockerfile");
        }
        // 更新状态为部署中
        project.setStatus(1);
        this.updateById(project);

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        // 为每个Agent创建部署任务
        for (int i = 0; i < agentIds.size(); i++) {
            Long agentId = agentIds.get(i);

            // 创建部署记录
            ProjectDeployRecord record = createDeployRecord(projectId, agentId, project.getName());

            // 异步执行首次部署（根据Dockerfile创建容器）
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> deployType == 1 ?
                    deployWithDockerfile(project, agentId, record.getId(), userId)
                    : redeployJarOnly(project, agentId, record.getId(), userId));
            futures.add(future);
        }

        // 等待所有部署完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    boolean allSuccess = true;
                    for (CompletableFuture<Boolean> future : futures) {
                        try {
                            if (!future.get()) {
                                allSuccess = false;
                            }
                        } catch (Exception e) {
                            log.error("获取部署结果失败", e);
                            allSuccess = false;
                        }
                    }
                    // 更新JAR包状态
                    project.setStatus(allSuccess ? 2 : 3);
                    this.updateById(project);
                    log.info("部署完成: id={}, success={}", projectId, allSuccess);
                    return allSuccess;
                });
    }

    /**
     * 重新部署（替换jar包）
     *
     * @param project  项目
     * @param agentId  服务id
     * @param recordId 部署记录id
     * @param userId   用户id
     * @return 结果
     */
    private boolean redeployJarOnly(Project project, Long agentId, Long recordId, Long userId) {
        try {
            // 检查容器是否存在
            updateDeployRecord(recordId, 1, "检查容器状态...");
            boolean containerExists = checkContainerExists(agentId, project.getName(), userId);

            if (!containerExists) {
                updateDeployRecord(recordId, 3, "容器不存在，请先部署");
                return false;
            }
            // 2. 生成重新部署脚本
            String redeployScript = ScriptUtil.redeployScript(project.getName(), project.getDockerfileContent(), jarPath);


            // 3. 将脚本保存为可下载文件
            String scriptFileName = "redeploy_" + project.getName() + "_" + System.currentTimeMillis() + ".sh";
            String scriptPath = filePath + File.separator + scriptFileName;
            try (FileWriter writer = new FileWriter(scriptPath)) {
                writer.write(redeployScript);
            }

            // 4. 生成脚本下载URL
            String scriptDownloadUrl = String.format(downPath + "/%s", scriptFileName);

            // 5. 在Agent端下载并执行脚本
            updateDeployRecord(recordId, 1, "下载并执行重新部署脚本...");

            // 构建下载和执行命令
            String remoteScriptPath = "/tmp/redeploy_" + project.getName() + ".sh";
            String downloadCmd = String.format("curl -L -o %s '%s'", remoteScriptPath, scriptDownloadUrl);
            String chmodCmd = String.format("chmod +x %s", remoteScriptPath);
            String executeCmd = String.format("bash %s 2>&1", remoteScriptPath);
            String cleanupCmd = String.format("rm -f %s", remoteScriptPath);

            // 组合命令一次性执行
            String combinedCmd = String.format("%s && %s && %s && %s",
                    downloadCmd, chmodCmd, executeCmd, cleanupCmd);

            String scriptResult = msgService.sendCMDMsgAndResponse(userId, agentId, combinedCmd);

            // 6. 清理本地脚本文件
            new File(scriptPath).delete();

            // 7. 解析脚本执行结果
            if (scriptResult.contains("DEPLOY_SUCCESS")) {
                updateDeployRecord(recordId, 2, "重新部署成功！\n" + ScriptUtil.extractDeploySuccessInfo(scriptResult));
                return true;
            } else {
                String errorInfo = ScriptUtil.extractDeployErrorInfo(scriptResult);
                updateDeployRecord(recordId, 3, "重新部署失败:\n" + errorInfo);
                return false;
            }
        } catch (Exception e) {
            log.error("重新部署失败", e);
            updateDeployRecord(recordId, 3, "重新部署失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 部署
     *
     * @param project  项目
     * @param agentId  服务id
     * @param recordId 部署记录id
     * @param userId   用户id
     * @return 结果
     */
    private Boolean deployWithDockerfile(Project project, Long agentId, Long recordId, Long userId) {
        try {
            // 获取脚本
            String deployScript = ScriptUtil.deployScript(project.getName(), project.getDockerfileContent(), project.getDockerComposeContent(), jarPath);

            // 将脚本保存为可下载文件
            String scriptFileName = "deploy_" + project.getName() + "_simple_" + System.currentTimeMillis() + ".sh";
            String scriptPath = filePath + File.separator + scriptFileName;
            try (FileWriter writer = new FileWriter(scriptPath)) {
                writer.write(deployScript);
            }

            // 生成脚本下载URL
            String scriptDownloadUrl = String.format(downPath + "/%s", scriptFileName);

            // 在Agent端执行脚本
            updateDeployRecord(recordId, 1, "执行部署脚本...");

            // 执行命令
            String remoteScriptPath = "/tmp/deploy_simple_" + project.getName() + ".sh";
            String downloadCmd = String.format("curl -s -L -o %s '%s'", remoteScriptPath, scriptDownloadUrl);
            String chmodCmd = String.format("chmod +x %s", remoteScriptPath);
            String executeCmd = String.format("bash -c '%s 2>&1'", remoteScriptPath);
            String cleanupCmd = String.format("rm -f %s", remoteScriptPath);

            // 组合命令（添加超时控制）
            String combinedCmd = String.format("timeout 300 %s && %s && %s && %s",
                    downloadCmd, chmodCmd, executeCmd, cleanupCmd);

            String scriptResult = msgService.sendCMDMsgAndResponse(userId, agentId, combinedCmd);

            // 清理本地脚本文件
            new File(scriptPath).delete();

            // 解析脚本执行结果
            if (scriptResult.contains("DEPLOY_SUCCESS")) {
                String successInfo = ScriptUtil.extractDeploySuccessInfo(scriptResult);
                updateDeployRecord(recordId, 2, "部署成功！\n" + successInfo);
                return true;
            } else {
                String errorInfo = ScriptUtil.extractDeployErrorInfo(scriptResult);
                updateDeployRecord(recordId, 3, "部署失败:\n" + errorInfo);
                return false;
            }
        } catch (Exception e) {
            log.error("部署失败", e);
            updateDeployRecord(recordId, 3, "部署失败: " + e.getMessage());
            return false;
        }

    }

    /**
     * 检查容器是否存在
     *
     * @param agentId       服务器id
     * @param containerName 容器名
     * @param userId        用户id
     * @return 是否存在 true 存在 false 不存在
     */
    private boolean checkContainerExists(Long agentId, String containerName, Long userId) {
        try {
            // 执行docker ps命令检查容器
            String checkCmd = String.format("docker ps -a --filter 'name=^%s$' --format '{{.Names}}'", containerName);
            String result = msgService.sendCMDMsgAndResponseNon(userId, agentId, checkCmd);
            return StringUtils.isNotBlank(result) && result.trim().equals(containerName);
        } catch (Exception e) {
            log.error("检查容器存在失败", e);
            return false;
        }
    }

    /**
     * 创建部署记录
     *
     * @param projectId     项目id
     * @param agentId       服务器id
     * @param containerName 容器名
     * @return 部署记录
     */
    private ProjectDeployRecord createDeployRecord(Long projectId, Long agentId, String containerName) {
        AgentConfig byId = agentConfigService.getById(agentId);
        ProjectDeployRecord record = new ProjectDeployRecord();
        record.setProjectId(projectId);
        record.setAgentId(agentId);
        record.setAgentIp(byId.getHostname());
        record.setContainerName(containerName);
        record.setStatus(0); // 待部署
        record.setCreateTime(new Date());
        record.setDeployLog("开始部署");
        projectDeployRecordMapper.insert(record);
        return record;
    }

    /**
     * 更新部署记录
     *
     * @param recordId 部署记录id
     * @param status   状态
     * @param logMsg   日志
     */
    private void updateDeployRecord(Long recordId, Integer status, String logMsg) {
        ProjectDeployRecord record = projectDeployRecordMapper.selectById(recordId);
        if (record != null) {
            record.setStatus(status);
            if (status == 2 || status == 3) {
                record.setDeployTime(new Date());
            }
            record.setDeployLog(logMsg);
            projectDeployRecordMapper.updateById(record);
        }
    }

}
