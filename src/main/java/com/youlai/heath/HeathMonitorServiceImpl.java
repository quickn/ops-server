package com.youlai.heath;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HeathMonitorServiceImpl extends ServiceImpl<HeathMonitorMapper, HeathMonitor> implements HeathMonitorService {

    @Autowired
    private HeathMonitorMapper heathMonitorMapper;

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
}
