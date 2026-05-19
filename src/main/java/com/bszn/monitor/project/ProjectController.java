package com.bszn.monitor.project;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bszn.system.common.result.Result;
import com.bszn.system.common.util.SecurityUtils;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author wzh
 * @date 2026/1/14 13:43
 * @description: 项目控制器
 */
@Slf4j
@RestController
@RequestMapping("/project")
@Tag(name = "项目管理")
@RequiredArgsConstructor
public class ProjectController {

    private final IProjectService projectService;

    @GetMapping("/page")
    @Operation(summary = "获取项目列表（分页）")
    public Result<IPage<Project>> page(ProjectDto dto) {
        return Result.success(projectService.page(dto.getPage(), dto.buildLambda()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "项目详情")
    public Result<Project> get(@ApiParam("项目id") @PathVariable Long id) {
        return Result.success(projectService.getById(id));
    }

    @PostMapping
    @Operation(summary = "保存")
    public Result<Project> save(@RequestBody @Validated ProjectParam projectParam) {
        Project entity = projectParam.toEntity();
        projectService.save(entity);
        return Result.success(entity);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改")
    public Result<Boolean> update(@ApiParam("项目id") @PathVariable Long id,
                                  @RequestBody @Validated ProjectParam projectParam) {
        Project entity = projectParam.toEntity();
        entity.setId(id);
        return Result.success(projectService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除")
    public Result<Boolean> remove(@ApiParam("项目id") @PathVariable Long id) {
        return Result.success(projectService.removeById(id));
    }

    @PostMapping("/deploy/{id}")
    @Operation(summary = "部署")
    @McpTool(name = "projectDeploy", description = "部署项目")
    public Result<String> deploy(@ApiParam(value = "项目id", required = true) @PathVariable Long id,
                                 @RequestBody @Validated DeployRequest request) {
        if (request.getDeployType() == 2) {
            projectService.redeploy(id, request.getAgentIds(), SecurityUtils.getUserId());
        } else if (request.getDeployType() == 1) {
            projectService.deploy(id, request.getAgentIds(), SecurityUtils.getUserId());
        } else {
            return Result.failed("无效的部署方式");
        }
        return Result.success("开始部署，查看日志关注部署状态！");
    }

    @PostMapping("/redeploy/{id}")
    @Operation(summary = "重新部署（只替换JAR包）")
    public Result<String> redeploy(@ApiParam(value = "项目id", required = true) @PathVariable Long id,
                                   @RequestBody DeployRequest request) {
        projectService.redeploy(id, request.getAgentIds(), SecurityUtils.getUserId());
        return Result.success("开始部署，查看日志关注部署状态！");
    }


    @GetMapping("/deploy-records/{id}")
    @Operation(summary = "获取部署记录")
    public Result<List<ProjectDeployRecord>> getDeployRecords(@PathVariable Long id) {
        try {
            List<ProjectDeployRecord> records = projectService.getDeployRecords(id);
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取部署记录异常", e);
            return Result.failed("获取部署记录失败");
        }
    }

    @PostMapping("/sync/{id}")
    @Operation(summary = "同步")
    public Result<Boolean> sync(@ApiParam(value = "项目id", required = true) @PathVariable Long id,
                                @RequestBody @Validated SyncRequest syncRequest) {
        return Result.success(projectService.sync(id, SecurityUtils.getUserId(), syncRequest));
    }

    @PostMapping("/backup")
    @Operation(summary = "备份/恢复")
    public Result<Boolean> backup(@RequestBody @Validated BackupRequest backupRequest) {
        return Result.success(projectService.backup(SecurityUtils.getUserId(), backupRequest));
    }

}
