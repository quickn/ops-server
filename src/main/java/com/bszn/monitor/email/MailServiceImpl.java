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
public class MailServiceImpl extends ServiceImpl<MailSetMapper, MailSet> implements MailService {

    @Override
    public MailSet getByServiceId(Integer serviceId) {
        MailSet mailSet = this.baseMapper.selectOne(Wrappers.<MailSet>lambdaQuery().eq(MailSet::getServiceId, serviceId));
        if (mailSet == null) {
            mailSet = this.baseMapper.selectOne(Wrappers.<MailSet>lambdaQuery().isNull(MailSet::getServiceId));
        }
        if (mailSet != null) {
            mailSet.setServiceId(serviceId);
        }
        return mailSet;
    }

    @Override
    public void saveNew(MailSet MailSet) {
        MailSet.setFromMailName(MailSet.getFromMailName().trim());
        MailSet.setFromPwd(MailSet.getFromPwd().trim());
        MailSet.setToMail(MailSet.getToMail().trim());
        MailSet.setSmtpHost(MailSet.getSmtpHost().trim());
        this.save(MailSet);
    }
}
