package com.bszn.monitor.email;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Created by Liuyun on 2023-09-11 14:56
 **/
@Service
@Slf4j
public class MailConfigServiceImpl extends ServiceImpl<MailConfigMapper, MailConfig> implements MailConfigService {

    @Override
    public MailConfig getByServiceId(Integer serviceId) {
        MailConfig mailSet = this.baseMapper.selectOne(Wrappers.<MailConfig>lambdaQuery().eq(MailConfig::getServiceId, serviceId));
        if (mailSet == null) {
            mailSet = this.baseMapper.selectOne(Wrappers.<MailConfig>lambdaQuery().isNull(MailConfig::getServiceId));
        }
        if (mailSet != null) {
            mailSet.setServiceId(serviceId);
        }
        return mailSet;
    }

    @Override
    public void saveNew(MailConfig MailSet) {
        MailSet.setFromMailName(MailSet.getFromMailName().trim());
        MailSet.setFromPwd(MailSet.getFromPwd().trim());
        MailSet.setToMail(MailSet.getToMail().trim());
        MailSet.setSmtpHost(MailSet.getSmtpHost().trim());
        this.save(MailSet);
    }
}
