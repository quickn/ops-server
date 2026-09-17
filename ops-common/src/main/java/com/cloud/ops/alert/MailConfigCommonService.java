package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cloud.ops.email.MailConfig;
import com.cloud.ops.email.MailConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class MailConfigCommonService {

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
