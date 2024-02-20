package com.youlai.monitor.email;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * Created by Liuyun on 2023-09-11 14:56
 **/
public interface MailService extends IService<MailSet> {

    MailSet getByServiceId(Integer serviceId);

    public void saveNew(MailSet MailSet);

}
