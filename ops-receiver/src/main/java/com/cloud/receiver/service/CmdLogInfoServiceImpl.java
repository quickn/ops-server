package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.receiver.entity.Agent;
import com.cloud.receiver.entity.CmdLogInfo;
import com.cloud.receiver.mapper.CmdLogInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CmdLogInfoServiceImpl extends ServiceImpl<CmdLogInfoMapper, CmdLogInfo> {

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
                .isError(agentJsonObject.getBoolean("isError"))
                .createTime(LocalDateTime.now())
                .build();
        this.save(cmdLogInfo);
    }
}
