package com.cloud.receiver.controller;

import com.cloud.receiver.service.IClearDataServiceImpl;
import com.cloud.receiver.service.SystemInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "接收器的定时任务")
@Slf4j
@RequestMapping("/receiver/schedule")
public class ScheduledController {

    @Resource
    SystemInfoService iSystemInfoService;
    @Resource
    IClearDataServiceImpl iClearDataService;

    @GetMapping("/checkDown")
    @ResponseBody
    @Operation(summary = "检查服务是否掉线")
    public String checkDown() {
        iSystemInfoService.checkDown();
        return null;
    }

    @GetMapping("/cleanData")
    @ResponseBody
    @Operation(summary = "清理数据")
    public String cleanData() {
        iClearDataService.clear();
        return "";
    }
}
