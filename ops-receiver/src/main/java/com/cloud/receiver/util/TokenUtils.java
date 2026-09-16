package com.cloud.receiver.util;

import com.alibaba.fastjson2.JSONObject;
import com.cloud.receiver.config.CommonConfig;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TokenUtils {
    private static final Logger logger = LoggerFactory.getLogger(TokenUtils.class);

    @Resource
    private CommonConfig commonConfig;

    /**
     * 验证agent的token和server的token是否一致
     *
     * @param agentJsonObject
     * @return
     */
    public boolean checkAgentToken(JSONObject agentJsonObject) {
        if (null == agentJsonObject) {
            return false;
        }
        String token = MD5Utils.GetMD5Code(commonConfig.getAgentToken());
        String agentToken = agentJsonObject.getString("agentToken");
        if (StringUtils.isEmpty(agentToken)) {
            return false;
        }
        if (token.equals(agentToken)) {
            return true;
        }
        return false;
    }

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
