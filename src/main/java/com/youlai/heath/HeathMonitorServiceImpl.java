package com.youlai.heath;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.base.http.RestUtil;
import com.youlai.log.LogInfoService;
import com.youlai.msg.WarnMailUtil;
import com.youlai.server.StaticKeys;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        List<HeathMonitor> heathMonitors = new ArrayList<HeathMonitor>();
        try {
            List<HeathMonitor> heathMonitorAllList = this.list();
            if (heathMonitorAllList.size() == 0) {
                return;
            }
            for (HeathMonitor h : heathMonitorAllList) {
                HeathMonitor updateTemp = new HeathMonitor();
                updateTemp.setId(h.getId());
                Long currTime = System.currentTimeMillis();
                int status = restUtil.get(h.getHeathUrl());
                updateTemp.setHeathStatus(status + "");
                heathMonitors.add(updateTemp);
                String logTitle = "接口状态异常";
                Long cha = 0L;
                if ("200".equals(updateTemp.getHeathStatus())) {
                    cha = System.currentTimeMillis() - currTime;
                    if (cha <= 3000) {
                        continue;
                    }
                    logTitle = "接口请求超时";
                }
                WarnMailUtil.sendHeathInfo(h, logTitle, true, cha);
            }
            if (heathMonitors.size() == 0) {
                return;
            }
            this.updateBatchById(heathMonitors);
        } catch (Exception e) {
            log.error("服务接口检测任务错误", e);
            logInfoService.save("服务接口检测错误", e.toString(), StaticKeys.LOG_ERROR);
        }
    }
}
