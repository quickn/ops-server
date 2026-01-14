package com.bszn.monitor.project;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bszn.system.common.result.Result;
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
    public Result<IPage<Project>> page(ProjectQueryPage dto) {
        return Result.success(projectService.page(dto.getPage(), dto.buildLambda()));
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
}
