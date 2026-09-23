package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 预警记录 Service 实现
 *
 * @author Liuyun
 */
@Service
@Slf4j
public class AlertRecordServiceImpl extends ServiceImpl<AlertRecordMapper, AlertRecord> implements AlertRecordService {

    @Override
    public IPage<AlertRecord> page(AlertRecordQueryDto dto) {
        return this.page(dto.getPage(), dto.buildLambda());
    }
}
