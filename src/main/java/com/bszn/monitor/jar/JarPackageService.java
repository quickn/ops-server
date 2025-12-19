package com.bszn.monitor.jar;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigMapper;
import com.bszn.monitor.docker.DockerContainer;
import com.bszn.monitor.docker.DockerContainerMapper;
import com.bszn.monitor.msg.IMsgService;
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

    @Value("${jar.upload.path:/opt/jars}")
    private String uploadPath;

    @Value("${docker.base.image:openjdk:8-jre-slim}")
    private String dockerBaseImage;

    @Value("${server.host:localhost}")
    private String serverHost;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * 上传JAR包
     */
    public JarPackage uploadJar(MultipartFile file, String remark) throws IOException {
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
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 保存文件
        String saveFileName = fileName + "-" + newVersion + ".jar";
        String filePath = uploadPath + File.separator + saveFileName;
        file.transferTo(new File(filePath));

        // 生成下载URL
        String downloadUrl = String.format("http://%s:%s/jar/download/%s",
                serverHost, serverPort, saveFileName);

        // 生成默认Docker镜像名称
        String dockerImageName = fileName.toLowerCase() + ":" + newVersion;
        String dockerContainerName = fileName.toLowerCase() + "-" + newVersion.replace(".", "-");

        // 保存记录
        JarPackage jarPackage = new JarPackage();
        jarPackage.setFileName(fileName);
        jarPackage.setOriginalName(originalName);
        jarPackage.setVersion(newVersion);
        jarPackage.setRemark(remark);
        jarPackage.setJarPath(filePath);
        jarPackage.setDownloadUrl(downloadUrl);
        jarPackage.setDockerImageName(dockerImageName);
        jarPackage.setDockerContainerName(dockerContainerName);
        jarPackage.setDockerfilePath("");
        jarPackage.setStatus(0); // 未部署
        jarPackage.setCreateTime(new Date());
        jarPackage.setUpdateTime(new Date());

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
            jarPackage.setUpdateTime(new Date());
            jarPackageMapper.updateById(jarPackage);
            return true;
        } catch (Exception e) {
            log.error("更新Dockerfile失败", e);
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
    public CompletableFuture<Boolean> deploy(Integer jarPackageId, List<Long> agentIds, List<String> containerNames) {
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
        jarPackage.setUpdateTime(new Date());
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
                return deployWithDockerfile(jarPackage, agentId, containerName, record.getId());
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
                    jarPackage.setUpdateTime(new Date());
                    jarPackageMapper.updateById(jarPackage);

                    log.info("JAR包首次部署完成: id={}, success={}", jarPackageId, allSuccess);
                    return allSuccess;
                });
    }

    /**
     * 重新部署 - 只替换JAR包
     */
    @Async
    public CompletableFuture<Boolean> redeploy(Integer jarPackageId, List<Long> agentIds, List<String> containerNames) {
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
        jarPackage.setUpdateTime(new Date());
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
                return redeployJarOnly(jarPackage, agentId, containerName, record.getId());
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
                    jarPackage.setUpdateTime(new Date());
                    jarPackageMapper.updateById(jarPackage);

                    log.info("JAR包重新部署完成: id={}, success={}", jarPackageId, allSuccess);
                    return allSuccess;
                });
    }

    /**
     * 根据Dockerfile部署到Agent（使用脚本方式）
     */
    private boolean deployWithDockerfile(JarPackage jarPackage, Long agentId,
                                         String containerName, Integer recordId) {
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
            boolean containerExists = checkContainerExists(agentId, containerName);

            if (containerExists) {
                updateDeployRecord(recordId, 3, "容器名称已存在: " + containerName);
                return false;
            }

            // 3. 解析Dockerfile获取配置信息
            Map<String, String> dockerInfo = parseDockerfileInfo(jarPackage.getDockerfileContent());
            String exposedPort = dockerInfo.getOrDefault("EXPOSE", "8080");

            // 4. 生成部署脚本
            String deployScript = generateDeployScript(
                    jarPackage.getDownloadUrl(),
                    jarPackage.getFileName(),
                    jarPackage.getVersion(),
                    jarPackage.getDockerImageName(),
                    containerName,
                    jarPackage.getDockerfileContent(),
                    exposedPort,
                    dockerInfo
            );

            // 5. 将脚本保存为可下载文件
            String scriptFileName = "deploy_" + containerName + "_" + System.currentTimeMillis() + ".sh";
            String scriptPath = uploadPath + File.separator + scriptFileName;
            try (FileWriter writer = new FileWriter(scriptPath)) {
                writer.write(deployScript);
            }

            // 6. 生成脚本下载URL
            String scriptDownloadUrl = String.format("http://%s:%s/jar/download/%s",
                    serverHost, serverPort, scriptFileName);

            // 7. 在Agent端下载并执行脚本
            updateDeployRecord(recordId, 1, "下载并执行部署脚本...");

            // 构建下载和执行命令
            String remoteScriptPath = "/tmp/deploy_" + containerName + ".sh";
            String downloadCmd = String.format("curl -L -o %s '%s'", remoteScriptPath, scriptDownloadUrl);
            String chmodCmd = String.format("chmod +x %s", remoteScriptPath);
            String executeCmd = String.format("bash %s 2>&1", remoteScriptPath);
            String cleanupCmd = String.format("rm -f %s", remoteScriptPath);

            // 组合命令一次性执行
            String combinedCmd = String.format("%s && %s && %s && %s",
                    downloadCmd, chmodCmd, executeCmd, cleanupCmd);

            String scriptResult = msgService.sendCMDMsgAndResponse(agentId, combinedCmd);

            // 8. 清理本地脚本文件
            new File(scriptPath).delete();

            // 9. 解析脚本执行结果
            if (scriptResult.contains("DEPLOY_SUCCESS")) {
                updateDeployRecord(recordId, 2, "部署成功！\n" + extractDeploySuccessInfo(scriptResult));

                // 更新容器信息到数据库
                updateContainerInfo(agentId, containerName, jarPackage.getDockerImageName());
                return true;
            } else {
                String errorInfo = extractDeployErrorInfo(scriptResult);
                updateDeployRecord(recordId, 3, "部署失败:\n" + errorInfo);
                return false;
            }

        } catch (Exception e) {
            log.error("根据Dockerfile部署失败", e);
            updateDeployRecord(recordId, 3, "部署失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 重新部署：只替换JAR包（使用脚本方式）
     */
    private boolean redeployJarOnly(JarPackage jarPackage, Long agentId, String containerName, Integer recordId) {
        try {
            AgentConfig agentConfig = agentConfigMapper.selectById(agentId);
            if (agentConfig == null) {
                updateDeployRecord(recordId, 3, "Agent不存在");
                return false;
            }

            // 1. 检查容器是否存在
            updateDeployRecord(recordId, 1, "检查容器状态...");
            boolean containerExists = checkContainerExists(agentId, containerName);

            if (!containerExists) {
                updateDeployRecord(recordId, 3, "容器不存在，请先部署");
                return false;
            }

            // 2. 生成重新部署脚本
            String redeployScript = generateRedeployScript(
                    jarPackage.getDownloadUrl(),
                    jarPackage.getFileName(),
                    jarPackage.getVersion(),
                    containerName
            );

            // 3. 将脚本保存为可下载文件
            String scriptFileName = "redeploy_" + containerName + "_" + System.currentTimeMillis() + ".sh";
            String scriptPath = uploadPath + File.separator + scriptFileName;
            try (FileWriter writer = new FileWriter(scriptPath)) {
                writer.write(redeployScript);
            }

            // 4. 生成脚本下载URL
            String scriptDownloadUrl = String.format("http://%s:%s/jar/download/%s",
                    serverHost, serverPort, scriptFileName);

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

            String scriptResult = msgService.sendCMDMsgAndResponse(agentId, combinedCmd);

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
     * 生成部署脚本
     */
    private String generateDeployScript(String downloadUrl, String fileName, String version,
                                        String imageName, String containerName, String dockerfileContent,
                                        String exposedPort, Map<String, String> dockerInfo) {
        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n\n");
        script.append("# JAR包首次部署脚本\n");
        script.append("# 文件: ").append(fileName).append("-").append(version).append(".jar\n");
        script.append("# 容器: ").append(containerName).append("\n");
        script.append("# 镜像: ").append(imageName).append("\n\n");

        script.append("set -e  # 遇到错误立即退出\n");
        script.append("set -o pipefail  # 管道命令错误也退出\n\n");

        script.append("# 定义函数\n");
        script.append("log_info() {\n");
        script.append("    echo \"[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1\"\n");
        script.append("}\n\n");

        script.append("log_error() {\n");
        script.append("    echo \"[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1\" >&2\n");
        script.append("}\n\n");

        script.append("cleanup() {\n");
        script.append("    log_info \"清理临时文件...\"\n");
        script.append("    rm -rf /tmp/build-* /tmp/*.jar 2>/dev/null || true\n");
        script.append("}\n\n");

        script.append("check_command() {\n");
        script.append("    if ! command -v $1 &> /dev/null; then\n");
        script.append("        log_error \"命令 $1 未安装\"\n");
        script.append("        return 1\n");
        script.append("    fi\n");
        script.append("}\n\n");

        script.append("# 设置trap，确保脚本退出时清理\n");
        script.append("trap cleanup EXIT\n\n");

        script.append("# 检查必要命令\n");
        script.append("log_info \"检查系统命令...\"\n");
        script.append("check_command curl\n");
        script.append("check_command docker\n\n");

        script.append("# 定义变量\n");
        script.append("JAR_URL=\"").append(downloadUrl).append("\"\n");
        script.append("JAR_NAME=\"").append(fileName).append("-").append(version).append(".jar\"\n");
        script.append("JAR_PATH=\"/tmp/$JAR_NAME\"\n");
        script.append("IMAGE_NAME=\"").append(imageName).append("\"\n");
        script.append("CONTAINER_NAME=\"").append(containerName).append("\"\n");
        script.append("BUILD_DIR=\"/tmp/build-${CONTAINER_NAME}-$(date +%s)\"\n\n");

        script.append("# 检查容器是否已存在\n");
        script.append("log_info \"检查容器是否已存在...\"\n");
        script.append("if docker ps -a --filter \"name=^${CONTAINER_NAME}$\" --format '{{.Names}}' | grep -q \"${CONTAINER_NAME}\"; then\n");
        script.append("    log_error \"容器 ${CONTAINER_NAME} 已存在\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 下载JAR文件\n");
        script.append("log_info \"下载JAR文件: $JAR_NAME\"\n");
        script.append("if ! curl -L -o \"$JAR_PATH\" \"$JAR_URL\"; then\n");
        script.append("    log_error \"JAR文件下载失败\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 检查文件是否下载成功\n");
        script.append("if [ ! -f \"$JAR_PATH\" ]; then\n");
        script.append("    log_error \"JAR文件不存在\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 创建构建目录\n");
        script.append("log_info \"创建构建目录...\"\n");
        script.append("mkdir -p \"$BUILD_DIR\"\n\n");

        script.append("# 移动JAR文件到构建目录\n");
        script.append("log_info \"准备构建文件...\"\n");
        script.append("mv \"$JAR_PATH\" \"$BUILD_DIR/app.jar\"\n\n");

        script.append("# 创建Dockerfile\n");
        script.append("log_info \"创建Dockerfile...\"\n");
        script.append("cat > \"$BUILD_DIR/Dockerfile\" << 'EOF'\n");
        script.append(dockerfileContent).append("\n");
        script.append("EOF\n\n");

        script.append("# 构建Docker镜像\n");
        script.append("log_info \"构建Docker镜像: $IMAGE_NAME\"\n");
        script.append("cd \"$BUILD_DIR\"\n");
        script.append("if ! docker build -t \"$IMAGE_NAME\" .; then\n");
        script.append("    log_error \"Docker镜像构建失败\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 清理构建目录\n");
        script.append("log_info \"清理构建目录...\"\n");
        script.append("rm -rf \"$BUILD_DIR\"\n\n");

        script.append("# 构建docker run命令\n");
        script.append("RUN_CMD=\"docker run -d --name $CONTAINER_NAME --restart=always\"\n");
        script.append("RUN_CMD=\"$RUN_CMD -p ").append(exposedPort).append(":").append(exposedPort).append("\"\n");

        // 添加环境变量
        for (Map.Entry<String, String> entry : dockerInfo.entrySet()) {
            if (entry.getKey().startsWith("ENV_")) {
                String envKey = entry.getKey().substring(4);
                script.append("RUN_CMD=\"$RUN_CMD -e ").append(envKey).append("=").append(entry.getValue()).append("\"\n");
            }
        }

        // 设置时区（如果没有在Dockerfile中设置）
        if (!dockerInfo.containsKey("ENV_TZ")) {
            script.append("RUN_CMD=\"$RUN_CMD -e TZ=Asia/Shanghai\"\n");
        }

        script.append("RUN_CMD=\"$RUN_CMD $IMAGE_NAME\"\n\n");

        script.append("# 运行容器\n");
        script.append("log_info \"启动容器: $CONTAINER_NAME\"\n");
        script.append("log_info \"执行命令: $RUN_CMD\"\n");
        script.append("if ! eval \"$RUN_CMD\"; then\n");
        script.append("    log_error \"容器启动失败\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 等待容器启动\n");
        script.append("log_info \"等待容器启动...\"\n");
        script.append("sleep 5\n\n");

        script.append("# 检查容器状态\n");
        script.append("log_info \"检查容器状态...\"\n");
        script.append("CONTAINER_STATUS=$(docker inspect -f '{{.State.Status}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("if [ \"$CONTAINER_STATUS\" != \"running\" ]; then\n");
        script.append("    log_error \"容器状态异常: $CONTAINER_STATUS\"\n");
        script.append("    log_error \"容器日志:\"\n");
        script.append("    docker logs \"$CONTAINER_NAME\" --tail=20 2>/dev/null || echo \"无法获取日志\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 获取容器信息\n");
        script.append("CONTAINER_ID=$(docker inspect -f '{{.Id}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("CONTAINER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("CONTAINER_IMAGE=$(docker inspect -f '{{.Config.Image}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n\n");

        script.append("# 输出成功信息\n");
        script.append("log_info \"部署成功！\"\n");
        script.append("echo \"=== 部署成功信息 ===\"\n");
        script.append("echo \"容器名称: $CONTAINER_NAME\"\n");
        script.append("echo \"容器ID: $CONTAINER_ID\"\n");
        script.append("echo \"容器状态: $CONTAINER_STATUS\"\n");
        script.append("echo \"容器IP: $CONTAINER_IP\"\n");
        script.append("echo \"容器镜像: $CONTAINER_IMAGE\"\n");
        script.append("echo \"暴露端口: ").append(exposedPort).append("\"\n");
        script.append("echo \"部署时间: $(date '+%Y-%m-%d %H:%M:%S')\"\n");
        script.append("echo \"DEPLOY_SUCCESS\"\n");

        return script.toString();
    }

    /**
     * 重新部署脚本（只替换JAR包）
     */
    private String generateRedeployScript(String downloadUrl, String fileName, String version,
                                          String containerName) {
        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n\n");
        script.append("# JAR包重新部署脚本\n");
        script.append("# 文件: ").append(fileName).append("-").append(version).append(".jar\n");
        script.append("# 容器: ").append(containerName).append("\n\n");

        script.append("set -e  # 遇到错误立即退出\n");
        script.append("set -o pipefail  # 管道命令错误也退出\n\n");

        script.append("# 定义函数\n");
        script.append("log_info() {\n");
        script.append("    echo \"[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1\"\n");
        script.append("}\n\n");

        script.append("log_error() {\n");
        script.append("    echo \"[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1\" >&2\n");
        script.append("}\n\n");

        script.append("cleanup() {\n");
        script.append("    log_info \"清理临时文件...\"\n");
        script.append("    rm -f /tmp/*.jar 2>/dev/null || true\n");
        script.append("}\n\n");

        script.append("# 设置trap，确保脚本退出时清理\n");
        script.append("trap cleanup EXIT\n\n");

        script.append("# 定义变量\n");
        script.append("JAR_URL=\"").append(downloadUrl).append("\"\n");
        script.append("JAR_NAME=\"").append(fileName).append("-").append(version).append(".jar\"\n");
        script.append("JAR_PATH=\"/tmp/$JAR_NAME\"\n");
        script.append("CONTAINER_NAME=\"").append(containerName).append("\"\n\n");

        script.append("# 检查容器是否存在\n");
        script.append("log_info \"检查容器是否存在...\"\n");
        script.append("if ! docker ps -a --filter \"name=^${CONTAINER_NAME}$\" --format '{{.Names}}' | grep -q \"${CONTAINER_NAME}\"; then\n");
        script.append("    log_error \"容器 ${CONTAINER_NAME} 不存在\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 检查容器状态\n");
        script.append("CONTAINER_STATUS=$(docker inspect -f '{{.State.Status}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("WAS_RUNNING=false\n");
        script.append("if [ \"$CONTAINER_STATUS\" = \"running\" ]; then\n");
        script.append("    log_info \"容器正在运行，准备停止...\"\n");
        script.append("    WAS_RUNNING=true\n");
        script.append("    if ! docker stop \"$CONTAINER_NAME\"; then\n");
        script.append("        log_error \"停止容器失败\"\n");
        script.append("        exit 1\n");
        script.append("    fi\n");
        script.append("    sleep 2\n");
        script.append("fi\n\n");

        script.append("# 下载新JAR文件\n");
        script.append("log_info \"下载新JAR文件: $JAR_NAME\"\n");
        script.append("if ! curl -L -o \"$JAR_PATH\" \"$JAR_URL\"; then\n");
        script.append("    log_error \"JAR文件下载失败\"\n");
        script.append("    # 尝试恢复容器状态\n");
        script.append("    if [ \"$WAS_RUNNING\" = \"true\" ]; then\n");
        script.append("        docker start \"$CONTAINER_NAME\" 2>/dev/null || true\n");
        script.append("    fi\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 检查文件是否下载成功\n");
        script.append("if [ ! -f \"$JAR_PATH\" ]; then\n");
        script.append("    log_error \"JAR文件不存在\"\n");
        script.append("    # 尝试恢复容器状态\n");
        script.append("    if [ \"$WAS_RUNNING\" = \"true\" ]; then\n");
        script.append("        docker start \"$CONTAINER_NAME\" 2>/dev/null || true\n");
        script.append("    fi\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 复制新JAR文件到容器内部\n");
        script.append("log_info \"替换容器中的JAR文件...\"\n");
        script.append("if ! docker cp \"$JAR_PATH\" \"${CONTAINER_NAME}:/app/app.jar\"; then\n");
        script.append("    log_error \"复制JAR文件到容器失败\"\n");
        script.append("    # 尝试恢复容器状态\n");
        script.append("    if [ \"$WAS_RUNNING\" = \"true\" ]; then\n");
        script.append("        docker start \"$CONTAINER_NAME\" 2>/dev/null || true\n");
        script.append("    fi\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 启动容器（如果之前是运行状态）\n");
        script.append("if [ \"$WAS_RUNNING\" = \"true\" ]; then\n");
        script.append("    log_info \"启动容器...\"\n");
        script.append("    if ! docker start \"$CONTAINER_NAME\"; then\n");
        script.append("        log_error \"启动容器失败\"\n");
        script.append("        exit 1\n");
        script.append("    fi\n");
        script.append("    \n");
        script.append("    # 等待容器启动\n");
        script.append("    log_info \"等待容器启动...\"\n");
        script.append("    sleep 3\n");
        script.append("    \n");
        script.append("    # 检查容器状态\n");
        script.append("    NEW_STATUS=$(docker inspect -f '{{.State.Status}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("    if [ \"$NEW_STATUS\" != \"running\" ]; then\n");
        script.append("        log_error \"容器启动后状态异常: $NEW_STATUS\"\n");
        script.append("        log_error \"容器日志:\"\n");
        script.append("        docker logs \"$CONTAINER_NAME\" --tail=20 2>/dev/null || echo \"无法获取日志\"\n");
        script.append("        exit 1\n");
        script.append("    fi\n");
        script.append("fi\n\n");

        script.append("# 输出成功信息\n");
        script.append("log_info \"重新部署成功！\"\n");
        script.append("echo \"=== 重新部署成功信息 ===\"\n");
        script.append("echo \"容器名称: $CONTAINER_NAME\"\n");
        script.append("CONTAINER_ID=$(docker inspect -f '{{.Id}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("echo \"容器ID: $CONTAINER_ID\"\n");
        script.append("CONTAINER_STATUS=$(docker inspect -f '{{.State.Status}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("echo \"容器状态: $CONTAINER_STATUS\"\n");
        script.append("CONTAINER_IMAGE=$(docker inspect -f '{{.Config.Image}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("echo \"容器镜像: $CONTAINER_IMAGE\"\n");
        script.append("echo \"JAR版本: ").append(version).append("\"\n");
        script.append("echo \"部署时间: $(date '+%Y-%m-%d %H:%M:%S')\"\n");
        script.append("echo \"REDEPLOY_SUCCESS\"\n");

        return script.toString();
    }

    /**
     * 提取部署成功信息
     */
    private String extractDeploySuccessInfo(String scriptResult) {
        log.info("jar脚本部署：{}", scriptResult);
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
        log.info("jar脚本重新部署：{}", scriptResult);
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
        log.info("jar脚本部署错误：{}", scriptResult);
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
        for (String line : lines) {
            line = line.trim().toUpperCase();

            // 解析EXPOSE指令
            if (line.startsWith("EXPOSE")) {
                String[] parts = line.split("\\s+");
                if (parts.length > 1) {
                    info.put("EXPOSE", parts[1]);
                }
            }
            // 解析ENV指令
            else if (line.startsWith("ENV")) {
                String envPart = line.substring(3).trim();
                String[] envParts = envPart.split("\\s+");
                if (envParts.length >= 2) {
                    String key = envParts[0];
                    String value = envParts[1].replace("\"", "").replace("'", "");
                    info.put("ENV_" + key, value);
                }
            }
            // 解析WORKDIR指令
            else if (line.startsWith("WORKDIR")) {
                String[] parts = line.split("\\s+");
                if (parts.length > 1) {
                    info.put("WORKDIR", parts[1]);
                }
            }
        }

        return info;
    }

    /**
     * 检查容器是否存在
     */
    private boolean checkContainerExists(Long agentId, String containerName) {
        try {
            AgentConfig agent = agentConfigMapper.selectById(agentId);
            if (agent == null) {
                return false;
            }

            // 执行docker ps命令检查容器
            String checkCmd = String.format("docker ps -a --filter 'name=^%s$' --format '{{.Names}}'", containerName);
            String result = msgService.sendCMDMsgAndResponse(agentId, checkCmd);

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
                existing.setUpdateTime(new Date());
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
            jarPackage.setUpdateTime(new Date());

            jarPackageMapper.updateById(jarPackage);

        } catch (Exception e) {
            log.error("保存部署目标失败", e);
        }
    }

    /**
     * 创建部署记录
     */
    private JarDeployRecord createDeployRecord(Integer jarPackageId, Long agentId, String containerName) {
        JarDeployRecord record = new JarDeployRecord();
        record.setJarPackageId(jarPackageId);
        record.setAgentId(agentId);
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
     * 获取JAR包下载文件
     */
    public File getJarFileForDownload(String fileName) {
        File jarFile = new File(uploadPath + File.separator + fileName);
        if (!jarFile.exists()) {
            throw new IllegalArgumentException("文件不存在: " + fileName);
        }
        return jarFile;
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
    public Page<JarPackage> getJarPackagePage(Page<JarPackage> page, LambdaQueryWrapper<JarPackage> wrapper) {
        return jarPackageMapper.selectPage(page, wrapper);
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
}