package com.cloud.receiver.service;

import com.cloud.receiver.mapper.*;
import com.cloud.receiver.util.DateUtil;
import com.cloud.receiver.util.msg.WarnPools;
import com.cloud.receiver.util.staticvar.StaticKeys;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Created by Liuyun on 2023-07-31 15:06
 **/
@Slf4j
@Service
public class IClearDataServiceImpl  {

    @Resource
    CpuStateMapper cpuStateMapper;
    @Resource
    MemStateMapper memStateMapper;
    @Resource
    SysLoadStateMapper sysLoadStateMapper;
    @Resource
    WarnLogInfoMapper logInfoMapper;
    @Resource
    DockerStatsMapper dockerStatsMapper;
    @Resource
    ProcessStatMapper processNetStatMapper;
    @Resource
    WarnLogInfoService logInfoService;

    public void clear() {
        log.info("定时清空历史数据任务开始----------" + DateUtil.getCurrentDateTime());
        WarnPools.clearOldData();//清空发告警邮件的记录
        String nowTime = DateUtil.getCurrentDateTime();
        //15天前时间
        String thrityDayBefore = DateUtil.getDateBefore(nowTime, 15);
        try {
            //执行删除操作begin
            dockerStatsMapper.deleteByDate(thrityDayBefore);
            cpuStateMapper.deleteByDate(thrityDayBefore);//删除cpu监控信息
            // deskStateMapper.deleteByDate(thrityDayBefore);//删除磁盘监控信息
            memStateMapper.deleteByDate(thrityDayBefore);//删除内存监控信息
            sysLoadStateMapper.deleteByDate(thrityDayBefore);//删除负载状态监控信息
            processNetStatMapper.deleteByDate(thrityDayBefore);//删除进程网络流量监控信息
            //删除15天前的日志信息
            logInfoMapper.deleteByDate(thrityDayBefore);
            //删除15天前数据库表统计信息
            //  dbTableCountService.deleteByDate(paramsDel);
            logInfoService.save("定时清空历史数据完成", "定时清空历史数据完成：", StaticKeys.LOG_ERROR);
            //执行删除操作end
        } catch (Exception e) {
            log.error("定时清空历史数据任务出错：", e);
            logInfoService.save("定时清空历史数据错误", "定时清空历史数据错误：" + e.toString(), StaticKeys.LOG_ERROR);
        }
        log.info("定时清空历史数据任务结束----------" + DateUtil.getCurrentDateTime());
    }
}
