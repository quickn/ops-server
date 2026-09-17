package com.cloud.ops.heath;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.base.http.RestUtil;
import com.cloud.receiver.service.RWarnLogInfoService;
import com.cloud.ops.server.StaticKeys;
import com.cloud.receiver.util.msg.WarnMailUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class ApiHeathMonitorServiceImpl extends ServiceImpl<ApiHeathMonitorMapper, ApiHeathMonitor> implements ApiHeathMonitorService {

    @Autowired
    private ApiHeathMonitorMapper heathMonitorMapper;
    @Resource
    RWarnLogInfoService logInfoService;
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
            if (heathMonitorAllList.isEmpty()) {
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

    @Override
    public ApiHeathMonitor handle(ApiHeathMonitor heathMonitor) {
        ApiHeathMonitor updateTemp = new ApiHeathMonitor();
        updateTemp.setId(heathMonitor.getId());
        Long currTime = System.currentTimeMillis();
        int status;
        ResponseEntity response = null;
        try {
            if ("post".equals(heathMonitor.getRequestMethod())) {
                response = restUtil.post(heathMonitor.getApiUrl(), heathMonitor.getContentType(), heathMonitor.getParam());
                status = response.getStatusCode().value();
                updateTemp.setBody(response.getBody().toString());
            } else {
                status = restUtil.get(heathMonitor.getApiUrl());
            }
        } catch (Exception e) {
            status = 500;
        }
        updateTemp.setHeathStatus(status);
        String logTitle = "接口状态异常";
        Long responseTime = (System.currentTimeMillis() - currTime) / 1000;
        if (status == 200) {
            if (heathMonitor.getTimeoutWarnTime() == null) {
                heathMonitor.setTimeoutWarnTime(3);
            }
            if (responseTime <= heathMonitor.getTimeoutWarnTime()) {
                failCount.put(heathMonitor.getId(), 0);
                return updateTemp;
            }
            logTitle = "接口请求超时";
        }
        updateTemp.setResponseTime(responseTime);
        if (updateTemp.getId() == null) {
            return updateTemp;
        }
        this.updateById(updateTemp);
        heathMonitor.setHeathStatus(updateTemp.getHeathStatus());
        Integer count = failCount.get(heathMonitor.getId());
        if (count == null) {
            count = 0;
        }
        ++count;
        failCount.put(heathMonitor.getId(), count);
        boolean isEmail = false;
        if (count >= 2) {
            isEmail = true;
            failCount.put(heathMonitor.getId(), 0);
        }
        WarnMailUtil.sendHeathInfo(heathMonitor, logTitle, isEmail, responseTime);
        return updateTemp;
    }
}
