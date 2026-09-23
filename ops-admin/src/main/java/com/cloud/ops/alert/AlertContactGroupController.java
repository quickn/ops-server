package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloud.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报警联系人组管理
 *
 * @author Liuyun
 */
@Tag(name = "报警联系人组")
@Slf4j
@RestController
@RequestMapping("/alertContactGroup")
@RequiredArgsConstructor
public class AlertContactGroupController {

    private final AlertContactGroupService alertContactGroupService;

    @GetMapping("/page")
    @Operation(summary = "联系人组列表（分页）")
    public Result<IPage<AlertContactGroup>> page(AlertContactGroupQueryDto dto) {
        return Result.success(alertContactGroupService.page(dto));
    }

    @GetMapping("/list")
    @Operation(summary = "联系人组列表（全部）")
    public Result<List<AlertContactGroup>> list() {
        return Result.success(alertContactGroupService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "联系人组详情")
    public Result<AlertContactGroup> get(@PathVariable Long id) {
        return Result.success(alertContactGroupService.getById(id));
    }

    @Operation(summary = "保存联系人组")
    @PostMapping("/save")
    public Result<Boolean> save(@RequestBody AlertContactGroup alertContactGroup) {
        return Result.success(alertContactGroupService.saveOrUpdate(alertContactGroup));
    }

    @Operation(summary = "删除联系人组")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        alertContactGroupService.removeById(id);
        return Result.success();
    }
}
