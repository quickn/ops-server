package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Liuyun
 */
@Service
public class AlertContactServiceImpl extends ServiceImpl<AlertContactMapper, AlertContact> implements AlertContactService {

    @Override
    public IPage<AlertContact> page(AlertContactQueryDto dto) {
        return this.page(dto.getPage(), dto.buildLambda());
    }

    @Override
    public List<AlertContact> listAll() {
        return this.list();
    }
}
