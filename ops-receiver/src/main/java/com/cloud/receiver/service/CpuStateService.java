package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cloud.ops.agent.Agent;
import com.cloud.ops.system.CpuState;
import com.cloud.ops.system.CpuStateMapper;
import com.cloud.receiver.util.DateUtil;
import com.cloud.receiver.util.ThreadLocalUtil;
import com.cloud.receiver.util.msg.WarnMailUtil;
import com.cloud.receiver.util.staticvar.BatchData;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CpuStateService {

    @Resource
    private CpuStateMapper cpuStateMapper;

    public void saveRecord(List<CpuState> recordList) throws Exception {
        if (recordList.isEmpty()) {
            return;
        }
        for (CpuState as : recordList) {
            as.setId(IdWorker.getId());
            as.setDateStr(DateUtil.getDateTimeString(as.getCreateTime()));
            cpuStateMapper.insert(as);
        }
    }

    public void saveAgentJsonObject(JSONObject agentJsonObject, Agent agentConfig) {
        JSONObject cpuState = agentJsonObject.getJSONObject("cpuState");
        if (cpuState != null) {
            CpuState bean = cpuState.toJavaObject(CpuState.class);
            bean.setServiceId(ThreadLocalUtil.getServiceId());
            bean.setServiceName(agentConfig.getServiceName());
            BatchData.CPU_STATE_LIST.add(bean);
            WarnMailUtil.sendCpuWarnInfo(bean, agentConfig);
        }
    }
}
