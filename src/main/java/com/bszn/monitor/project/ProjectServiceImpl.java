package com.bszn.monitor.project;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bszn.monitor.agent.Agent;
import com.bszn.monitor.agent.AgentService;
import com.bszn.monitor.file.IFileService;
import com.bszn.monitor.file.SyncFileParam;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.system.common.exception.BusinessException;
import com.bszn.system.common.util.SecurityUtils;
import com.bszn.utils.IpUtil;
import com.bszn.utils.ScriptUtil;
import jakarta.annotation.Resource;
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

    private final AgentService agentConfigService;

    private final IMsgService msgService;

    @Value("${file.upload.file-path}")
    private String filePath;

    @Value("${file.upload.jar-path}")
    private String jarPath;

    @Value("${file.upload.down-path}")
    private String downPath;

    @Value("${file.upload.work-path}")
    private String workPath;

    @Resource
    IFileService fileService;

    @SneakyThrows
    @Override
    public CompletableFuture<Boolean> deploy(DeployRequest deployRequest) {
        if (deployRequest.getProjectId() == null && StringUtils.isEmpty(deployRequest.getProjectName())) {
            throw new BusinessException("项目ID或项目名称不能为空");
        }
        return deploy(deployRequest.getProjectId(), deployRequest.getProjectName(), deployRequest.getAgentIds(), SecurityUtils.getUserId(), deployRequest.getDeployType());
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
     * 同步
     *
     * @param syncRequest 请求参数
     * @return 结果
     */
    @Override
    public Boolean sync(SyncRequest syncRequest) {
        Project project = getByIdOrName(syncRequest.getProjectId(), syncRequest.getProjectName());
        if (Objects.isNull(project)) {
            throw new BusinessException("项目不存在");
        }
        String sourceDir = StrUtil.isEmpty(project.getSourceDir()) ? workPath + this.jarPath : project.getSourceDir() + File.separator + project.getName();
        SyncFileParam syncFileParam = new SyncFileParam();
        syncFileParam.setSourceAgentId(syncRequest.getSourceAgentId());
        syncFileParam.setJumpServiceId(syncRequest.getServiceId());
        syncFileParam.setSourcePath(sourceDir);
        syncFileParam.setSyncType(syncRequest.getType());
        syncFileParam.setTargetPath(project.getTargetDir());
        fileService.syncFileByJumpServer(syncFileParam);
        return true;
    }

    /**
     * 备份/恢复
     *
     * @param userId        用户id
     * @param backupRequest 请求参数
     * @return 结果
     */
    @Override
    public Boolean backup(Long userId, BackupRequest backupRequest) {

        String sourceDir = StrUtil.isEmpty(backupRequest.getSourceDir()) ?
                workPath + jarPath : backupRequest.getSourceDir();
        String targetDir = StrUtil.isEmpty(backupRequest.getTargetDir()) ?
                workPath + jarPath + "_bak" : backupRequest.getTargetDir();

        if (backupRequest.getType() == 1 && Objects.equals(sourceDir, targetDir)) {
            throw new BusinessException("2个目录相同无法备份");
        }
        // 获取跳板机
        Agent jump = agentConfigService.getjumpServers(backupRequest.getServiceId());

        // 根据类型决定复制方向
        String fromDir, toDir;
        String operation;
        if (backupRequest.getType() == 1) {
            // 备份：docker -> docker_bak
            fromDir = sourceDir;
            toDir = targetDir;
            operation = "备份";
        } else {
            // 恢复：docker_bak -> docker
            fromDir = targetDir;
            toDir = sourceDir;
            operation = "恢复";
        }

        StringBuilder result = new StringBuilder();

        StringBuilder cmd = new StringBuilder();
        // 备份才需要删除原目录
        if (backupRequest.getType() == 1) {
            cmd.append(String.format("rm -rf %s && ", toDir));
        }
        cmd.append(String.format("cp -r %s %s", fromDir, toDir));

        String cmdResult = msgService.sendCMDMsgAndResponse(
                userId,
                jump.getId(),
                cmd.toString(),
                300
        );

        result.append("服务器 ").append(jump.getHostname())
                .append(" ").append(operation).append("结果：").append(cmdResult)
                .append(" === 分隔线 === ");

        log.info("{}结果：{}", operation, result);
        return true;
    }

    private Project getByIdOrName(Long projectId, String projectName) {
        return projectId != null ? getById(projectId) : getOne(new LambdaQueryWrapper<Project>().eq(Project::getName, projectName));
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
    private CompletableFuture<Boolean> deploy(Long projectId, String projectName, List<Long> agentIds, Long userId, Integer deployType) {
        Project project = getByIdOrName(projectId, projectName);
        if (Objects.isNull(project)) {
            throw new BusinessException("项目不存在");
        }
        if (StrUtil.isEmpty(project.getDockerfileContent())) {
            throw new BusinessException("尚未设置Dockerfile");
        }
        // 更新状态为部署中
        Project projectTemp = new Project();
        projectTemp.setId(projectId);
        projectTemp.setStatus(1);
        this.updateById(projectTemp);

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
                    projectTemp.setStatus(allSuccess ? 2 : 3);
                    this.updateById(projectTemp);
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
            int type = project.getType() == 1 ? 3 : project.getType();
            String jarPath = StrUtil.isEmpty(project.getTargetDir()) ? workPath + this.jarPath : project.getTargetDir();
            String redeployScript = ScriptUtil.deployScript(project.getName(), project.getDockerfileContent(), project.getDockerComposeContent(), jarPath, type);


            // 3. 将脚本保存为可下载文件
            String scriptFileName = "redeploy_" + project.getName() + "_" + System.currentTimeMillis() + ".sh";
            String scriptPath = workPath + filePath + File.separator + scriptFileName;
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
            String jarPath = StrUtil.isEmpty(project.getTargetDir()) ? workPath + this.jarPath : project.getTargetDir();
            String deployScript = ScriptUtil.deployScript(project.getName(), project.getDockerfileContent(), project.getDockerComposeContent(), jarPath, project.getType());

            // 将脚本保存为可下载文件
            String scriptFileName = "deploy_" + project.getName() + ".sh";
            String scriptPath = workPath + filePath + File.separator + scriptFileName;
            new File(scriptPath).delete();
            try (FileWriter writer = new FileWriter(scriptPath)) {
                writer.write(deployScript);
            }
            if (downPath.contains("$ip")) {
                downPath = downPath.replace("$ip", IpUtil.getIPv4Ip());
            }
            // 生成脚本下载URL
            String scriptDownloadUrl = String.format(downPath + "/%s", scriptFileName);

            // 在Agent端执行脚本
            updateDeployRecord(recordId, 1, "执行部署脚本...");

            // 执行命令
            String remoteScriptPath = String.format("/tmp/deploy_%s.sh", project.getName());
            String downloadCmd = String.format("curl -s -L -o %s '%s'", remoteScriptPath, scriptDownloadUrl);
            String chmodCmd = String.format("chmod +x %s", remoteScriptPath);
            String executeCmd = String.format("bash -c '%s 2>&1'", remoteScriptPath);
            String cleanupCmd = String.format("rm -f %s", remoteScriptPath);

            // 组合命令（添加超时控制）
            String combinedCmd = String.format("%s && timeout 300 %s && %s && %s ",
                    cleanupCmd, downloadCmd, chmodCmd, executeCmd);

            String scriptResult = msgService.sendCMDMsgAndResponse(userId, agentId, combinedCmd);


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
        Agent byId = agentConfigService.getById(agentId);
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
