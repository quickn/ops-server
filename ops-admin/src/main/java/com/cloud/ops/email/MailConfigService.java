package com.cloud.ops.email;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * Created by Liuyun on 2023-09-11 14:56
 **/
public interface MailConfigService extends IService<MailConfig> {
    void saveNew(MailConfig MailSet);
}
