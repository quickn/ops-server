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

    /**
     * 根据条件查询心跳监控列表
     *
     * @return
     */
    @GetMapping(value = "/listHeaths")
    public Result<Page<ApiHeathMonitor>> listHeaths(@ParameterObject ApiHeathQueryPage heathQueryPage) {
        Page<ApiHeathMonitor> pageInfo = heathMonitorService.queryPage(heathQueryPage);
        return Result.success(pageInfo);
    }

    /**
     * 保存心跳监控信息
     *
     * @return
     */
    @PostMapping(value = "/save")
    public Result saveHeathMonitor(@RequestBody ApiHeathMonitor heathMonitor) {
        heathMonitorService.saveOrUpdate(heathMonitor);
        return Result.success();
    }


    /**
     * 删除心跳监控
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/delete/{ids}")
    @ResponseBody
    public Result delete(@PathVariable Integer ids) {
        heathMonitorService.removeById(ids);
        return Result.success();
    }

    @GetMapping(value = "/heathMonitorTask")
    @Operation(summary = "执行监控")
    public void heathMonitorTask() {
        heathMonitorService.heathMonitorTask();
    }
}
