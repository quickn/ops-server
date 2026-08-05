package com.bszn.monitor.warnLog;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bszn.monitor.agent.Agent;
import com.bszn.monitor.agent.AgentService;
import com.bszn.monitor.email.MailConfig;
import com.bszn.msg.WarnMailUtil;
import com.bszn.server.StaticKeys;
import com.bszn.server.SystemInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

@Service
@Slf4j
public class WarnLogInfoServiceImpl extends ServiceImpl<WarnLogInfoMapper, WarnLogInfo> implements WarnLogInfoService {

    @Autowired
    private WarnLogInfoMapper logInfoMapper;
    @Resource
    AgentService iAgentConfigService;

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
            long minute = WarnLogInfoServiceImpl.differMinute(temp.getCreateTime(), new Date());
            //小于八个小时
            if (minute <= 60 * 8) {
                sendEmail = false;
            }
        }
        return sendEmail;
    }


    public boolean saveErrorLog(String title, String infoContent, String emailContent, Agent agentConfig) {
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
        logInfoMapper.insert(logInfo);
        if (sendEmail && StringUtils.isNotEmpty(emailContent)) {
            WarnMailUtil.sendWarnMail(agentConfig, title, emailContent);
        }
        return sendEmail;
    }

    /**
     * 获取截止日期与起始日期相差的分钟数
     *
     * @param start 起始日期
     * @param end   截止日期
     * @return
     */
    public static Long differMinute(LocalDateTime start, Date end) {
        if (start != null && end != null) {
            long differTime = end.getTime() - Timestamp.valueOf(start).getTime();
            Long m = differTime / 1000 / 60;
            if ((differTime / 1000) % 60 > 0) {//不足一分钟算一分钟
                m += 1;
            }
            if (m < 0) return 0L;
            return m;
        } else {
            return 0L;
        }
    }

    public void saveErrorLog(String title, String infoContent, SystemInfo systemInfo) {
        if (StringUtils.isEmpty(title)) {
            return;
        }
        Agent agentConfig = iAgentConfigService.getServiceIdAndHostname(systemInfo.getServiceId(), systemInfo.getHostname());
        if (agentConfig == null) {
            return;
        }
        this.saveErrorLog(title, infoContent, infoContent, agentConfig);
    }

    @Override
    public boolean checkSendEmail(MailConfig mailConfig, String title) {
        if (!mailConfig.getIsSendMail()) {
            return false;
        }
        WarnLogInfo temp = logInfoMapper.getLastByServiceIdAndTitle(mailConfig.getServiceId(), title);
        if (temp != null) {
            long minute = WarnLogInfoServiceImpl.differMinute(temp.getCreateTime(), new Date());
            //小于1个小时
            if (minute <= mailConfig.getTimeInterval()) {
                return false;
            }
        }
        return true;
    }

}
