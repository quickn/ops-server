package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloud.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 预警规则管理
 *
 * @author Liuyun
 */
@Tag(name = "预警规则")
@Slf4j
@RestController
@RequestMapping("/alertRule")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @GetMapping("/page")
    @Operation(summary = "预警规则列表（分页）")
    public Result<IPage<AlertRule>> page(AlertRuleQueryDto dto) {
        return Result.success(alertRuleService.page(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "预警规则详情")
    public Result<AlertRule> get(@PathVariable Long id) {
        return Result.success(alertRuleService.getById(id));
    }

    @Operation(summary = "保存预警规则")
    @PostMapping("/save")
    public Result<Boolean> save(@RequestBody AlertRule alertRule) {
        return Result.success(alertRuleService.saveOrUpdate(alertRule));
    }

    @Operation(summary = "删除预警规则")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        alertRuleService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "触发预警")
    @PostMapping("/trigger/{id}")
    public Result<Boolean> trigger(@PathVariable Long id, @RequestParam String alertContent) {
        alertRuleService.triggerAlert(id, alertContent);
        return Result.success();
    }

    @Operation(summary = "手动触发预警（跳过连续阈值）")
    @PostMapping("/manualTrigger/{id}")
    public Result<Boolean> manualTrigger(@PathVariable Long id, @RequestParam String alertContent) {
        alertRuleService.manualTrigger(id, alertContent);
        return Result.success();
    }
}
