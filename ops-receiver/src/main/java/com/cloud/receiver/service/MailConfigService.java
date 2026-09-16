package com.cloud.receiver.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cloud.receiver.entity.MailConfig;
import com.cloud.receiver.mapper.MailConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class MailConfigService {

    @Resource
    private MailConfigMapper mailSetMapper;

    public MailConfig getByServiceId(Integer serviceId) {
        MailConfig mailSet = this.mailSetMapper.selectOne(Wrappers.<MailConfig>lambdaQuery().eq(MailConfig::getServiceId, serviceId));
        if (mailSet == null) {
            mailSet = this.mailSetMapper.selectOne(Wrappers.<MailConfig>lambdaQuery().isNull(MailConfig::getServiceId));
        }
        if (mailSet != null) {
            mailSet.setServiceId(serviceId);
        }
        return mailSet;
    }

}
