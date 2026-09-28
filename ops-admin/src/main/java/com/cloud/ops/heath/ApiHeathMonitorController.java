package com.cloud.ops.heath;

import com.cloud.ops.taskAlert.ApiHeathCheckTaskHandler;
import com.cloud.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "接口健康监控")
@RestController
@RequestMapping("/apiHeathMonitor")
public class ApiHeathMonitorController {

    @Resource
    private ApiHeathCheckTaskHandler apiHeathCheckTaskHandler;

    @Operation(summary = "测试接口")
    @PostMapping(value = "/testApi")
    public Result<ApiHeathMonitor> testApi(@RequestBody ApiHeathMonitor apiHeathMonitor) {
        apiHeathMonitor.setId(null);
        ApiHeathMonitor temp = apiHeathCheckTaskHandler.check(apiHeathMonitor);
        return Result.success(temp);
    }

}
