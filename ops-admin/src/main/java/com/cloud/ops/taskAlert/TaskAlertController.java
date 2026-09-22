package com.cloud.ops.taskAlert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloud.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 任务告警
 *
 * @author Liuyun
 */
@Tag(name = "任务告警")
@Slf4j
@RestController
@RequestMapping("/taskAlert")
@RequiredArgsConstructor
public class TaskAlertController {

    private final TaskAlertService taskAlertService;

    @GetMapping("/page")
    @Operation(summary = "任务告警列表（分页）")
    public Result<IPage<TaskAlert>> page(TaskAlertQueryDto dto) {
        return Result.success(taskAlertService.page(dto.getPage(), dto.buildLambda()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "任务告警详情")
    public Result<TaskAlert> get(@PathVariable Long id) {
        return Result.success(taskAlertService.getById(id));
    }

    @Operation(summary = "保存任务告警")
    @PostMapping("/save")
    public Result<Boolean> save(@RequestBody TaskAlert taskAlert) {
        return Result.success(taskAlertService.saveOrUpdate(taskAlert));
    }

    @Operation(summary = "删除任务告警")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        taskAlertService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "立即执行任务")
    @PostMapping("/execute/{id}")
    public Result<Boolean> execute(@PathVariable Long id) {
        taskAlertService.execute(id);
        return Result.success();
    }

    @Operation(summary = "手动触发任务告警扫描")
    @GetMapping("/taskAlertCheck")
    public Result<Boolean> taskAlertCheck() {
        taskAlertService.taskAlertCheck();
        return Result.success();
    }

}
