package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.ops.agent.Agent;
import com.cloud.ops.cmdlog.CmdLogInfo;
import com.cloud.ops.cmdlog.CmdLogInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RCmdLogInfoServiceImpl extends ServiceImpl<CmdLogInfoMapper, CmdLogInfo> {

    public void saveAgentJsonObject(JSONObject agentJsonObject, Agent agentConfig) {
        String script = agentJsonObject.getString("cmdLogInfoCmd");
        String command = agentJsonObject.getString("command");
        if (StringUtils.isEmpty(script)) {
            return;
        }
        String cmdLogInfoResult = agentJsonObject.getString("cmdLogInfoResult");
        CmdLogInfo cmdLogInfo = CmdLogInfo.builder()
                .serviceId(agentConfig.getServiceId())
                .serviceName(agentConfig.getServiceName())
                .agentId(agentConfig.getId())
                .agentIp(agentConfig.getHostname())
                .command(command)
                .script(script)
                .result(cmdLogInfoResult)
                .isSuccess(!agentJsonObject.getBooleanValue("isError", true))
                .build();
        this.save(cmdLogInfo);
    }
}
