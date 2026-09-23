package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Liuyun
 */
@Service
public class AlertContactGroupServiceImpl extends ServiceImpl<AlertContactGroupMapper, AlertContactGroup> implements AlertContactGroupService {

    @Override
    public IPage<AlertContactGroup> page(AlertContactGroupQueryDto dto) {
        return this.page(dto.getPage(), dto.buildLambda());
    }

    @Override
    public List<AlertContactGroup> listAll() {
        return this.list();
    }
}
