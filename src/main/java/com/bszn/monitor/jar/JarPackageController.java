package com.bszn.monitor.jar;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public Result<JarPackage> uploadJar(@RequestParam("file") MultipartFile file, @RequestParam(value = "remark", required = false) String remark) {
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

    @PutMapping("/{id}/dockerfile")
    @Operation(summary = "更新Dockerfile")
    public Result<String> updateDockerfile(@PathVariable Integer id, @RequestBody DockerfileRequest request) {
        try {
            if (request.getDockerfileContent() == null || request.getDockerfileContent().isEmpty()) {
                return Result.failed("Dockerfile内容不能为空");
            }
            boolean success = jarPackageService.updateDockerfile(id, request.getDockerfileContent());
            if (success) {
                return Result.success("Dockerfile更新成功");
            }
            return Result.failed("Dockerfile更新失败");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            log.error("更新Dockerfile异常", e);
            return Result.failed("更新异常: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/dockerfile")
    @Operation(summary = "获取Dockerfile")
    public Result<String> getDockerfile(@PathVariable Integer id) {
        try {
            JarPackage jarPackage = jarPackageService.getJarPackageById(id);
            if (jarPackage == null) {
                return Result.failed("JAR包不存在");
            }
            return Result.success(jarPackage.getDockerfileContent());
        } catch (Exception e) {
            log.error("获取Dockerfile异常", e);
            return Result.failed("获取失败");
        }
    }

    @GetMapping("/download/{fileName}")
    @Operation(summary = "下载JAR包")
    public ResponseEntity<Resource> downloadJar(@PathVariable String fileName,
                                                HttpServletRequest request) {
        try {
            File file = jarPackageService.getJarFileForDownload(fileName);
            Resource resource = new FileSystemResource(file);
            String contentType = request.getServletContext().getMimeType(file.getAbsolutePath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("下载JAR包异常", e);
            return ResponseEntity.notFound().build();
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
//
//    @PostMapping("/deploy/{id}")
//    @Operation(summary = "首次部署JAR包（根据Dockerfile创建容器）")
//    public Result<String> deploy(@PathVariable Integer id, @RequestBody DeployRequest request) {
//        try {
//            if (request.getAgentIds() == null || request.getAgentIds().isEmpty()) {
//                return Result.failed("请选择Agent");
//            }
//            if (request.getContainerNames() == null || request.getContainerNames().isEmpty()) {
//                return Result.failed("请输入容器名称");
//            }
//            if (request.getAgentIds().size() != request.getContainerNames().size()) {
//                return Result.failed("Agent数量与容器数量必须一致");
//            }
//            // 检查容器名称是否重复
//            Set<String> containerNameSet = new HashSet<>(request.getContainerNames());
//            if (containerNameSet.size() != request.getContainerNames().size()) {
//                return Result.failed("容器名称不能重复");
//            }
//            // 检查JAR包是否存在
//            JarPackage jarPackage = jarPackageService.getJarPackageById(id);
//            if (jarPackage == null) {
//                return Result.failed("JAR包不存在");
//            }
//            // 检查Dockerfile是否已设置
//            if (StringUtils.isBlank(jarPackage.getDockerfileContent())) {
//                return Result.failed("首次部署需要先设置Dockerfile");
//            }
//            jarPackageService.deploy(id, request.getAgentIds(), request.getContainerNames());
//            return Result.success("开始部署，系统将根据Dockerfile自动创建容器");
//
//        } catch (Exception e) {
//            log.error("部署异常", e);
//            return Result.failed("部署异常: " + e.getMessage());
//        }
//    }
//
    @PostMapping("/redeploy/{id}")
    @Operation(summary = "重新部署（只替换JAR包）")
    public Result<String> redeploy(@PathVariable Integer id, @RequestBody(required = false) DeployRequest request) {
        try {
            List<Long> agentIds = null;
            List<String> containerNames = null;
            if (request != null) {
                agentIds = request.getAgentIds();
                containerNames = request.getContainerNames();

                if (agentIds != null && containerNames != null &&
                        !agentIds.isEmpty() && !containerNames.isEmpty()) {
                    if (agentIds.size() != containerNames.size()) {
                        return Result.failed("Agent数量与容器数量必须一致");
                    }
                }
            }
            jarPackageService.redeploy(id, agentIds, containerNames);
            return Result.success("开始重新部署，请稍后查看状态");
        } catch (Exception e) {
            log.error("重新部署异常", e);
            return Result.failed("重新部署异常: " + e.getMessage());
        }
    }

    @PostMapping("/deploy/{id}")
    @Operation(summary = "部署JAR包（智能判断）")
    public Result<String> deploy(@PathVariable Integer id, @RequestBody DeployRequest request) {
        try {
            if (request.getAgentIds() == null || request.getAgentIds().isEmpty()) {
                return Result.failed("请选择Agent");
            }

            if (request.getContainerNames() == null || request.getContainerNames().isEmpty()) {
                return Result.failed("请输入容器名称");
            }

            if (request.getAgentIds().size() != request.getContainerNames().size()) {
                return Result.failed("Agent数量与容器数量必须一致");
            }
            // 检查容器名称是否重复
            Set<String> containerNameSet = new HashSet<>(request.getContainerNames());
            if (containerNameSet.size() != request.getContainerNames().size()) {
                return Result.failed("容器名称不能重复");
            }
            // 检查JAR包是否存在
            JarPackage jarPackage = jarPackageService.getJarPackageById(id);
            if (jarPackage == null) {
                return Result.failed("JAR包不存在");
            }

            // 检查Dockerfile是否已设置
            if (StringUtils.isBlank(jarPackage.getDockerfileContent())) {
                return Result.failed("首次部署需要先设置Dockerfile");
            }
            // 智能判断：如果容器已存在，自动使用重新部署逻辑
            jarPackageService.deployWithAutoStrategy(id, request.getAgentIds(), request.getContainerNames());
            String message;
            if (jarPackage.getStatus() == 0) {
                message = "首次部署，将根据Dockerfile创建容器";
            } else {
                message = "容器已存在，将替换JAR包并重启";
            }
            return Result.success(message);
        } catch (Exception e) {
            log.error("部署异常", e);
            return Result.failed("部署异常: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/replace")
    @Operation(summary = "替换JAR包文件")
    public Result<String> replaceJarFile(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.failed("请选择要替换的文件");
            }
            jarPackageService.replaceJarFile(id, file);
            return Result.success("JAR包替换成功");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            log.error("替换JAR包异常", e);
            return Result.failed("替换异常: " + e.getMessage());
        }
    }

    @GetMapping("/page")
    @Operation(summary = "获取JAR包列表（分页）")
    public Result<Page<JarPackage>> page(JarQueryPage jarQueryPage) {
        try {
            return Result.success(jarPackageService.getJarPackagePage(
                    jarQueryPage.getPage(),
                    jarQueryPage.buildLambda()
            ));
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
            Map<String, Object> stats = jarPackageService.getJarPackageStats(id);
            return Result.success(stats);
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

    @Data
    public static class DockerfileRequest {
        private String dockerfileContent;
    }
}