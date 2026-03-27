package com.bszn.monitor.heath;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;


@Tag(name = "接口健康监控")
@RestController
@RequestMapping("/apiHeathMonitor")
public class ApiHeathMonitorController {

    @Resource
    private ApiHeathMonitorService heathMonitorService;

    @Operation(summary = "心跳监控列表（分页）")
    @GetMapping(value = "/listHeaths")
    public Result<Page<ApiHeathMonitor>> listHeaths(@ParameterObject ApiHeathQueryPage heathQueryPage) {
        return Result.success(heathMonitorService.page(heathQueryPage.getPage(), heathQueryPage.buildLambda()));
    }

    @Operation(summary = "保存心跳监控信息")
    @PostMapping(value = "/save")
    public Result<Boolean> saveHeathMonitor(@RequestBody ApiHeathMonitor heathMonitor) {
        return Result.success(heathMonitorService.saveOrUpdate(heathMonitor));
    }

    @Operation(summary = "删除心跳监控")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        heathMonitorService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "执行监控")
    @GetMapping(value = "/heathMonitorTask")
    public void heathMonitorTask() {
        heathMonitorService.heathMonitorTask();
    }

    @Operation(summary = "测试接口")
    @PostMapping(value = "/testApi")
    public Result<ApiHeathMonitor> testApi(@RequestBody ApiHeathMonitor apiHeathMonitor) {
        apiHeathMonitor.setId(null);
        ApiHeathMonitor temp = heathMonitorService.handle(apiHeathMonitor);
        return Result.success(temp);
    }

}
