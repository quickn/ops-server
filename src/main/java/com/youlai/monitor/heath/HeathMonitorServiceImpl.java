package com.youlai.monitor.heath;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.base.http.RestUtil;
import com.youlai.monitor.log.LogInfoService;
import com.youlai.msg.WarnMailUtil;
import com.youlai.server.StaticKeys;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class HeathMonitorServiceImpl extends ServiceImpl<HeathMonitorMapper, HeathMonitor> implements HeathMonitorService {

    @Autowired
    private HeathMonitorMapper heathMonitorMapper;
    @Resource
    LogInfoService logInfoService;
    @Resource
    RestUtil restUtil;

    @Override
    public boolean save(HeathMonitor HeathMonitor) {
        if (StringUtils.isEmpty(HeathMonitor.getHeathUrl())) {
            HeathMonitor.setHeathUrl(HeathMonitor.getHeathUrl().trim());
        }
        heathMonitorMapper.insert(HeathMonitor);
        return true;
    }

    @Override
    public Page<HeathMonitor> queryPage(HeathQueryPage heathQueryPage) {
        Page<HeathMonitor> pageQuery = this.heathMonitorMapper.queryPage(heathQueryPage, baseMapper.getPage());
        return pageQuery;
    }

    @Override
    public void heathMonitorTask() {
        log.info("heathMonitorTask------------");
        try {
            List<HeathMonitor> heathMonitorAllList = heathMonitorMapper.selectListByMonitor();
            if (heathMonitorAllList.size() == 0) {
                return;
            }
            HeathMonitorService heathMonitorService = SpringUtil.getBean(HeathMonitorService.class);
            for (HeathMonitor heathMonitor : heathMonitorAllList) {
                heathMonitorService.handle(heathMonitor);
            }
        } catch (Exception e) {
            log.error("服务接口检测任务错误", e);
            logInfoService.save("服务接口检测错误", e.toString(), StaticKeys.LOG_ERROR);
        }
    }

    @Async
    @Override
    public void handle(HeathMonitor heathMonitor) {
        HeathMonitor updateTemp = new HeathMonitor();
        updateTemp.setId(heathMonitor.getId());
        Long currTime = System.currentTimeMillis();
        int status = restUtil.get(heathMonitor.getHeathUrl());
        updateTemp.setHeathStatus(status + "");
        String logTitle = "接口状态异常";
        Long responseTime = System.currentTimeMillis() - currTime;
        if ("200".equals(updateTemp.getHeathStatus())) {
            if (responseTime <= 3000) {
                return;
            }
            logTitle = "接口请求超时";
        }
        updateTemp.setResponseTime(responseTime);
        updateTemp.setUpdateTime(LocalDateTime.now());
        this.updateById(updateTemp);
        heathMonitor.setHeathStatus(updateTemp.getHeathStatus());
        WarnMailUtil.sendHeathInfo(heathMonitor, logTitle, true, responseTime);
    }
}
