package com.bszn.task;

import com.bszn.monitor.heath.HeathMonitorService;
import com.bszn.system.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Created by Liuyun on 2023-09-09 16:17
 **/
@Configuration //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling // 2.开启定时任务
@Slf4j
public class ScheduledTask {

    @Resource
    HeathMonitorService heathMonitorService;


    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setThreadNamePrefix("@admin-scheduler-");
        taskScheduler.setPoolSize(6);
        //线程池拒绝策略
        taskScheduler.setRejectedExecutionHandler((r, e) -> {
            log.error("定时任务拒绝策略异常", r);
        });
        //对异常进行统一处理
        taskScheduler.setErrorHandler(throwable -> {
            if (!(throwable instanceof BusinessException)) {
                log.error("定时任务异常", throwable);
            } else {
                log.debug("定时任务异常", throwable);
            }
        });
        taskScheduler.initialize();
        return taskScheduler;
    }

    @Scheduled(initialDelay = 5000L, fixedRateString = "${base.heathTimes}")
    public void heathMonitorTask() {
        heathMonitorService.heathMonitorTask();
    }

}
