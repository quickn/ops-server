package com.youlai.task;

import com.youlai.base.http.RestUtil;
import com.youlai.heath.HeathMonitor;
import com.youlai.heath.HeathMonitorService;
import com.youlai.log.LogInfo;
import com.youlai.log.LogInfoService;
import com.youlai.msg.WarnMailUtil;
import com.youlai.msg.WarnPools;
import com.youlai.server.StaticKeys;
import com.youlai.system.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liuyun on 2023-09-09 16:17
 **/
@Configuration //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling // 2.开启定时任务
@Slf4j
public class ScheduledTask {

    @Resource
    HeathMonitorService heathMonitorService;
    @Resource
    LogInfoService logInfoService;
    @Resource
    RestUtil restUtil;

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

    @Scheduled(initialDelay = 1000L, fixedRateString = "${base.heathTimes}")
    public void heathMonitorTask() {
        log.info("heathMonitorTask------------");
        List<HeathMonitor> heathMonitors = new ArrayList<HeathMonitor>();
        List<LogInfo> logInfoList = new ArrayList<LogInfo>();
        try {
            List<HeathMonitor> heathMonitorAllList = heathMonitorService.list();
            if (heathMonitorAllList.size() == 0) {
                return;
            }
            for (HeathMonitor h : heathMonitorAllList) {
                h.setCreateTime(null);
                h.setUpdateTime(null);
                int status = restUtil.get(h.getHeathUrl());
                h.setHeathStatus(status + "");
                heathMonitors.add(h);
                if (!"200".equals(h.getHeathStatus())) {
//                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(h.getId()))) {
//                            continue;
//                        }
                    LogInfo logInfo = new LogInfo();
                    logInfo.setHostname("服务接口检测异常：" + h.getAppName());
                    logInfo.setInfoContent("服务接口检测异常：" + h.getAppName() + "，" + h.getHeathUrl() + "，返回状态" + h.getHeathStatus());
                    logInfo.setState(StaticKeys.LOG_ERROR);
                    logInfoList.add(logInfo);
                    WarnMailUtil.sendHeathInfo(h, true);
                } else {
                    if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(h.getId()))) {
                        WarnMailUtil.sendHeathInfo(h, false);
                    }
                }
            }
            heathMonitorService.updateBatchById(heathMonitors);
            if (logInfoList.size() > 0) {
                logInfoService.saveBatch(logInfoList);
            }
        } catch (Exception e) {
            log.error("服务接口检测任务错误", e);
            logInfoService.save("服务接口检测错误", e.toString(), StaticKeys.LOG_ERROR);
        }
    }

}
