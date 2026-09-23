package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 报警联系人组 Service
 *
 * @author Liuyun
 */
public interface AlertContactGroupService extends IService<AlertContactGroup> {

    IPage<AlertContactGroup> page(AlertContactGroupQueryDto dto);

    List<AlertContactGroup> listAll();
}
