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

    public MailConfig getByUserId(Long createBy) {
        return this.mailSetMapper.selectOne(Wrappers.<MailConfig>lambdaQuery().eq(MailConfig::getCreateBy, createBy));
    }

}
