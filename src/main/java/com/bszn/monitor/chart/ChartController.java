package com.bszn.monitor.chart;

import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author wzh
 * @date 2026/1/13 13:13
 * @description:
 */
@Slf4j
@RestController
@RequestMapping("/chart")
@Tag(name = "图表管理")
@RequiredArgsConstructor
public class ChartController {

    private final ChartService chartService;

    @PostMapping("/cpu")
    @Operation(summary = "cpu监控")
    public Result<List<ChartIpVO>> cpuList(@RequestBody @Validated ChartDto chartDto) {
        return Result.success(chartService.cpuList(chartDto));
    }

    @PostMapping("/mem")
    @Operation(summary = "内存监控")
    public Result<List<ChartIpVO>> memList(@RequestBody @Validated ChartDto chartDto) {
        return Result.success(chartService.memList(chartDto));
    }


}
