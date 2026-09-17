package com.cloud.receiver.util;

import com.cloud.receiver.config.CommonConfig;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class TokenUtils {

    @Resource
    private CommonConfig commonConfig;

    public boolean checkAgentToken(String agentToken) {
        if (null == agentToken) {
            return false;
        }
        String token = MD5Utils.GetMD5Code(commonConfig.getAgentToken());
        if (StringUtils.isEmpty(agentToken)) {
            return false;
        }
        if (token.equals(agentToken)) {
            return true;
        }
        return false;

    }

}
