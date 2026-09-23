package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 报警联系人 Service
 *
 * @author Liuyun
 */
public interface AlertContactService extends IService<AlertContact> {

    IPage<AlertContact> page(AlertContactQueryDto dto);

    List<AlertContact> listAll();
}
