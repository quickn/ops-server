package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 预警记录 Service
 *
 * @author Liuyun
 */
public interface AlertRecordService extends IService<AlertRecord> {

    /**
     * 分页查询预警记录
     */
    IPage<AlertRecord> page(AlertRecordQueryDto dto);
}
