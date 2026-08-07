package com.bszn.monitor.warnLog;

import com.bszn.ops.cmd.LogCmdForm;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "监控日志")
@RestController
@RequestMapping(value = "/log")
@Slf4j
public class LogController {
    @Resource
    ILogService iLogService;

    @GetMapping("/realTime")
    @Operation(summary = "实时日志查询")
    public Result realtime(@ParameterObject LogCmdForm logCmdForm) {
        String log = iLogService.getLogsByServiceId(logCmdForm);
        return Result.success(log);
    }

    @ResponseBody
    @GetMapping("/analysis")
    @Operation(summary = "日志分析")
    public Result analysis(@ParameterObject LogCmdForm logCmdForm) {
        return Result.success(iLogService.analysis(logCmdForm));
    }

    @ResponseBody
    @GetMapping("/detail")
    @Operation(summary = "日志详情")
    public Result detail(@ParameterObject LogCmdForm logCmdForm) {
        return Result.success(iLogService.detail(logCmdForm));
    }
}
