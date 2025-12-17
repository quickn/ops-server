package com.bszn.monitor.jar;

import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/jar")
@Tag(name = "JAR包管理", description = "JAR包上传、部署和管理")
@RequiredArgsConstructor
public class JarPackageController {

    private final JarPackageService jarPackageService;

    @PostMapping("/upload")
    @Operation(summary = "上传JAR包")
    public Result<JarPackage> uploadJar(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "remark", defaultValue = "") String remark) {
        try {
            if (file.isEmpty()) {
                return Result.failed("请选择要上传的文件");
            }

            JarPackage jarPackage = jarPackageService.uploadJar(file, remark);
            return Result.success(jarPackage);

        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            log.error("上传JAR包异常", e);
            return Result.failed("上传异常: " + e.getMessage());
        }
    }

    @GetMapping("/agents")
    @Operation(summary = "获取所有Agent")
    public Result<List<Map<String, Object>>> getAllAgents() {
        try {
            List<Map<String, Object>> agents = jarPackageService.getAllAgents();
            return Result.success(agents);
        } catch (Exception e) {
            log.error("获取Agent列表异常", e);
            return Result.failed("获取Agent列表失败");
        }
    }

    @PostMapping("/deploy/{id}")
    @Operation(summary = "部署JAR包")
    public Result<String> deploy(@PathVariable Integer id,
                                 @RequestBody DeployRequest request) {
        try {
            if (request.getAgentIds() == null || request.getAgentIds().isEmpty()) {
                return Result.failed("请选择Agent");
            }

            if (request.getContainerNames() == null || request.getContainerNames().isEmpty()) {
                return Result.failed("请选择容器");
            }

            if (request.getAgentIds().size() != request.getContainerNames().size()) {
                return Result.failed("Agent数量与容器数量必须一致");
            }

            CompletableFuture<Boolean> future = jarPackageService.deploy(
                    id, request.getAgentIds(), request.getContainerNames()
            );

            return Result.success("开始部署，请稍后查看状态");

        } catch (Exception e) {
            log.error("部署异常", e);
            return Result.failed("部署异常: " + e.getMessage());
        }
    }

    @PostMapping("/redeploy/{id}")
    @Operation(summary = "重新部署")
    public Result<String> redeploy(@PathVariable Integer id) {
        try {
            CompletableFuture<Boolean> future = jarPackageService.redeploy(id);
            return Result.success("开始重新部署");
        } catch (Exception e) {
            log.error("重新部署异常", e);
            return Result.failed("重新部署异常: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    @Operation(summary = "获取JAR包列表")
    public Result<List<JarPackage>> listJarPackages() {
        try {
            List<JarPackage> jarPackages = jarPackageService.getJarPackageList();
            return Result.success(jarPackages);
        } catch (Exception e) {
            log.error("获取JAR包列表异常", e);
            return Result.failed("获取列表失败");
        }
    }

    @GetMapping("/deploy-records/{id}")
    @Operation(summary = "获取部署记录")
    public Result<List<JarDeployRecord>> getDeployRecords(@PathVariable Integer id) {
        try {
            List<JarDeployRecord> records = jarPackageService.getDeployRecords(id);
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取部署记录异常", e);
            return Result.failed("获取部署记录失败");
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除JAR包")
    public Result<String> deleteJarPackage(@PathVariable Integer id) {
        try {
            boolean success = jarPackageService.deleteJarPackage(id);
            if (success) {
                return Result.success("删除成功");
            }
            return Result.failed("删除失败");
        } catch (Exception e) {
            log.error("删除JAR包异常", e);
            return Result.failed("删除异常: " + e.getMessage());
        }
    }

    @GetMapping("/status/{id}")
    @Operation(summary = "获取部署状态")
    public Result<Map<String, Object>> getDeployStatus(@PathVariable Integer id) {
        try {
            // 获取JAR包信息
            JarPackage jarPackage = jarPackageService.getJarPackageById(id);
            if (jarPackage == null) {
                return Result.failed("JAR包不存在");
            }

            // 获取部署记录
            List<JarDeployRecord> records = jarPackageService.getDeployRecords(id);

            // 计算统计信息
            Map<String, Object> status = new java.util.HashMap<>();
            status.put("jarPackage", jarPackage);
            status.put("deployRecords", records);

            long total = records.size();
            long success = records.stream().filter(r -> r.getStatus() == 2).count();
            long failed = records.stream().filter(r -> r.getStatus() == 3).count();
            long deploying = records.stream().filter(r -> r.getStatus() == 1).count();

            status.put("total", total);
            status.put("success", success);
            status.put("failed", failed);
            status.put("deploying", deploying);
            status.put("progress", total > 0 ? (success * 100 / total) : 0);

            return Result.success(status);

        } catch (Exception e) {
            log.error("获取部署状态异常", e);
            return Result.failed("获取部署状态失败");
        }
    }

    // 请求参数类
    @Data
    public static class DeployRequest {
        private List<Long> agentIds;
        private List<String> containerNames;
    }
}