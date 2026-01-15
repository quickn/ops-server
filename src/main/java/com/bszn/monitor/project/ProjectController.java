package com.bszn.monitor.project;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bszn.system.common.result.Result;
import com.bszn.system.common.util.SecurityUtils;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/deploy/{projectId}")
    @Operation(summary = "部署")
    public Result<String> deploy(@ApiParam(value = "项目id", required = true) @PathVariable Long projectId,
                                 @RequestBody @Validated DeployRequest request) {
        projectService.deploy(projectId, request.getAgentIds(), SecurityUtils.getUserId());
        return Result.success("开始部署，查看日志关注部署状态！");
    }

    @PostMapping("/redeploy/{projectId}")
    @Operation(summary = "重新部署（只替换JAR包）")
    public Result<String> redeploy(@ApiParam(value = "项目id", required = true) @PathVariable Long projectId,
                                   @RequestBody DeployRequest request) {
        projectService.redeploy(projectId, request.getAgentIds(), SecurityUtils.getUserId());
        return Result.success("重新部署，查看日志关注部署状态！");
    }

}
