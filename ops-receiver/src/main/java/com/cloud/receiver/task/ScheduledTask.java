package com.cloud.receiver.task;

import com.cloud.receiver.entity.CpuState;
import com.cloud.receiver.entity.MemState;
import com.cloud.receiver.service.*;
import com.cloud.receiver.util.DateUtil;
import com.cloud.receiver.util.staticvar.BatchData;
import com.cloud.receiver.util.staticvar.StaticKeys;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ScheduledTask {

    /**
     * 线程池
     */
    static ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 40, 2, TimeUnit.MINUTES, new LinkedBlockingDeque<>());

    @Resource
    SystemInfoService iSystemInfoService;
    @Resource
    DiskStateService deskStateService;
    @Resource
    WarnLogInfoService logInfoService;
    @Resource
    CpuStateService cpuStateService;
    @Resource
    MemStateService memStateService;
    @Resource
    DockerContainerServiceImpl dockerContainerService;

    /**
     * 30秒后执行
     * 检测docker容器是否正常运行
     */
    @Scheduled(initialDelay = 30000L, fixedRate = 5 * 60 * 1000)
    public void checkDocker() {
        dockerContainerService.checkDocker();
    }


    /**
     * 300秒后执行
     * 检测主机是否已经下线，检测进程是否下线
     */
    @Scheduled(initialDelay = 300000L, fixedRate = 20 * 60 * 1000)
    public void hostDownCheckTask() {
        try {
            Runnable runnable = () -> {
                iSystemInfoService.checkDown();
            };
            executor.execute(runnable);
        } catch (Exception e) {
            log.error("检测主机是否下线错误", e);
        }
    }

    /**
     * 30秒后执行，之后每隔1分钟执行, 单位：ms。
     * 批量提交数据
     */
    @Scheduled(initialDelay = 5000L, fixedRate = 60 * 1000)
    public synchronized void commitTask() {
        log.info("批量提交监控数据任务开始----------" + DateUtil.getCurrentDateTime());
        try {
            if (!BatchData.CPU_STATE_LIST.isEmpty()) {
                List<CpuState> CPU_STATE_LIST = new ArrayList<CpuState>();
                CPU_STATE_LIST.addAll(BatchData.CPU_STATE_LIST);
                BatchData.CPU_STATE_LIST.clear();
                cpuStateService.saveRecord(CPU_STATE_LIST);
            }
            if (!BatchData.MEM_STATE_LIST.isEmpty()) {
                List<MemState> MEM_STATE_LIST = new ArrayList<MemState>();
                MEM_STATE_LIST.addAll(BatchData.MEM_STATE_LIST);
                BatchData.MEM_STATE_LIST.clear();
                memStateService.saveRecord(MEM_STATE_LIST);
            }
            logInfoService.insertBatch();
            deskStateService.insertBatch();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            log.error("批量提交监控数据错误----------", e);
            logInfoService.save("commitTask", "批量提交监控数据错误：" + e.toString(), StaticKeys.LOG_ERROR);
        }
        log.info("批量提交监控数据任务结束----------" + DateUtil.getCurrentDateTime());
    }

    @Resource
    IClearDataServiceImpl clearDataService;


    /**
     * 每天凌晨1:10执行
     * 删除历史数据，15天
     */
    @Scheduled(cron = "0 10 1 * * ?")
    public void clearHisdataTask() {
        clearDataService.clear();
    }

}
