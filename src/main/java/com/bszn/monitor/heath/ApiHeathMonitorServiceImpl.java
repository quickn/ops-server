package com.bszn.monitor.heath;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bszn.base.http.RestUtil;
import com.bszn.monitor.log.LogInfoService;
import com.bszn.msg.WarnMailUtil;
import com.bszn.server.StaticKeys;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class ApiHeathMonitorServiceImpl extends ServiceImpl<ApiHeathMonitorMapper, ApiHeathMonitor> implements ApiHeathMonitorService {

    @Autowired
    private ApiHeathMonitorMapper heathMonitorMapper;
    @Resource
    LogInfoService logInfoService;
    @Resource
    RestUtil restUtil;

    /**
     * 累计失败次数
     */
    HashMap<Long, Integer> failCount = new HashMap();

    @Override
    public boolean save(ApiHeathMonitor HeathMonitor) {
        if (StringUtils.isEmpty(HeathMonitor.getApiUrl())) {
            HeathMonitor.setApiUrl(HeathMonitor.getApiUrl().trim());
        }
        heathMonitorMapper.insert(HeathMonitor);
        return true;
    }

    @Override
    public Page<ApiHeathMonitor> queryPage(ApiHeathQueryPage heathQueryPage) {
        Page<ApiHeathMonitor> pageQuery = this.heathMonitorMapper.queryPage(heathQueryPage, baseMapper.getPage());
        return pageQuery;
    }

    @Override
    public void heathMonitorTask() {
        log.info("heathMonitorTask------------");
        try {
            List<ApiHeathMonitor> heathMonitorAllList = heathMonitorMapper.selectListByMonitor();
            if (heathMonitorAllList.size() == 0) {
                return;
            }
            ApiHeathMonitorService heathMonitorService = SpringUtil.getBean(ApiHeathMonitorService.class);
            for (ApiHeathMonitor heathMonitor : heathMonitorAllList) {
                heathMonitorService.handle(heathMonitor);
            }
        } catch (Exception e) {
            log.error("服务接口检测任务错误", e);
            logInfoService.save("服务接口检测错误", e.toString(), StaticKeys.LOG_ERROR);
        }
    }

    @Async
    @Override
    public void handle(ApiHeathMonitor heathMonitor) {
        ApiHeathMonitor updateTemp = new ApiHeathMonitor();
        updateTemp.setId(heathMonitor.getId());
        Long currTime = System.currentTimeMillis();
        int status = restUtil.get(heathMonitor.getApiUrl());
        updateTemp.setHeathStatus(status);
        String logTitle = "接口状态异常";
        Long responseTime = System.currentTimeMillis() - currTime;
        if ("200".equals(updateTemp.getHeathStatus())) {
            if (responseTime <= 3000) {
                failCount.put(heathMonitor.getId(), 0);
                return;
            }
            logTitle = "接口请求超时";
        }
        Integer count = failCount.get(heathMonitor.getId());
        if (count == null) {
            count = 0;
        }
        count = ++count;
        failCount.put(heathMonitor.getId(), count);
        updateTemp.setResponseTime(responseTime);
        updateTemp.setUpdateTime(LocalDateTime.now());
        this.updateById(updateTemp);
        heathMonitor.setHeathStatus(updateTemp.getHeathStatus());
        boolean isEmail = false;
        if (count >= 2) {
            isEmail = true;
        }
        WarnMailUtil.sendHeathInfo(heathMonitor, logTitle, isEmail, responseTime);
    }
}
