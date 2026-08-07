package com.bszn.ops.jar;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
                                        @RequestParam("serviceId") Integer serviceId,
                                        @RequestParam("serviceName") String serviceName,
                                        @RequestParam(value = "remark", required = false) String remark) {
        try {
            if (file.isEmpty()) {
                return Result.failed("请选择要上传的文件");
            }
            JarPackage jarPackage = jarPackageService.uploadJar(file, serviceId, serviceName, remark);
            return Result.success(jarPackage);
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            log.error("上传JAR包异常", e);
            return Result.failed("上传异常: " + e.getMessage());
        }
    }

    @GetMapping("/versions")
    @Operation(summary = "获取文件的所有版本")
    public Result<List<JarPackage>> getAllVersions(@RequestParam String fileName) {
        try {
            List<JarPackage> versions = jarPackageService.getAllVersionsByFileName(fileName);
            return Result.success(versions);
        } catch (Exception e) {
            log.error("获取版本列表异常", e);
            return Result.failed("获取版本列表失败");
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
    public Result<IPage<JarPackage>> page(JarQueryPage jarQueryPage) {
        try {
            return Result.success(jarPackageService.getJarPackagePage(jarQueryPage.getPage(),
                    jarQueryPage.build()
            ));
        } catch (Exception e) {
            log.error("获取JAR包列表异常", e);
            return Result.failed("获取列表失败");
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
}