package com.bszn.monitor.jar;

import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/jar")
@Tag(name = "JAR包管理", description = "JAR包上传、部署和管理")
@RequiredArgsConstructor
public class JarPackageController {

    private final JarPackageService jarPackageService;

    @PostMapping("/upload")
    @Operation(summary = "上传JAR包")
    public Result<Object> uploadJar(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "remark", defaultValue = "") String remark,
                                    @RequestParam("serviceId") Integer serviceId,
                                    @RequestParam("agentIds") String agentIdsStr,
                                    @RequestParam("containerNames") String containerNamesStr) {
        try {
            if (file.isEmpty()) {
                return Result.failed("请选择要上传的文件");
            }

            if (serviceId == null) {
                return Result.failed("请选择服务");
            }

            // 解析Agent IDs
            List<Long> agentIds = parseIds(agentIdsStr);
            if (agentIds.isEmpty()) {
                return Result.failed("请选择至少一个Agent");
            }

            // 解析容器名称
            List<String> containerNames = parseNames(containerNamesStr);
            if (containerNames.isEmpty()) {
                return Result.failed("请选择至少一个容器");
            }

            // 验证数量匹配
            if (agentIds.size() != containerNames.size()) {
                return Result.failed("Agent数量与容器数量必须一致");
            }

            JarPackage jarPackage = jarPackageService.uploadJar(file, remark, serviceId, agentIds, containerNames);

            Map<String, Object> data = new HashMap<>();
            data.put("jarPackage", jarPackage);
            data.put("message", "上传成功，请进行部署操作");

            return Result.success(data);

        } catch (IllegalArgumentException e) {
            log.error("参数错误", e);
            return Result.failed(e.getMessage());
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.failed("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("上传JAR包异常", e);
            return Result.failed("上传异常: " + e.getMessage());
        }
    }

    @PostMapping("/build/{id}")
    @Operation(summary = "初构建部署")
    public Result<Object> initialBuild(@PathVariable Integer id) {
        try {
            CompletableFuture<Boolean> future = jarPackageService.initialBuild(id);

            Map<String, Object> data = new HashMap<>();
            data.put("jarPackageId", id);
            data.put("message", "开始部署，请稍后查看部署状态");

            return Result.success(data);

        } catch (Exception e) {
            log.error("部署异常", e);
            return Result.failed("部署异常: " + e.getMessage());
        }
    }

    @PostMapping("/rebuild/{id}")
    @Operation(summary = "重新构建部署")
    public Result<Object> rebuild(@PathVariable Integer id) {
        try {
            CompletableFuture<Boolean> future = jarPackageService.rebuild(id);

            Map<String, Object> data = new HashMap<>();
            data.put("jarPackageId", id);
            data.put("message", "开始重新部署，请稍后查看部署状态");

            return Result.success(data);

        } catch (Exception e) {
            log.error("重新部署异常", e);
            return Result.failed("重新部署异常: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除JAR包")
    public Result<Object> deleteJar(@PathVariable Integer id) {
        try {
            boolean success = jarPackageService.deleteJar(id);
            if (success) {
                return Result.success("删除成功");
            }
            return Result.failed("删除失败，JAR包不存在");
        } catch (Exception e) {
            log.error("删除JAR包异常", e);
            return Result.failed("删除异常: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取JAR包详情")
    public Result<Object> getJarPackage(@PathVariable Integer id) {
        try {
            Map<String, Object> detail = jarPackageService.getJarPackageDetail(id);
            if (detail != null) {
                return Result.success(detail);
            }
            return Result.failed("JAR包不存在");
        } catch (Exception e) {
            log.error("获取JAR包详情异常", e);
            return Result.failed("获取详情异常: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    @Operation(summary = "获取JAR包列表")
    public Result<Object> listJarPackages(@RequestParam(value = "serviceId", required = false) Integer serviceId,
                                          @RequestParam(value = "status", required = false) Integer status) {
        try {
            List<JarPackage> jarPackages;
            if (serviceId != null) {
                jarPackages = jarPackageService.getJarPackagesByServiceId(serviceId);
            } else if (status != null) {
                jarPackages = jarPackageService.getJarPackagesByStatus(status);
            } else {
                jarPackages = jarPackageService.getAllJarPackages();
            }

            Map<String, Object> data = new HashMap<>();
            data.put("jarPackages", jarPackages);
            data.put("total", jarPackages.size());

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取JAR包列表异常", e);
            return Result.failed("获取列表异常: " + e.getMessage());
        }
    }

    @GetMapping("/available-agents/{serviceId}")
    @Operation(summary = "获取可用的Agent和容器")
    public Result<Object> getAvailableAgents(@PathVariable Integer serviceId) {
        try {
            List<Map<String, Object>> agents = jarPackageService.getAvailableAgentsWithContainers(serviceId);

            Map<String, Object> data = new HashMap<>();
            data.put("agents", agents);
            data.put("total", agents.size());

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取可用Agent列表异常", e);
            return Result.failed("获取可用Agent列表异常: " + e.getMessage());
        }
    }

    @GetMapping("/deploy-records/{id}")
    @Operation(summary = "获取部署记录")
    public Result<Object> getDeployRecords(@PathVariable Integer id) {
        try {
            List<JarDeployRecord> records = jarPackageService.getDeployRecords(id);

            Map<String, Object> data = new HashMap<>();
            data.put("deployRecords", records);
            data.put("total", records.size());

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取部署记录异常", e);
            return Result.failed("获取部署记录异常: " + e.getMessage());
        }
    }

    @GetMapping("/status/{id}")
    @Operation(summary = "获取部署状态")
    public Result<Object> getDeployStatus(@PathVariable Integer id) {
        try {
            Map<String, Object> detail = jarPackageService.getJarPackageDetail(id);
            if (detail == null) {
                return Result.failed("JAR包不存在");
            }

            JarPackage jarPackage = (JarPackage) detail.get("jarPackage");
            List<JarDeployRecord> records =
                    (List<JarDeployRecord>) detail.get("deployRecords");

            // 计算部署统计
            long total = records.size();
            long success = records.stream().filter(r -> r.getStatus() == 2).count();
            long failed = records.stream().filter(r -> r.getStatus() == 3).count();
            long deploying = records.stream().filter(r -> r.getStatus() == 1).count();
            long pending = records.stream().filter(r -> r.getStatus() == 0).count();

            Map<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("jarPackage", jarPackage);
            statusInfo.put("deployRecords", records);
            statusInfo.put("total", total);
            statusInfo.put("success", success);
            statusInfo.put("failed", failed);
            statusInfo.put("deploying", deploying);
            statusInfo.put("pending", pending);
            statusInfo.put("progress", total > 0 ? (success * 100 / total) : 0);

            return Result.success(statusInfo);
        } catch (Exception e) {
            log.error("获取部署状态异常", e);
            return Result.failed("获取部署状态异常: " + e.getMessage());
        }
    }

    /**
     * 解析ID字符串
     */
    private List<Long> parseIds(String idsStr) {
        if (StringUtils.isBlank(idsStr)) {
            return List.of();
        }
        try {
            return Arrays.stream(idsStr.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Agent ID格式错误");
        }
    }

    /**
     * 解析名称字符串
     */
    private List<String> parseNames(String namesStr) {
        if (StringUtils.isBlank(namesStr)) {
            return List.of();
        }
        return Arrays.stream(namesStr.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }


}