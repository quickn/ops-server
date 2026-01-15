package com.bszn.monitor.jar;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigMapper;
import com.bszn.monitor.agent.AgentConfigService;
import com.bszn.monitor.docker.DockerContainer;
import com.bszn.monitor.docker.DockerContainerMapper;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.utils.ScriptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JarPackageService extends ServiceImpl<JarPackageMapper, JarPackage> {

    private final JarPackageMapper jarPackageMapper;
    private final AgentConfigMapper agentConfigMapper;
    private final DockerContainerMapper dockerContainerMapper;
    private final JarDeployRecordMapper jarDeployRecordMapper;
    private final IMsgService msgService;
    private final AgentConfigService agentConfigService;

    @Value("${file.upload.file-path}")
    private String filePath;

    @Value("${file.upload.jar-path}")
    private String jarPath;

    @Value("${file.upload.down-path}")
    private String downPath;

    /**
     * 上传JAR包
     */
    public JarPackage uploadJar(MultipartFile file, Integer serviceId, String serviceName, String remark) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.endsWith(".jar")) {
            throw new IllegalArgumentException("文件必须是JAR格式");
        }

        String fileName = originalName.replace(".jar", "");

        // 获取最新版本
        String maxVersion = jarPackageMapper.selectMaxVersion(fileName);
        String newVersion = "1.0.0";
        if (maxVersion != null) {
            newVersion = incrementVersion(maxVersion);
        }

        // 创建上传目录
        File uploadDir = new File(filePath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 保存文件
        String saveFileName = fileName + "-" + newVersion + ".jar";
        String filePath = this.filePath + File.separator + saveFileName;
        file.transferTo(new File(filePath));

        // 生成下载URL
        String downloadUrl = String.format(downPath + "/%s", saveFileName);

        // 生成默认Docker镜像名称
        String dockerImageName = fileName.toLowerCase() + ":" + newVersion;
        String dockerContainerName = fileName.toLowerCase() + "-" + newVersion.replace(".", "-");

        // 保存记录
        JarPackage jarPackage = new JarPackage();
        jarPackage.setFileName(fileName);
        jarPackage.setOriginalName(saveFileName);
        jarPackage.setVersion(newVersion);
        jarPackage.setRemark(remark);
        jarPackage.setJarPath(filePath);
        jarPackage.setDownloadUrl(downloadUrl);
        jarPackage.setDockerImageName(dockerImageName);
        jarPackage.setDockerContainerName(dockerContainerName);
        jarPackage.setStatus(0); // 未部署
        jarPackage.setServiceId(serviceId);
        jarPackage.setServiceName(serviceName);

        jarPackageMapper.insert(jarPackage);
        return jarPackage;
    }

    /**
     * 更新Dockerfile
     */
    public boolean updateDockerfile(Integer id, String dockerfileContent) {
        try {
            JarPackage jarPackage = jarPackageMapper.selectById(id);
            if (jarPackage == null) {
                throw new IllegalArgumentException("JAR包不存在");
            }

            if (StringUtils.isBlank(dockerfileContent)) {
                throw new IllegalArgumentException("Dockerfile内容不能为空");
            }

            jarPackage.setDockerfileContent(dockerfileContent);
            jarPackageMapper.updateById(jarPackage);
            return true;
        } catch (Exception e) {
            log.error("更新Dockerfile失败", e);
            throw e;
        }
    }

    /**
     * 更新DockerCompose
     */
    public boolean updateDockerCompose(Integer id, String dockerComposeContent) {
        try {
            JarPackage jarPackage = jarPackageMapper.selectById(id);
            if (jarPackage == null) {
                throw new IllegalArgumentException("JAR包不存在");
            }

            if (StringUtils.isBlank(dockerComposeContent)) {
                throw new IllegalArgumentException("DockerCompose内容不能为空");
            }

            jarPackage.setDockerComposeContent(dockerComposeContent);
            jarPackageMapper.updateById(jarPackage);
            return true;
        } catch (Exception e) {
            log.error("更新DockerCompose失败", e);
            throw e;
        }
    }

    /**
     * 获取所有Agent（包含没有容器的Agent）
     */
    public List<Map<String, Object>> getAllAgents() {
        // 获取所有Agent
        List<AgentConfig> allAgents = agentConfigMapper.selectList(
                new QueryWrapper<AgentConfig>().eq("is_monitor", 1)
        );

        List<Map<String, Object>> result = new ArrayList<>();

        for (AgentConfig agent : allAgents) {
            // 获取该Agent上的容器
            List<DockerContainer> containers = dockerContainerMapper.selectList(
                    new QueryWrapper<DockerContainer>()
                            .eq("hostname", agent.getHostname())
            );

            Map<String, Object> agentInfo = new HashMap<>();
            agentInfo.put("agentId", agent.getId());
            agentInfo.put("agentName", agent.getHostname());
//            agentInfo.put("agentIp", agent.getAgentIp());
//            agentInfo.put("agentPort", agent.getAgentPort());
            agentInfo.put("containers", containers);
            agentInfo.put("hasContainers", !containers.isEmpty());
            agentInfo.put("containerCount", containers.size());

            result.add(agentInfo);
        }

        return result;
    }

    /**
     * 首次部署 - 根据Dockerfile自动创建容器
     */
    @Async
    public CompletableFuture<Boolean> deploy(Integer jarPackageId, List<Long> agentIds, List<String> containerNames, Long userId) {
        JarPackage jarPackage = jarPackageMapper.selectById(jarPackageId);
        if (jarPackage == null) {
            log.error("JAR包不存在: id={}", jarPackageId);
            return CompletableFuture.completedFuture(false);
        }

        // 验证Dockerfile是否已设置
        if (StringUtils.isBlank(jarPackage.getDockerfileContent())) {
            log.error("首次部署需要设置Dockerfile");
            return CompletableFuture.completedFuture(false);
        }

        // 验证参数
        if (agentIds.isEmpty() || containerNames.isEmpty()) {
            log.error("部署参数不能为空");
            return CompletableFuture.completedFuture(false);
        }

        if (agentIds.size() != containerNames.size()) {
            log.error("Agent数量与容器数量不匹配");
            return CompletableFuture.completedFuture(false);
        }

        // 检查容器名称是否重复
        Set<String> containerNameSet = new HashSet<>(containerNames);
        if (containerNameSet.size() != containerNames.size()) {
            log.error("容器名称不能重复");
            return CompletableFuture.completedFuture(false);
        }

        // 保存部署目标
        saveDeploymentTargets(jarPackage, agentIds, containerNames);

        // 更新状态为部署中
        jarPackage.setStatus(1);
        jarPackageMapper.updateById(jarPackage);

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // 为每个Agent创建部署任务
        for (int i = 0; i < agentIds.size(); i++) {
            Long agentId = agentIds.get(i);
            String containerName = containerNames.get(i);

            // 创建部署记录
            JarDeployRecord record = createDeployRecord(jarPackageId, agentId, containerName);

            // 异步执行首次部署（根据Dockerfile创建容器）
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                return deployWithDockerfile(jarPackage, agentId, containerName, record.getId(), userId);
            });

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
                    jarPackage.setStatus(allSuccess ? 2 : 3);
                    jarPackageMapper.updateById(jarPackage);

                    log.info("JAR包首次部署完成: id={}, success={}", jarPackageId, allSuccess);
                    return allSuccess;
                });
    }

    /**
     * 重新部署 - 只替换JAR包
     */
    @Async
    public CompletableFuture<Boolean> redeploy(Integer jarPackageId, List<Long> agentIds, List<String> containerNames, Long userId) {
        JarPackage jarPackage = jarPackageMapper.selectById(jarPackageId);
        if (jarPackage == null) {
            log.error("JAR包不存在: id={}", jarPackageId);
            return CompletableFuture.completedFuture(false);
        }

        // 如果提供了新的部署目标，则更新目标
        if (agentIds != null && !agentIds.isEmpty() && containerNames != null && !containerNames.isEmpty()) {
            if (agentIds.size() != containerNames.size()) {
                log.error("Agent数量与容器数量不匹配");
                return CompletableFuture.completedFuture(false);
            }
            saveDeploymentTargets(jarPackage, agentIds, containerNames);
        } else {
            // 使用原来的部署目标
            agentIds = getAgentIdList(jarPackage.getAgentIds());
            containerNames = getContainerNameList(jarPackage.getTargetContainerNames());
        }

        if (agentIds.isEmpty() || containerNames.isEmpty()) {
            log.error("没有部署目标");
            return CompletableFuture.completedFuture(false);
        }

        // 更新状态为部署中
        jarPackage.setStatus(1);
        jarPackageMapper.updateById(jarPackage);

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // 为每个Agent创建重新部署任务
        for (int i = 0; i < agentIds.size(); i++) {
            Long agentId = agentIds.get(i);
            String containerName = containerNames.get(i);

            // 创建部署记录
            JarDeployRecord record = createDeployRecord(jarPackageId, agentId, containerName);

            // 异步执行重新部署（只替换JAR包）
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                return redeployJarOnly(jarPackage, agentId, containerName, record.getId(), userId);
            });

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
                            log.error("获取重新部署结果失败", e);
                            allSuccess = false;
                        }
                    }

                    // 更新JAR包状态
                    jarPackage.setStatus(allSuccess ? 2 : 3);
                    jarPackageMapper.updateById(jarPackage);

                    log.info("JAR包重新部署完成: id={}, success={}", jarPackageId, allSuccess);
                    return allSuccess;
                });
    }

    /**
     * 智能部署：自动判断使用首次部署还是重新部署
     */
    @Async
    public CompletableFuture<Boolean> deployWithAutoStrategy(Integer jarPackageId,
                                                             List<Long> agentIds,
                                                             List<String> containerNames, Long userId) {
        JarPackage jarPackage = jarPackageMapper.selectById(jarPackageId);
        if (jarPackage == null) {
            log.error("JAR包不存在: id={}", jarPackageId);
            return CompletableFuture.completedFuture(false);
        }

        // 检查哪些容器已存在
        boolean allExist = true;
        boolean allNotExist = true;

        for (int i = 0; i < agentIds.size(); i++) {
            Long agentId = agentIds.get(i);
            String containerName = containerNames.get(i);

            boolean exists = checkContainerExists(agentId, containerName, userId);
            if (exists) {
                allNotExist = false;
            } else {
                allExist = false;
            }
        }

        // 智能选择策略
        if (allNotExist) {
            // 所有容器都不存在，使用首次部署
            log.info("所有容器都不存在，使用首次部署策略");
            return deploy(jarPackageId, agentIds, containerNames, userId);
        } else if (allExist) {
            // 所有容器都存在，使用重新部署
            log.info("所有容器都已存在，使用重新部署策略");
            return redeploy(jarPackageId, agentIds, containerNames, userId);
        } else {
            // 混合情况：部分存在，部分不存在
            log.info("容器状态混合，采用混合部署策略");
            return deployMixed(jarPackageId, agentIds, containerNames, userId);
        }
    }

    /**
     * 混合部署策略：对已存在的容器重新部署，对新容器首次部署
     */
    private CompletableFuture<Boolean> deployMixed(Integer jarPackageId,
                                                   List<Long> agentIds,
                                                   List<String> containerNames, Long userId) {
        JarPackage jarPackage = jarPackageMapper.selectById(jarPackageId);
        if (jarPackage == null) {
            return CompletableFuture.completedFuture(false);
        }

        // 保存部署目标
        saveDeploymentTargets(jarPackage, agentIds, containerNames);

        // 更新状态为部署中
        jarPackage.setStatus(1);
        jarPackageMapper.updateById(jarPackage);

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // 为每个部署目标创建任务
        for (int i = 0; i < agentIds.size(); i++) {
            Long agentId = agentIds.get(i);
            String containerName = containerNames.get(i);

            // 检查容器是否存在
            boolean containerExists = checkContainerExists(agentId, containerName, userId);

            // 创建部署记录
            JarDeployRecord record = createDeployRecord(jarPackageId, agentId, containerName);

            CompletableFuture<Boolean> future;

            if (containerExists) {
                // 容器已存在，使用重新部署
                future = CompletableFuture.supplyAsync(() -> redeployJarOnly(jarPackage, agentId, containerName, record.getId(), userId));
            } else {
                // 容器不存在，使用首次部署
                future = CompletableFuture.supplyAsync(() -> deployWithDockerfile(jarPackage, agentId, containerName, record.getId(), userId));
            }
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
                    jarPackage.setStatus(allSuccess ? 2 : 3);
                    jarPackageMapper.updateById(jarPackage);
                    log.info("混合部署完成: id={}, success={}", jarPackageId, allSuccess);
                    return allSuccess;
                });
    }

    /**
     * 简化版替换JAR包文件
     */
    public JarPackage replaceJarFile(Integer id, MultipartFile file) throws IOException {
        JarPackage jarPackage = jarPackageMapper.selectById(id);
        if (jarPackage == null) {
            throw new IllegalArgumentException("JAR包不存在");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.endsWith(".jar")) {
            throw new IllegalArgumentException("文件必须是JAR格式");
        }

        // 备份原文件
        File originalFile = new File(jarPackage.getJarPath());
        if (originalFile.exists()) {
            File backupFile = new File(jarPackage.getJarPath() + ".backup_" + System.currentTimeMillis());
            try {
                Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log.info("原文件已备份: {}", backupFile.getAbsolutePath());
            } catch (Exception e) {
                log.warn("文件备份失败: {}", e.getMessage());
            }
        }

        // 替换文件（直接覆盖）
        file.transferTo(originalFile);
        log.info("JAR包文件替换成功: {}", jarPackage.getOriginalName());

        // 更新记录
        jarPackageMapper.updateById(jarPackage);

        return jarPackage;
    }


    /**
     * 根据Dockerfile部署到Agent（使用精简版脚本）
     */
    private boolean deployWithDockerfile(JarPackage jarPackage, Long agentId,
                                         String containerName, Integer recordId, Long userId) {
        try {
            AgentConfig agentConfig = agentConfigMapper.selectById(agentId);
            if (agentConfig == null) {
                updateDeployRecord(recordId, 3, "Agent不存在");
                return false;
            }

            // 1. 检查Dockerfile是否已设置
            if (StringUtils.isBlank(jarPackage.getDockerfileContent())) {
                updateDeployRecord(recordId, 3, "Dockerfile未设置");
                return false;
            }

            // 2. 检查容器名称是否已存在
            updateDeployRecord(recordId, 1, "检查容器状态...");
            boolean containerExists = checkContainerExists(agentId, containerName, userId);

            if (containerExists) {
                updateDeployRecord(recordId, 3, "容器名称已存在: " + containerName);
                return false;
            }

            // 3. 生成精简版部署脚本（跳过JAR测试）
            String deployScript = ScriptUtil.deployScript(
                    jarPackage.getDownloadUrl(),
                    jarPackage.getFileName(),
                    jarPackage.getVersion(),
                    jarPackage.getDockerImageName(),
                    containerName,
                    jarPackage.getDockerfileContent(),
                    jarPackage.getDockerComposeContent(),
                    jarPath
            );

            // 4. 将脚本保存为可下载文件
            String scriptFileName = "deploy_" + containerName + "_simple_" + System.currentTimeMillis() + ".sh";
            String scriptPath = filePath + File.separator + scriptFileName;
            try (FileWriter writer = new FileWriter(scriptPath)) {
                writer.write(deployScript);
            }

            // 5. 生成脚本下载URL
            String scriptDownloadUrl = String.format(downPath + "/%s", scriptFileName);

            // 6. 在Agent端执行脚本
            updateDeployRecord(recordId, 1, "执行部署脚本...");

            // 使用更可靠的执行方式
            String remoteScriptPath = "/tmp/deploy_simple_" + containerName + ".sh";
            String downloadCmd = String.format("curl -s -L -o %s '%s'", remoteScriptPath, scriptDownloadUrl);
            String chmodCmd = String.format("chmod +x %s", remoteScriptPath);
            String executeCmd = String.format("bash -c '%s 2>&1'", remoteScriptPath);
            String cleanupCmd = String.format("rm -f %s", remoteScriptPath);

            // 组合命令（添加超时控制）
            String combinedCmd = String.format("timeout 300 %s && %s && %s && %s",
                    downloadCmd, chmodCmd, executeCmd, cleanupCmd);

            String scriptResult = msgService.sendCMDMsgAndResponse(userId, agentId, combinedCmd);

            // 7. 清理本地脚本文件
            new File(scriptPath).delete();

            // 8. 解析脚本执行结果
            if (scriptResult.contains("DEPLOY_SUCCESS")) {
                String successInfo = extractDeploySuccessInfo(scriptResult);
                updateDeployRecord(recordId, 2, "部署成功！\n" + successInfo);

                // 更新容器信息到数据库
                updateContainerInfo(agentId, containerName, jarPackage.getDockerImageName());
                return true;
            } else {
                String errorInfo = extractDeployErrorInfo(scriptResult);
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
     * 重新部署：只替换JAR包（使用脚本方式）
     */
    private boolean redeployJarOnly(JarPackage jarPackage, Long agentId, String containerName, Integer recordId, Long userId) {
        try {
            AgentConfig agentConfig = agentConfigMapper.selectById(agentId);
            if (agentConfig == null) {
                updateDeployRecord(recordId, 3, "Agent不存在");
                return false;
            }

            // 1. 检查容器是否存在
            updateDeployRecord(recordId, 1, "检查容器状态...");
            boolean containerExists = checkContainerExists(agentId, containerName, userId);

            if (!containerExists) {
                updateDeployRecord(recordId, 3, "容器不存在，请先部署");
                return false;
            }

            // 2. 生成重新部署脚本
            String redeployScript = ScriptUtil.redeployScript(
                    jarPackage.getDownloadUrl(),
                    jarPackage.getFileName(),
                    jarPackage.getVersion(),
                    containerName,
                    jarPackage.getDockerfileContent(),
                    jarPath
            );

            // 3. 将脚本保存为可下载文件
            String scriptFileName = "redeploy_" + containerName + "_" + System.currentTimeMillis() + ".sh";
            String scriptPath = filePath + File.separator + scriptFileName;
            try (FileWriter writer = new FileWriter(scriptPath)) {
                writer.write(redeployScript);
            }

            // 4. 生成脚本下载URL
            String scriptDownloadUrl = String.format(downPath + "/%s", scriptFileName);

            // 5. 在Agent端下载并执行脚本
            updateDeployRecord(recordId, 1, "下载并执行重新部署脚本...");

            // 构建下载和执行命令
            String remoteScriptPath = "/tmp/redeploy_" + containerName + ".sh";
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
            if (scriptResult.contains("REDEPLOY_SUCCESS")) {
                updateDeployRecord(recordId, 2, "重新部署成功！\n" + extractRedeploySuccessInfo(scriptResult));
                return true;
            } else {
                String errorInfo = extractDeployErrorInfo(scriptResult);
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
     * 提取部署成功信息
     */
    private String extractDeploySuccessInfo(String scriptResult) {
        StringBuilder info = new StringBuilder();
        String[] lines = scriptResult.split("\n");
        boolean inSuccessSection = false;

        for (String line : lines) {
            if (line.contains("=== 部署成功信息 ===")) {
                inSuccessSection = true;
                continue;
            }
            if (inSuccessSection && line.contains("DEPLOY_SUCCESS")) {
                break;
            }
            if (inSuccessSection) {
                info.append(line).append("\n");
            }
        }

        return info.toString().trim();
    }

    /**
     * 提取重新部署成功信息
     */
    private String extractRedeploySuccessInfo(String scriptResult) {
        StringBuilder info = new StringBuilder();
        String[] lines = scriptResult.split("\n");
        boolean inSuccessSection = false;

        for (String line : lines) {
            if (line.contains("=== 重新部署成功信息 ===")) {
                inSuccessSection = true;
                continue;
            }
            if (inSuccessSection && line.contains("REDEPLOY_SUCCESS")) {
                break;
            }
            if (inSuccessSection) {
                info.append(line).append("\n");
            }
        }

        return info.toString().trim();
    }

    /**
     * 提取部署错误信息
     */
    private String extractDeployErrorInfo(String scriptResult) {
        StringBuilder errorInfo = new StringBuilder();
        String[] lines = scriptResult.split("\n");

        for (String line : lines) {
            if (line.contains("[ERROR]")) {
                errorInfo.append(line).append("\n");
            }
        }

        if (errorInfo.length() == 0) {
            // 如果没有明确的错误信息，返回最后10行
            int start = Math.max(0, lines.length - 10);
            for (int i = start; i < lines.length; i++) {
                errorInfo.append(lines[i]).append("\n");
            }
        }

        return errorInfo.toString().trim();
    }

    /**
     * 解析Dockerfile获取配置信息
     */
    private Map<String, String> parseDockerfileInfo(String dockerfileContent) {
        Map<String, String> info = new HashMap<>();
        if (StringUtils.isBlank(dockerfileContent)) {
            return info;
        }
        String[] lines = dockerfileContent.split("\n");
        String[] instructions = {InstructionConstant.FROM, InstructionConstant.EXPOSE, InstructionConstant.WORKDIR, InstructionConstant.ENTRYPOINT};
        for (String line : lines) {
            line = line.trim();
            for (int i = 0; i < instructions.length; i++) {
                // 解析指令
                if (line.startsWith(instructions[i])) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 1) {
                        info.put(instructions[i], parts[1]);
                    }
                }
            }
        }
        return info;
    }

    /**
     * 检查容器是否存在
     */
    private boolean checkContainerExists(Long agentId, String containerName, Long userId) {
        try {
            AgentConfig agent = agentConfigMapper.selectById(agentId);
            if (agent == null) {
                return false;
            }

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
     * 更新容器信息到数据库
     */
    private void updateContainerInfo(Long agentId, String containerName, String imageName) {
        try {
            AgentConfig agent = agentConfigMapper.selectById(agentId);
            if (agent == null) {
                return;
            }

            // 检查容器是否已存在数据库中
            QueryWrapper<DockerContainer> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("hostname", agent.getHostname())
                    .eq("names", containerName);

            DockerContainer existing = dockerContainerMapper.selectOne(queryWrapper);

            if (existing == null) {
                // 创建新的容器记录
                DockerContainer container = new DockerContainer();
                container.setHostname(agent.getHostname());
                container.setNames(containerName);
                container.setImage(imageName);
                container.setStatus("running");
                container.setCreateTime(LocalDateTime.now());
                dockerContainerMapper.insert(container);
            } else {
                // 更新现有记录
                existing.setImage(imageName);
                existing.setStatus("running");
                dockerContainerMapper.updateById(existing);
            }

        } catch (Exception e) {
            log.error("更新容器信息失败", e);
        }
    }

    /**
     * 保存部署目标
     */
    private void saveDeploymentTargets(JarPackage jarPackage, List<Long> agentIds, List<String> containerNames) {
        try {
            // 获取Agent名称
            List<String> agentNameList = new ArrayList<>();
            for (Long agentId : agentIds) {
                AgentConfig agent = agentConfigMapper.selectById(agentId);
                if (agent != null) {
                    agentNameList.add(agent.getHostname());
                } else {
                    agentNameList.add("Agent-" + agentId);
                }
            }

            // 保存到JAR包记录
            jarPackage.setAgentIds(String.join(",", agentIds.stream().map(String::valueOf).collect(Collectors.toList())));
            jarPackage.setAgentNames(String.join(",", agentNameList));
            jarPackage.setTargetContainerNames(String.join(",", containerNames));
            jarPackage.setDockerContainerName(String.join(",", containerNames));

            jarPackageMapper.updateById(jarPackage);

        } catch (Exception e) {
            log.error("保存部署目标失败", e);
        }
    }

    /**
     * 创建部署记录
     */
    private JarDeployRecord createDeployRecord(Integer jarPackageId, Long agentId, String containerName) {
        AgentConfig byId = agentConfigService.getById(agentId);
        JarDeployRecord record = new JarDeployRecord();
        record.setJarPackageId(jarPackageId);
        record.setAgentId(agentId);
        record.setAgentIp(byId.getHostname());
        record.setContainerName(containerName);
        record.setStatus(0); // 待部署
        record.setCreateTime(new Date());
        record.setDeployLog("开始部署");
        jarDeployRecordMapper.insert(record);
        return record;
    }

    /**
     * 更新部署记录
     */
    private void updateDeployRecord(Integer recordId, Integer status, String logMsg) {
        try {
            JarDeployRecord record = jarDeployRecordMapper.selectById(recordId);
            if (record != null) {
                record.setStatus(status);
                if (status == 2 || status == 3) {
                    record.setDeployTime(new Date());
                }
                record.setDeployLog(logMsg);
                jarDeployRecordMapper.updateById(record);
            }
        } catch (Exception e) {
            log.error("更新部署记录失败", e);
        }
    }

    /**
     * 获取部署记录
     */
    public List<JarDeployRecord> getDeployRecords(Integer jarPackageId) {
        return jarDeployRecordMapper.selectByJarPackageId(jarPackageId);
    }

    /**
     * 获取JAR包列表（带分页）
     */
    public IPage<JarPackage> getJarPackagePage(Page<JarPackage> page, QueryWrapper<JarPackage> wrapper) {
        return jarPackageMapper.selectLatestVersionByPage(page, wrapper);
    }

    /**
     * 删除JAR包
     */
    public boolean deleteJarPackage(Integer id) {
        try {
            JarPackage jarPackage = jarPackageMapper.selectById(id);
            if (jarPackage == null) {
                return false;
            }

            // 删除物理文件
            File jarFile = new File(jarPackage.getJarPath());
            if (jarFile.exists()) {
                jarFile.delete();
            }

            // 删除部署记录
            jarDeployRecordMapper.delete(
                    new QueryWrapper<JarDeployRecord>().eq("jar_package_id", id)
            );

            // 删除JAR包记录
            jarPackageMapper.deleteById(id);

            return true;
        } catch (Exception e) {
            log.error("删除JAR包失败", e);
            return false;
        }
    }

    /**
     * 根据ID获取JAR包
     */
    public JarPackage getJarPackageById(Integer id) {
        return jarPackageMapper.selectById(id);
    }

    /**
     * 获取JAR包的状态统计
     */
    public Map<String, Object> getJarPackageStats(Integer id) {
        Map<String, Object> stats = new HashMap<>();

        JarPackage jarPackage = jarPackageMapper.selectById(id);
        if (jarPackage != null) {
            stats.put("jarPackage", jarPackage);

            List<JarDeployRecord> records = jarDeployRecordMapper.selectByJarPackageId(id);
            stats.put("deployRecords", records);

            long total = records.size();
            long success = records.stream().filter(r -> r.getStatus() == 2).count();
            long failed = records.stream().filter(r -> r.getStatus() == 3).count();
            long deploying = records.stream().filter(r -> r.getStatus() == 1).count();

            stats.put("total", total);
            stats.put("success", success);
            stats.put("failed", failed);
            stats.put("deploying", deploying);
            stats.put("progress", total > 0 ? (success * 100 / total) : 0);
        }

        return stats;
    }

    /**
     * 获取Agent ID列表
     */
    private List<Long> getAgentIdList(String agentIds) {
        if (StringUtils.isBlank(agentIds)) {
            return new ArrayList<>();
        }
        return Arrays.stream(agentIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    /**
     * 获取容器名称列表
     */
    private List<String> getContainerNameList(String containerNames) {
        if (StringUtils.isBlank(containerNames)) {
            return new ArrayList<>();
        }
        return Arrays.stream(containerNames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 版本号递增
     */
    private String incrementVersion(String version) {
        try {
            String[] parts = version.split("\\.");
            int patch = Integer.parseInt(parts[2]);
            return parts[0] + "." + parts[1] + "." + (patch + 1);
        } catch (Exception e) {
            return version + ".1";
        }
    }

    /**
     * 获取所有版本（按文件名）
     */
    public List<JarPackage> getAllVersionsByFileName(String fileName) {
        return lambdaQuery().eq(JarPackage::getFileName, fileName).orderByDesc(JarPackage::getId).list();
    }

    /**
     * 重新构建容器（使用Dockerfile重新构建镜像并部署）
     */
    @Async
    public CompletableFuture<Boolean> rebuildContainer(Integer jarPackageId, List<Long> agentIds, List<String> containerNames, Long userId) {
        JarPackage jarPackage = jarPackageMapper.selectById(jarPackageId);
        if (jarPackage == null) {
            log.error("JAR包不存在: id={}", jarPackageId);
            return CompletableFuture.completedFuture(false);
        }

        // 如果提供了新的部署目标，则更新目标
        if (agentIds != null && !agentIds.isEmpty() && containerNames != null && !containerNames.isEmpty()) {
            if (agentIds.size() != containerNames.size()) {
                log.error("Agent数量与容器数量不匹配");
                return CompletableFuture.completedFuture(false);
            }
            saveDeploymentTargets(jarPackage, agentIds, containerNames);
        } else {
            // 使用原来的部署目标
            agentIds = getAgentIdList(jarPackage.getAgentIds());
            containerNames = getContainerNameList(jarPackage.getTargetContainerNames());
        }

        if (agentIds.isEmpty() || containerNames.isEmpty()) {
            log.error("没有部署目标");
            return CompletableFuture.completedFuture(false);
        }

        // 更新状态为部署中
        jarPackage.setStatus(1);
        jarPackageMapper.updateById(jarPackage);

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // 为每个Agent创建重新构建任务
        for (int i = 0; i < agentIds.size(); i++) {
            Long agentId = agentIds.get(i);
            String containerName = containerNames.get(i);

            // 创建部署记录
            JarDeployRecord record = createDeployRecord(jarPackageId, agentId, containerName);

            // 异步执行重新构建
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                return rebuildWithDockerfile(jarPackage, agentId, containerName, record.getId(), userId);
            });

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
                            log.error("获取重新构建结果失败", e);
                            allSuccess = false;
                        }
                    }

                    // 更新JAR包状态
                    jarPackage.setStatus(allSuccess ? 2 : 3);
                    jarPackageMapper.updateById(jarPackage);

                    log.info("容器重新构建完成: id={}, success={}", jarPackageId, allSuccess);
                    return allSuccess;
                });
    }

    /**
     * 使用Dockerfile重新构建容器
     */
    private boolean rebuildWithDockerfile(JarPackage jarPackage, Long agentId,
                                          String containerName, Integer recordId, Long userId) {
        try {
            AgentConfig agentConfig = agentConfigMapper.selectById(agentId);
            if (agentConfig == null) {
                updateDeployRecord(recordId, 3, "Agent不存在");
                return false;
            }

            // 1. 检查Dockerfile是否已设置
            if (StringUtils.isBlank(jarPackage.getDockerfileContent())) {
                updateDeployRecord(recordId, 3, "Dockerfile未设置");
                return false;
            }

            // 2. 生成重新构建脚本（使用Dockerfile重新构建镜像）
            String rebuildScript = ScriptUtil.deployScript(
                    jarPackage.getDownloadUrl(),
                    jarPackage.getFileName(),
                    jarPackage.getVersion(),
                    jarPackage.getDockerImageName(),
                    containerName,
                    jarPackage.getDockerfileContent(),
                    jarPackage.getDockerComposeContent(),
                    jarPath
            );

            // 3. 将脚本保存为可下载文件
            String scriptFileName = "rebuild_" + containerName + "_" + System.currentTimeMillis() + ".sh";
            String scriptPath = filePath + File.separator + scriptFileName;
            try (FileWriter writer = new FileWriter(scriptPath)) {
                writer.write(rebuildScript);
            }

            // 4. 生成脚本下载URL
            String scriptDownloadUrl = String.format(downPath + "/%s", scriptFileName);

            // 5. 在Agent端下载并执行脚本
            updateDeployRecord(recordId, 1, "下载并执行重新构建脚本...");

            // 构建下载和执行命令
            String remoteScriptPath = "/tmp/rebuild_" + containerName + ".sh";
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
                updateDeployRecord(recordId, 2, "重新构建成功！\n" + extractDeploySuccessInfo(scriptResult));
                return true;
            } else {
                String errorInfo = extractDeployErrorInfo(scriptResult);
                updateDeployRecord(recordId, 3, "重新构建失败:\n" + errorInfo);
                return false;
            }

        } catch (Exception e) {
            log.error("重新构建失败", e);
            updateDeployRecord(recordId, 3, "重新构建失败: " + e.getMessage());
            return false;
        }
    }

}