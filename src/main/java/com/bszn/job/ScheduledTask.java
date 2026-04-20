package com.bszn.job;

import com.bszn.monitor.heath.ApiHeathMonitorService;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created by Liuyun on 2023-09-09 16:17
 **/
@Component
@Slf4j
public class ScheduledTask {

    @Resource
    ApiHeathMonitorService heathMonitorService;

    @XxlJob("heathMonitorTask")
    public void heathMonitorTask() {
        heathMonitorService.heathMonitorTask();
    }

}
