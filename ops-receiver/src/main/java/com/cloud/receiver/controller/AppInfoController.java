package com.cloud.receiver.controller;

import com.alibaba.fastjson2.JSONObject;
import com.cloud.receiver.util.TokenUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
@RequestMapping("/appInfo")
public class AppInfoController {

    @Resource
    private TokenUtils tokenUtils;

    /**
     * agent查询进程列表
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "agentList")
    public String agentList(@RequestBody String paramBean, @RequestHeader(value = "agentToken", required = false) String agentToken) {
        JSONObject agentJsonObject = JSONObject.parseObject(paramBean);
        if (StringUtils.isEmpty(agentToken)) {
            agentToken = agentJsonObject.getString("agentToken");
        }
        if (!tokenUtils.checkAgentToken(agentToken)) {
            log.error("token is invalidate");
            return "error：token is invalidate";
        }
        Map<String, Object> params = new HashMap<String, Object>();
        if (null == agentJsonObject.get("hostname") || StringUtils.isEmpty(agentJsonObject.get("hostname").toString())) {
            return "";
        }
        params.put("hostname", agentJsonObject.get("hostname").toString());
        return "";
    }

}
