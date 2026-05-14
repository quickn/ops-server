package com.bszn.monitor.jar;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bszn.monitor.agent.Agent;
import com.bszn.monitor.agent.AgentMapper;
import com.bszn.monitor.docker.DockerContainer;
import com.bszn.monitor.docker.DockerContainerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JarPackageService extends ServiceImpl<JarPackageMapper, JarPackage> {

    private final JarPackageMapper jarPackageMapper;
    private final AgentMapper agentConfigMapper;
    private final DockerContainerMapper dockerContainerMapper;

    @Value("${file.upload.file-path}")
    private String filePath;

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

        // 保存记录
        JarPackage jarPackage = new JarPackage();
        jarPackage.setFileName(fileName);
        jarPackage.setOriginalName(saveFileName);
        jarPackage.setVersion(newVersion);
        jarPackage.setRemark(remark);
        jarPackage.setJarPath(filePath);
        jarPackage.setDownloadUrl(downloadUrl);
        jarPackage.setServiceId(serviceId);
        jarPackage.setServiceName(serviceName);
        jarPackageMapper.insert(jarPackage);
        return jarPackage;
    }


    /**
     * 获取所有Agent（包含没有容器的Agent）
     */
    public List<Map<String, Object>> getAllAgents() {
        // 获取所有Agent
        List<Agent> allAgents = agentConfigMapper.selectList(
                new QueryWrapper<Agent>().eq("is_monitor", 1)
        );

        List<Map<String, Object>> result = new ArrayList<>();

        for (Agent agent : allAgents) {
            // 获取该Agent上的容器
            List<DockerContainer> containers = dockerContainerMapper.selectList(
                    new QueryWrapper<DockerContainer>()
                            .eq("hostname", agent.getHostname())
            );

            Map<String, Object> agentInfo = new HashMap<>();
            agentInfo.put("agentId", agent.getId());
            agentInfo.put("agentName", agent.getHostname());
            agentInfo.put("containers", containers);
            agentInfo.put("hasContainers", !containers.isEmpty());
            agentInfo.put("containerCount", containers.size());

            result.add(agentInfo);
        }

        return result;
    }

    /**
     * 获取JAR包列表（带分页）
     */
    public IPage<JarPackage> getJarPackagePage(Page<JarPackage> page, QueryWrapper<JarPackage> wrapper) {
        return jarPackageMapper.selectLatestVersionByPage(page, wrapper);
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
            // 删除JAR包记录
            jarPackageMapper.deleteById(id);
            return true;
        } catch (Exception e) {
            log.error("删除JAR包失败", e);
            return false;
        }
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


}