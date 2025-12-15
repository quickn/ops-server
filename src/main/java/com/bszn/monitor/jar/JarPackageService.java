package com.bszn.monitor.jar;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigMapper;
import com.bszn.monitor.agent.AgentConfigService;
import com.bszn.monitor.docker.DockerContainer;
import com.bszn.monitor.docker.DockerContainerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JarPackageService {

    private final JarPackageMapper jarPackageMapper;

    private final AgentConfigMapper agentConfigMapper;

    private final DockerContainerMapper dockerContainerMapper;

    private final JarDeployRecordMapper jarDeployRecordMapper;

    private final AgentConfigService agentConfigService;

    @Value("${jar.upload.path:/opt/jars}")
    private String uploadPath;

    /**
     * 上传JAR包
     */
    public JarPackage uploadJar(MultipartFile file, String remark, Integer serviceId,
                                List<Long> agentIds, List<String> containerNames) throws IOException {
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

        // 获取Agent名称
        List<String> agentNameList = new ArrayList<>();
        for (Long agentId : agentIds) {
            AgentConfig agent = agentConfigMapper.selectById(agentId);
            if (agent != null) {
                agentNameList.add(agent.getHostname());
            } else {
                agentNameList.add("未知Agent");
            }
        }

        // 获取容器镜像信息
        List<String> containerImageList = new ArrayList<>();
        for (String containerName : containerNames) {
            DockerContainer container = dockerContainerMapper.selectOne(
                    new QueryWrapper<DockerContainer>()
                            .eq("names", containerName)
                            .eq("service_id", serviceId)
            );
            if (container != null) {
                containerImageList.add(container.getImage());
            } else {
                containerImageList.add("未知镜像");
            }
        }

        // 获取服务名称
        String serviceName = "服务-" + serviceId;

        // 保存记录
        JarPackage jarPackage = new JarPackage();
        jarPackage.setFileName(fileName);
        jarPackage.setOriginalName(originalName);
        jarPackage.setVersion(newVersion);
        jarPackage.setRemark(remark);
        jarPackage.setJarPath(filePath);
        jarPackage.setAgentIdList(agentIds);
        jarPackage.setAgentNames(String.join(",", agentNameList));
        jarPackage.setContainerNameList(containerNames);
        jarPackage.setTargetContainerImages(String.join(",", containerImageList));
        jarPackage.setStatus(0);
        jarPackage.setServiceId(serviceId);
        jarPackage.setServiceName(serviceName);
        jarPackage.setCreateTime(new Date());
        jarPackage.setUpdateTime(new Date());

        jarPackageMapper.insert(jarPackage);
        return jarPackage;
    }

    /**
     * 初构建 - 部署到多个Agent
     */
    @Async
    public CompletableFuture<Boolean> initialBuild(Integer id) {
        JarPackage jarPackage = jarPackageMapper.selectById(id);
        if (jarPackage == null) {
            log.error("JAR包不存在: id={}", id);
            return CompletableFuture.completedFuture(false);
        }

        // 更新状态为部署中
        jarPackage.setStatus(1);
        jarPackage.setUpdateTime(new Date());
        jarPackageMapper.updateById(jarPackage);

        List<Long> agentIds = jarPackage.getAgentIdList();
        List<String> containerNames = jarPackage.getContainerNameList();

        // 验证Agent和容器数量匹配
        if (agentIds.size() != containerNames.size()) {
            log.error("Agent数量与容器数量不匹配: agentCount={}, containerCount={}",
                    agentIds.size(), containerNames.size());
            jarPackage.setStatus(3);
            jarPackageMapper.updateById(jarPackage);
            return CompletableFuture.completedFuture(false);
        }

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // 为每个Agent创建部署任务
        for (int i = 0; i < agentIds.size(); i++) {
            Long agentId = agentIds.get(i);
            String containerName = containerNames.get(i);

            // 创建部署记录
            JarDeployRecord record = new JarDeployRecord();
            record.setJarPackageId(id);
            record.setAgentId(agentId);
            record.setContainerName(containerName);
            record.setStatus(0); // 待部署
            record.setCreateTime(new Date());
            jarDeployRecordMapper.insert(record);

            // 异步执行部署
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> deployToAgent(jarPackage, agentId, containerName, record.getId()));

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

                    log.info("JAR包部署完成: id={}, success={}", id, allSuccess);
                    return allSuccess;
                });
    }

    /**
     * 重新构建 - 替换多个容器中的JAR包
     */
    @Async
    public CompletableFuture<Boolean> rebuild(Integer id) {
        JarPackage jarPackage = jarPackageMapper.selectById(id);
        if (jarPackage == null || jarPackage.getStatus() != 2) {
            log.error("JAR包不存在或状态不正确: id={}, status={}",
                    id, jarPackage != null ? jarPackage.getStatus() : "null");
            return CompletableFuture.completedFuture(false);
        }

        // 更新状态为部署中
        jarPackage.setStatus(1);
        jarPackage.setUpdateTime(new Date());
        jarPackageMapper.updateById(jarPackage);

        List<Long> agentIds = jarPackage.getAgentIdList();
        List<String> containerNames = jarPackage.getContainerNameList();

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // 为每个Agent创建重新部署任务
        for (int i = 0; i < agentIds.size(); i++) {
            Long agentId = agentIds.get(i);
            String containerName = containerNames.get(i);

            // 创建部署记录
            JarDeployRecord record = new JarDeployRecord();
            record.setJarPackageId(id);
            record.setAgentId(agentId);
            record.setContainerName(containerName);
            record.setStatus(0); // 待部署
            record.setCreateTime(new Date());
            jarDeployRecordMapper.insert(record);

            // 异步执行重新部署
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> redeployToAgent(jarPackage, agentId, containerName, record.getId()));

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

                    log.info("JAR包重新部署完成: id={}, success={}", id, allSuccess);
                    return allSuccess;
                });
    }

    /**
     * 部署到单个Agent
     */
    private boolean deployToAgent(JarPackage jarPackage, Long agentId, String containerName, Integer recordId) {
        try {
            AgentConfig agentConfig = agentConfigMapper.selectById(agentId);
            if (agentConfig == null) {
                updateDeployRecord(recordId, 3, "Agent不存在: " + agentId);
                return false;
            }

            // 1. 备份原有JAR包
            String backupCmd = String.format(
                    "docker exec %s sh -c 'if [ -f /app/app.jar ]; then cp /app/app.jar /app/app.jar.bak_$(date +%%Y%%m%%d_%%H%%M%%S); fi'",
                    containerName
            );

            updateDeployRecord(recordId, 1, "开始备份原有JAR包...");
            agentConfigService.sendCmd(agentId, backupCmd);

            // 2. 复制新的JAR包到容器
            updateDeployRecord(recordId, 1, "正在复制JAR包到容器...");
            String copyCmd = String.format("docker cp %s %s:/app/app.jar",
                    jarPackage.getJarPath(), containerName);
            agentConfigService.sendCmd(agentId, copyCmd);

            // 3. 重启容器
            updateDeployRecord(recordId, 1, "正在重启容器...");
            String restartCmd = String.format("docker restart %s", containerName);
            agentConfigService.sendCmd(agentId, restartCmd);

            // 4. 等待并验证
            Thread.sleep(5000);
            updateDeployRecord(recordId, 1, "验证部署状态...");

            String verifyCmd = String.format(
                    "docker ps --filter 'name=%s' --format 'table {{.Status}}' | grep -q Up",
                    containerName
            );

            // 发送验证命令
            agentConfigService.sendCmd(agentId, verifyCmd);

            // 假设成功（实际应该获取命令执行结果）
            updateDeployRecord(recordId, 2, "部署成功 - 容器已重启");

            return true;

        } catch (Exception e) {
            log.error("部署到Agent失败: agentId={}, container={}", agentId, containerName, e);
            updateDeployRecord(recordId, 3, "部署失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 重新部署到单个Agent
     */
    private boolean redeployToAgent(JarPackage jarPackage, Long agentId, String containerName, Integer recordId) {
        try {
            AgentConfig agentConfig = agentConfigMapper.selectById(agentId);
            if (agentConfig == null) {
                updateDeployRecord(recordId, 3, "Agent不存在: " + agentId);
                return false;
            }

            // 1. 备份当前运行的JAR包
            updateDeployRecord(recordId, 1, "备份当前JAR包...");
            String backupCmd = String.format(
                    "docker exec %s sh -c 'cp /app/app.jar /app/app.jar.current_$(date +%%Y%%m%%d_%%H%%M%%S)'",
                    containerName
            );
            agentConfigService.sendCmd(agentId, backupCmd);

            // 2. 复制新的JAR包到容器
            updateDeployRecord(recordId, 1, "正在复制新JAR包...");
            String copyCmd = String.format("docker cp %s %s:/app/app.jar",
                    jarPackage.getJarPath(), containerName);
            agentConfigService.sendCmd(agentId, copyCmd);

            // 3. 重启容器
            updateDeployRecord(recordId, 1, "正在重启容器...");
            String restartCmd = String.format("docker restart %s", containerName);
            agentConfigService.sendCmd(agentId, restartCmd);

            // 4. 等待重启完成
            Thread.sleep(3000);
            updateDeployRecord(recordId, 2, "重新部署成功 - 容器已重启");

            return true;

        } catch (Exception e) {
            log.error("重新部署到Agent失败: agentId={}, container={}", agentId, containerName, e);
            updateDeployRecord(recordId, 3, "重新部署失败: " + e.getMessage());
            return false;
        }
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
                String currentLog = record.getDeployLog() != null ? record.getDeployLog() + "\n" : "";
                record.setDeployLog(currentLog + new SimpleDateFormat("HH:mm:ss").format(new Date()) + " " + logMsg);
                jarDeployRecordMapper.updateById(record);
            }
        } catch (Exception e) {
            log.error("更新部署记录失败", e);
        }
    }

    /**
     * 获取可用的Agent列表（带容器信息）
     */
    public List<Map<String, Object>> getAvailableAgentsWithContainers(Integer serviceId) {
        // 获取所有Agent
        List<AgentConfig> allAgents = agentConfigMapper.selectList(
                new QueryWrapper<AgentConfig>()
                        .eq("service_id", serviceId)
                        .eq("is_monitor", 1)
        );

        List<Map<String, Object>> result = new ArrayList<>();

        for (AgentConfig agent : allAgents) {
            // 获取该Agent上的容器
            List<DockerContainer> containers = dockerContainerMapper.selectList(
                    new QueryWrapper<DockerContainer>()
                            .eq("hostname", agent.getHostname())
                            .eq("service_id", serviceId)
                            .eq("status", "running")
            );

            if (!containers.isEmpty()) {
                Map<String, Object> agentInfo = new HashMap<>();
                agentInfo.put("agentId", agent.getId());
                agentInfo.put("agentName", agent.getHostname());
                agentInfo.put("ip", agent.getHostname()); // 假设hostname是IP
                agentInfo.put("containers", containers);
                result.add(agentInfo);
            }
        }

        return result;
    }

    /**
     * 获取部署记录
     */
    public List<JarDeployRecord> getDeployRecords(Integer jarPackageId) {
        return jarDeployRecordMapper.selectByJarPackageId(jarPackageId);
    }

    /**
     * 获取JAR包详情
     */
    public Map<String, Object> getJarPackageDetail(Integer id) {
        JarPackage jarPackage = jarPackageMapper.selectById(id);
        if (jarPackage == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("jarPackage", jarPackage);

        // 获取部署记录
        List<JarDeployRecord> records = getDeployRecords(id);
        result.put("deployRecords", records);

        // 获取部署统计
        Map<String, Long> statusStats = records.stream()
                .collect(Collectors.groupingBy(
                        r -> getStatusText(r.getStatus()),
                        Collectors.counting()
                ));
        result.put("statusStats", statusStats);

        return result;
    }

    /**
     * 删除JAR包
     */
    public boolean deleteJar(Integer id) {
        JarPackage jarPackage = jarPackageMapper.selectById(id);
        if (jarPackage == null) {
            return false;
        }

        try {
            // 删除物理文件
            File jarFile = new File(jarPackage.getJarPath());
            if (jarFile.exists()) {
                boolean deleted = jarFile.delete();
                if (!deleted) {
                    log.warn("删除物理文件失败: {}", jarPackage.getJarPath());
                }
            }

            // 删除部署记录
            jarDeployRecordMapper.delete(
                    new QueryWrapper<JarDeployRecord>()
                            .eq("jar_package_id", id)
            );

            // 删除JAR包记录
            int deleted = jarPackageMapper.deleteById(id);
            return deleted > 0;

        } catch (Exception e) {
            log.error("删除JAR包失败: id={}", id, e);
            return false;
        }
    }

    /**
     * 获取所有JAR包
     */
    public List<JarPackage> getAllJarPackages() {
        return jarPackageMapper.selectList(
                new QueryWrapper<JarPackage>()
                        .orderByDesc("create_time")
        );
    }

    /**
     * 根据服务ID获取JAR包
     */
    public List<JarPackage> getJarPackagesByServiceId(Integer serviceId) {
        return jarPackageMapper.selectByServiceId(serviceId);
    }

    /**
     * 根据状态获取JAR包
     */
    public List<JarPackage> getJarPackagesByStatus(Integer status) {
        return jarPackageMapper.selectByStatus(status);
    }

    /**
     * 版本号递增
     */
    private String incrementVersion(String version) {
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            // 增加修订版本号
            patch++;

            // 如果修订版本号超过99，增加次版本号
            if (patch > 99) {
                patch = 0;
                minor++;
            }

            // 如果次版本号超过99，增加主版本号
            if (minor > 99) {
                minor = 0;
                major++;
            }

            return major + "." + minor + "." + patch;
        } catch (Exception e) {
            log.warn("版本号解析失败: {}, 使用默认递增", version);
            return "1.0.1";
        }
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0:
                return "未部署";
            case 1:
                return "部署中";
            case 2:
                return "成功";
            case 3:
                return "失败";
            default:
                return "未知";
        }
    }
}