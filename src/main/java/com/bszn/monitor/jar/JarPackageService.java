package com.bszn.monitor.jar;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigMapper;
import com.bszn.monitor.agent.AgentConfigService;
import com.bszn.monitor.docker.DockerContainer;
import com.bszn.monitor.docker.DockerContainerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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

    private JarDeployRecordMapper jarDeployRecordMapper;

    private final AgentConfigService agentConfigService;

    @Value("${jar.upload.path:/opt/jars}")
    private String uploadPath;

    /**
     * 上传JAR包（自动生成Docker镜像名称）
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

        // 生成Docker镜像名称
        String dockerImageName = fileName.toLowerCase() + ":" + newVersion;

        // 保存记录
        JarPackage jarPackage = new JarPackage();
        jarPackage.setFileName(fileName);
        jarPackage.setOriginalName(originalName);
        jarPackage.setVersion(newVersion);
        jarPackage.setRemark(remark);
        jarPackage.setJarPath(filePath);
        jarPackage.setDockerImageName(dockerImageName);
        jarPackage.setStatus(0); // 未部署
        jarPackage.setCreateTime(new Date());
        jarPackage.setUpdateTime(new Date());

        jarPackageMapper.insert(jarPackage);
        return jarPackage;
    }

    /**
     * 获取所有Agent（用于部署选择）
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
                            .eq("status", "running")
            );

            if (!containers.isEmpty()) {
                Map<String, Object> agentInfo = new HashMap<>();
                agentInfo.put("agentId", agent.getId());
                agentInfo.put("agentName", agent.getHostname());
                agentInfo.put("containers", containers);
                result.add(agentInfo);
            }
        }

        return result;
    }

    /**
     * 部署JAR包
     */
    @Async
    public CompletableFuture<Boolean> deploy(Integer jarPackageId, List<Long> agentIds, List<String> containerNames) {
        JarPackage jarPackage = jarPackageMapper.selectById(jarPackageId);
        if (jarPackage == null) {
            log.error("JAR包不存在: id={}", jarPackageId);
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

            // 异步执行部署
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                return deployToAgent(jarPackage, agentId, containerName, record.getId());
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

                    log.info("JAR包部署完成: id={}, success={}", jarPackageId, allSuccess);
                    return allSuccess;
                });
    }

    /**
     * 重新部署
     */
    @Async
    public CompletableFuture<Boolean> redeploy(Integer jarPackageId) {
        JarPackage jarPackage = jarPackageMapper.selectById(jarPackageId);
        if (jarPackage == null || jarPackage.getStatus() != 2) {
            log.error("JAR包不存在或未部署成功");
            return CompletableFuture.completedFuture(false);
        }

        // 获取之前的部署目标
        List<Long> agentIds = getAgentIdList(jarPackage.getAgentIds());
        List<String> containerNames = getContainerNameList(jarPackage.getTargetContainerNames());

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

            // 异步执行重新部署
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                return redeployToAgent(jarPackage, agentId, containerName, record.getId());
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
            jarPackage.setUpdateTime(new Date());

            jarPackageMapper.updateById(jarPackage);

        } catch (Exception e) {
            log.error("保存部署目标失败", e);
        }
    }

    /**
     * 部署到单个Agent
     */
    private boolean deployToAgent(JarPackage jarPackage, Long agentId, String containerName, Integer recordId) {
        try {
            AgentConfig agentConfig = agentConfigMapper.selectById(agentId);
            if (agentConfig == null) {
                updateDeployRecord(recordId, 3, "Agent不存在");
                return false;
            }

            // 1. 复制JAR包到容器
            updateDeployRecord(recordId, 1, "正在复制JAR包...");
            String copyCmd = String.format("docker cp %s %s:/app/app.jar",
                    jarPackage.getJarPath(), containerName);
            agentConfigService.sendCmd(agentId, copyCmd);

            // 2. 重启容器
            updateDeployRecord(recordId, 1, "正在重启容器...");
            String restartCmd = String.format("docker restart %s", containerName);
            agentConfigService.sendCmd(agentId, restartCmd);

            // 3. 等待重启完成
            Thread.sleep(5000);

            updateDeployRecord(recordId, 2, "部署成功");
            return true;

        } catch (Exception e) {
            log.error("部署失败", e);
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
                updateDeployRecord(recordId, 3, "Agent不存在");
                return false;
            }

            // 1. 复制新JAR包到容器
            updateDeployRecord(recordId, 1, "正在复制新JAR包...");
            String copyCmd = String.format("docker cp %s %s:/app/app.jar",
                    jarPackage.getJarPath(), containerName);
            agentConfigService.sendCmd(agentId, copyCmd);

            // 2. 重启容器
            updateDeployRecord(recordId, 1, "正在重启容器...");
            String restartCmd = String.format("docker restart %s", containerName);
            agentConfigService.sendCmd(agentId, restartCmd);

            // 3. 等待重启完成
            Thread.sleep(3000);

            updateDeployRecord(recordId, 2, "重新部署成功");
            return true;

        } catch (Exception e) {
            log.error("重新部署失败", e);
            updateDeployRecord(recordId, 3, "重新部署失败: " + e.getMessage());
            return false;
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
     * 获取部署记录
     */
    public List<JarDeployRecord> getDeployRecords(Integer jarPackageId) {
        return jarDeployRecordMapper.selectByJarPackageId(jarPackageId);
    }

    /**
     * 获取JAR包列表
     */
    public List<JarPackage> getJarPackageList() {
        return jarPackageMapper.selectList(
                new QueryWrapper<JarPackage>().orderByDesc("create_time")
        );
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