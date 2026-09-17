package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.base.util.DateUtil;
import com.cloud.base.util.MyIdWorker;
import com.cloud.ops.agent.Agent;
import com.cloud.ops.alert.WarnLogInfo;
import com.cloud.ops.alert.WarnLogInfoMapper;
import com.cloud.ops.server.StaticKeys;
import com.cloud.ops.system.SystemInfo;
import com.cloud.receiver.util.msg.WarnMailUtil;
import com.cloud.receiver.util.staticvar.BatchData;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RWarnLogInfoService extends ServiceImpl<WarnLogInfoMapper, WarnLogInfo> {

    @Resource
    private WarnLogInfoMapper logInfoMapper;

    @Resource
    private RAgentConfigServiceImpl rAgentConfigService;


    public void saveRecord(List<WarnLogInfo> recordList) {
        if (recordList.isEmpty()) {
            return;
        }
        for (WarnLogInfo logInfo : recordList) {
            logInfo.setId(MyIdWorker.getId());
        }
        this.saveBatch(recordList);
    }

    public void save(String hostname, String infoContent, String state) {
        WarnLogInfo logInfo = new WarnLogInfo();
        // logInfo.setHostname(hostname);
        logInfo.setTitle(hostname);
        logInfo.setInfoContent(infoContent);
        logInfo.setState(state);
        logInfoMapper.insert(logInfo);
    }

    private boolean checkSendEmail(String hostname, String title, Boolean isEmail) {
        if (isEmail == null || !isEmail) {
            return false;
        }
        boolean sendEmail = true;
        WarnLogInfo temp = logInfoMapper.getLastByHostnameAndTitle(hostname, title);
        if (temp != null) {
            long minute = DateUtil.differMinute(temp.getCreateTime(), LocalDateTime.now());
            //小于八个小时
            if (minute <= 60 * 8) {
                sendEmail = false;
            }
        }
        return sendEmail;
    }

    public boolean saveErrorLog(String title, String infoContent, String emailContent, Agent agentConfig) {
        return this.saveErrorLog(title, infoContent, emailContent, agentConfig, null);
    }

    public boolean saveErrorLog(String title, String infoContent, String emailContent, Agent agentConfig, String threshold) {
        if (StringUtils.isEmpty(title)) {
            return false;
        }
        boolean sendEmail = this.checkSendEmail(agentConfig.getHostname(), title, agentConfig.getIsMail());
        WarnLogInfo logInfo = new WarnLogInfo();
        logInfo.setHostname(agentConfig.getHostname());
        logInfo.setServiceName(agentConfig.getServiceName());
        logInfo.setServiceId(agentConfig.getServiceId());
        logInfo.setTitle(title);
        logInfo.setState(StaticKeys.LOG_ERROR);
        logInfo.setInfoContent(infoContent);
        logInfo.setSendEmail(sendEmail);
        logInfo.setThreshold(threshold);
        logInfoMapper.insert(logInfo);
        if (sendEmail && StringUtils.isNotEmpty(emailContent)) {
            WarnMailUtil.sendWarnMail(agentConfig, title, emailContent);
        }
        return sendEmail;
    }

    public void insertBatch() {
        try {
            if (BatchData.LOG_INFO_LIST.isEmpty()) {
                return;
            }
            List<WarnLogInfo> LOG_INFO_LIST = new ArrayList<>();
            LOG_INFO_LIST.addAll(BatchData.LOG_INFO_LIST);
            BatchData.LOG_INFO_LIST.clear();
            this.saveRecord(LOG_INFO_LIST);
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    public void saveAgentJsonObject(JSONObject agentJsonObject, Agent agentConfig) {
        String warnLogInfo = agentJsonObject.getString("warnLogInfo");
        if (StringUtils.isEmpty(warnLogInfo)) {
            return;
        }
        this.saveErrorLog(agentJsonObject.getString("title"), warnLogInfo, warnLogInfo
                , agentConfig, agentJsonObject.getString("threshold"));
    }

    public void saveErrorLog(String title, String infoContent, SystemInfo systemInfo) {
        if (StringUtils.isEmpty(title)) {
            return;
        }
        Agent agentConfig = rAgentConfigService.getServiceIdAndHostname(systemInfo.getServiceId(), systemInfo.getHostname());
        if (agentConfig == null) {
            return;
        }
        this.saveErrorLog(title, infoContent, infoContent, agentConfig);
    }

}
