package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.receiver.entity.Agent;
import com.cloud.receiver.entity.SystemInfo;
import com.cloud.receiver.entity.WarnLogInfo;
import com.cloud.receiver.mapper.SystemInfoMapper;
import com.cloud.receiver.util.DateUtil;
import com.cloud.receiver.util.msg.WarnMailUtil;
import com.cloud.receiver.util.msg.WarnPools;
import com.cloud.receiver.util.staticvar.StaticKeys;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SystemInfoService extends ServiceImpl<SystemInfoMapper, SystemInfo> {

    @Resource
    SystemInfoMapper systemInfoMapper;

    @Resource
    WarnLogInfoService logInfoService;

    @Resource
    AgentConfigServiceImpl iAgentConfigService;

    @Transactional
    public void updateRecord(List<SystemInfo> recordList) {
        if (recordList.isEmpty()) {
            return;
        }
        this.updateBatchById(recordList);
    }

    public void saveAgentJsonObject(JSONObject agentJsonObject, Agent agentConfig) {
        JSONObject systemInfoJson = agentJsonObject.getJSONObject("systemInfo");
        if (systemInfoJson == null) {
            return;
        }
        SystemInfo systemInfo = systemInfoJson.toJavaObject(SystemInfo.class);
        systemInfo.setServiceId(agentConfig.getServiceId());
        systemInfo.setServiceName(agentConfig.getServiceName());
        SystemInfo temp = this.baseMapper.selectOne(Wrappers.<SystemInfo>lambdaQuery()
                .eq(SystemInfo::getServiceId, systemInfo.getServiceId())
                .eq(SystemInfo::getHostname, systemInfo.getHostname()));
        if (temp == null) {
            this.baseMapper.insert(systemInfo);
        } else {
            systemInfo.setId(temp.getId());
            systemInfo.setState(1);
            this.baseMapper.updateById(systemInfo);
        }
    }

    public List<SystemInfo> listAll() {
        return systemInfoMapper.listAll();
    }

    public void checkDown() {
        List<SystemInfo> list = this.listAll();
        if (list.isEmpty()) {
            return;
        }
        Date date = DateUtil.getNowTime();
        long delayTime = 5 * 60 * 1000;
        List<SystemInfo> updateList = new ArrayList<>();
        List<WarnLogInfo> logInfoList = new ArrayList<>();
        for (SystemInfo systemInfo : list) {
            Date updateTime = systemInfo.getUpdateTime();
            if (updateTime == null) {
                continue;
            }
            long diff = date.getTime() - updateTime.getTime();
            if (diff > delayTime) {
                if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(systemInfo.getId()))) {
                    continue;
                }
                systemInfo.setState(StaticKeys.DOWN_STATE);
                WarnLogInfo logInfo = new WarnLogInfo();
                logInfo.setHostname(systemInfo.getHostname());
                logInfo.setTitle("主机下线");
                logInfo.setInfoContent("超过5分钟未上报状态，可能已下线：" + systemInfo.getHostname());
                logInfo.setState(StaticKeys.LOG_ERROR);
                logInfo.setServiceId(systemInfo.getServiceId());
                logInfo.setServiceName(systemInfo.getServiceName());
                logInfoList.add(logInfo);
                updateList.add(systemInfo);
                iAgentConfigService.offline(systemInfo.getServiceId(), systemInfo.getHostname());
                WarnMailUtil.sendHostDown(systemInfo, true);
            } else {
                if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(systemInfo.getId()))) {
                    WarnMailUtil.sendHostDown(systemInfo, false);
                }
            }
        }
        if (!updateList.isEmpty()) {
            this.updateRecord(updateList);
        }
        if (!logInfoList.isEmpty()) {
            logInfoService.saveRecord(logInfoList);
        }
    }
}
