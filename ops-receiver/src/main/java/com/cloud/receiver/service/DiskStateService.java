package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.receiver.entity.Agent;
import com.cloud.receiver.entity.DiskState;
import com.cloud.receiver.mapper.DiskStateMapper;
import com.cloud.receiver.util.MyIdWorker;
import com.cloud.receiver.util.msg.WarnMailUtil;
import com.cloud.receiver.util.staticvar.BatchData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DiskStateService extends ServiceImpl<DiskStateMapper, DiskState> {

    @Transactional
    public void saveRecord(List<DiskState> recordList) {
        if (recordList.isEmpty()) {
            return;
        }
        for (DiskState as : recordList) {
            as.setId(MyIdWorker.getUUID());
        }
        this.saveBatch(recordList);
    }

    public void saveAgentJsonObject(JSONObject agentJsonObject, Agent agentConfig) {
        JSONArray deskStateList = agentJsonObject.getJSONArray("deskStateList");
        if (deskStateList == null) {
            return;
        }
        ArrayList<DiskState> list = new ArrayList<>();
        for (Object jsonObjects : deskStateList) {
            JSONObject jsonObject = (JSONObject) jsonObjects;
            String userPer = jsonObject.getString("usePer");
            if (StringUtils.isEmpty(userPer)) {
                continue;
            }
            Double uPer = Double.valueOf(userPer.replace("%", ""));
            jsonObject.replace("usePer", uPer);
            DiskState bean = jsonObject.toJavaObject(DiskState.class);
            bean.setServiceId(agentConfig.getServiceId());
            bean.setServiceName(agentConfig.getServiceName());
            list.add(bean);
            WarnMailUtil.sendDiskWarnInfo(bean, agentConfig);
        }
        BatchData.DESK_STATE_MAP.put(agentConfig.getHostname(), list);
    }

    public void deleteByHostname(String hostname) {
        this.baseMapper.delete(new LambdaQueryWrapper<DiskState>().eq(DiskState::getHostname, hostname));
    }

    public void insertBatch() {
        try {
            if (BatchData.DESK_STATE_MAP.isEmpty()) {
                return;
            }
            for (Map.Entry<String, ArrayList<DiskState>> entry : BatchData.DESK_STATE_MAP.entrySet()) {
                this.deleteByHostname(entry.getKey());
                this.saveRecord(entry.getValue());
            }
            BatchData.DESK_STATE_MAP.clear();
        } catch (Exception e) {
            log.error("error", e);
        }
    }
}
