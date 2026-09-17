package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.ops.agent.Agent;
import com.cloud.ops.processnetstat.ProcessStat;
import com.cloud.ops.processnetstat.ProcessStatMapper;
import com.cloud.receiver.util.CamelCaseUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 进程网络流量统计服务
 * 处理 ops-agent-go 采集的 ProcessNetStat 数据并持久化
 **/
@Service
@Slf4j
public class RProcessStatService extends ServiceImpl<ProcessStatMapper, ProcessStat> {

    @Resource
    CommonDataService commonDataService;

    @Resource
    private RWarnLogInfoService logInfoService;

    /**
     * 解析并保存 ops-agent-go 上报的 ProcessNetStat 数据
     * Go agent 发送 JSON 格式:
     * "processNetStat": [
     * {"name": "java",   "sent": 1024.5, "received": 512.3},
     * {"name": "nginx",  "sent": 2048.0, "received": 1024.0}
     * ]
     */
    public void saveAgentJsonObject(JSONObject agentJsonObject, Agent agentConfig) {
        Object object = agentJsonObject.get("processNetStatList");
        if (object == null) {
            return;
        }
        // Go 协议: JSONArray 格式
        if (object instanceof JSONArray list) {
            for (int i = 0; i < list.size(); i++) {
                JSONObject item = list.getJSONObject(i);
                String processName = item.getString("processName");
                if (StringUtils.isEmpty(processName)) {
                    continue;
                }
                item.put("hostname", agentConfig.getHostname());
                item.put("serviceId", agentConfig.getServiceId());
                item.put("serviceName", agentConfig.getServiceName());
                ProcessStat bean = item.toJavaObject(ProcessStat.class);
                CamelCaseUtil.camelCaseToUnderline(item);
                commonDataService.insert("process_stat", item);
                checkWarn(bean, processName, agentConfig);
            }
        }
    }


    private void checkWarn(ProcessStat bean, String processName, Agent agentConfig) {
        if (agentConfig.getNetThresholdMbps() != null
                && bean.getSentRate() != null
                && (bean.getSentRate() > agentConfig.getNetThresholdMbps() || bean.getRecvRate() > agentConfig.getNetThresholdMbps())) {
            String content = String.format("服务器:%s PID:%s 进程:%s 容器名:%s 发送流量:%.2fKB,接收流量:%.2fKB,阈值:%sfKB",
                    bean.getHostname(), bean.getPid(), processName, bean.getContainerName(), bean.getSentRate(), bean.getRecvRate(), agentConfig.getNetThresholdMbps());
            logInfoService.saveErrorLog("进程发送流量告警", content, content, agentConfig, agentConfig.getNetThresholdMbps() + "KB");
        }
    }
}
