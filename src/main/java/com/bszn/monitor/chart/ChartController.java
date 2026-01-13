package com.bszn.monitor.chart;

import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/cpu")
    @Operation(summary = "cpu监控")
    public Result<List<ChartVO>> cpuList(ChartDto chartDto) {
        return Result.success(chartService.cpuList(chartDto));
    }

    @GetMapping("/mem")
    @Operation(summary = "内存监控")
    public Result<List<ChartVO>> memList(ChartDto chartDto) {
        return Result.success(chartService.memList(chartDto));
    }


}
