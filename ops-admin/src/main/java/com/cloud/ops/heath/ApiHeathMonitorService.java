package com.cloud.ops.heath;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * Created by Liuyun on 2023-09-09 11:18
 **/
public interface ApiHeathMonitorService extends IService<ApiHeathMonitor> {

    Page<ApiHeathMonitor> queryPage(ApiHeathQueryPage heathQueryPage);

    void heathMonitorTask();

    ApiHeathMonitor handle(ApiHeathMonitor heathMonitor);
}
