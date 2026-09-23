package com.cloud.receiver.util.msg;

import com.cloud.base.spring.ApplicationContextHelper;
import com.cloud.base.util.ThreadPoolUtil;
import com.cloud.ops.agent.Agent;
import com.cloud.ops.alert.MailConfigCommonService;
import com.cloud.ops.app.AppInfo;
import com.cloud.ops.email.MailConfig;
import com.cloud.ops.heath.ApiHeathMonitor;
import com.cloud.ops.server.StaticKeys;
import com.cloud.ops.system.CpuState;
import com.cloud.ops.system.DiskState;
import com.cloud.ops.system.MemState;
import com.cloud.ops.system.SystemInfo;
import com.cloud.receiver.service.RWarnLogInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.HtmlEmail;

import java.util.Date;

@Slf4j
public class WarnMailUtil {

    public static final String content_suffix = "<p><a target='_blank' href='http://bisenpark.com'>百胜智能</a>敬上";

    private static final RWarnLogInfoService logInfoService = ApplicationContextHelper.getBean(RWarnLogInfoService.class);

    private static final MailConfigCommonService mailConfigService = ApplicationContextHelper.getBean(MailConfigCommonService.class);


    /**
     * 判断系统内存使用率是否超过98%，超过则发送告警邮件
     *
     * @param memState
     * @param agentConfig
     * @return
     */
    public static boolean sendMemWarnInfo(MemState memState, Agent agentConfig) {
        if (memState.getUsePer() == null) {
            return false;
        }
        try {
            if (agentConfig.getMemWarnVal() == null) {
                return false;
            }
            if (memState.getUsePer() < agentConfig.getMemWarnVal()) {
                return false;
            }
            String title = "服务器内存告警";
            String commContent = "服务器：" + agentConfig.getHostname() + "内存使用率为" + memState.getUsePer() + "%" + ",阈值:" + agentConfig.getMemWarnVal() + "%";
            logInfoService.saveErrorLog(title, commContent, commContent, agentConfig, agentConfig.getMemWarnVal() + "%");
        } catch (Exception e) {
            log.error("发送内存告警邮件失败：", e);
            logInfoService.saveErrorLog("发送内存告警邮件错误", e.toString(), null, agentConfig);
        }
        return true;
    }


    /**
     * 判断系统cpu使用率是否超过98%，超过则发送告警邮件
     *
     * @param cpuState
     * @param agentConfig
     * @return
     */
    public static boolean sendCpuWarnInfo(CpuState cpuState, Agent agentConfig) {
        if (cpuState.getSys() == null) {
            return false;
        }
        try {
            if (agentConfig.getCpuWarnVal() == null) {
                return false;
            }
            if (cpuState.getSys() < agentConfig.getCpuWarnVal()) {
                return false;
            }
            String title = "CPU告警";
            String commContent = "服务器：" + cpuState.getHostname() + ",CPU使用率为" +
                    cpuState.getSys() + "%，阈值:" + agentConfig.getCpuWarnVal() + "%";
            logInfoService.saveErrorLog(title, commContent, commContent, agentConfig, agentConfig.getCpuWarnVal() + "%");
        } catch (Exception e) {
            log.error("发送内存告警邮件失败：", e);
            logInfoService.saveErrorLog("发送CPU告警邮件错误", e.toString(), null, agentConfig);
        }
        return true;
    }

    public static void sendWarnMail(Agent agentConfig, String title, String commContent) {
        if (StaticKeys.mailConfig == null) {
            return;
        }
        if (!agentConfig.getIsMail()) {
            return;
        }
        MailConfig mailConfig = StaticKeys.mailConfig;
        sendMail(null, mailConfig.getToMail(), agentConfig.getServiceName() + " " + title, commContent);
    }


    /**
     * 主机下线发送告警邮件
     *
     * @param systemInfo 主机信息
     * @param isDown     是否是下线告警，true下线告警，false上线恢复
     * @return
     */
    public static boolean sendHostDown(SystemInfo systemInfo, boolean isDown) {
        if (isDown) {
            try {
                String commContent = "主机已经超过10分钟未上报数据，可能已经下线：" + systemInfo.getHostname() + "，备注：" + systemInfo.getRemark()
                        + "。如果不再监控该主机在列表删除即可，同时不会再收到该主机告警邮件";
                //记录发送信息
                logInfoService.saveErrorLog("主机下线告警", commContent, systemInfo);
            } catch (Exception e) {
                log.error("发送主机下线告警邮件失败：", e);
                logInfoService.save("发送主机下线告警邮件错误", e.toString(), StaticKeys.LOG_ERROR);
            }
        } else {
            try {
                String commContent = "主机已经恢复上线：" + systemInfo.getHostname() + "，备注：" + systemInfo.getRemark()
                        + "。";
                //记录发送信息
                logInfoService.saveErrorLog("主机恢复上线通知", commContent, systemInfo);
            } catch (Exception e) {
                log.error("发送主机恢复上线通知邮件失败：", e);
                logInfoService.save("发送主机恢复上线通知邮件错误", e.toString(), StaticKeys.LOG_ERROR);
            }
        }
        return false;
    }

    /**
     * 进程下线发送告警邮件
     *
     * @param appInfo 进程信息
     * @param isDown  是否是下线告警，true下线告警，false上线恢复
     * @return
     */
    public static boolean sendAppDown(AppInfo appInfo, boolean isDown) {
        if (StaticKeys.mailConfig == null) {
            return false;
        }
        MailConfig mailSet = StaticKeys.mailConfig;
        String key = appInfo.getId().toString();
        if (isDown) {
            if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(key))) {
                return false;
            }
            try {
                String title = "进程下线告警：" + appInfo.getHostname() + "，" + appInfo.getAppName();
                String commContent = "进程已经超过10分钟未上报数据，可能已经下线：" + appInfo.getHostname() + "，" + appInfo.getAppName()
                        + "。如果不再监控该进程在列表删除即可，同时不会再收到该进程告警邮件";
                //发送邮件
                sendMail(null, mailSet.getToMail(), title, commContent);
                //标记已发送过告警信息
                WarnPools.MEM_WARN_MAP.put(key, "1");
                //记录发送信息
                logInfoService.save(title, commContent, StaticKeys.LOG_ERROR);
            } catch (Exception e) {
                log.error("发送进程下线告警邮件失败：", e);
                logInfoService.save("发送进程下线告警错误", e.toString(), StaticKeys.LOG_ERROR);
            }
        } else {
            WarnPools.MEM_WARN_MAP.remove(key);
            try {
                String title = "进程恢复上线通知：" + appInfo.getHostname() + "，" + appInfo.getAppName();
                String commContent = "进程恢复上线通知：" + appInfo.getHostname() + "，" + appInfo.getAppName();
                //发送邮件
                sendMail(null, mailSet.getToMail(), title, commContent);
                //记录发送信息
                logInfoService.save(title, commContent, StaticKeys.LOG_ERROR);
            } catch (Exception e) {
                log.error("发送进程恢复上线通知邮件失败：", e);
                logInfoService.save("发送进程恢复上线通知错误", e.toString(), StaticKeys.LOG_ERROR);
            }
        }
        return false;
    }

    public static String sendMail(MailConfig mailConfig, String mailTitle, String mailContent) {
        ThreadPoolUtil.getInstance().getNewCachedThreadPool().execute(() -> {
            try {
                HtmlEmail email = new HtmlEmail();
                email.setHostName(mailConfig.getSmtpHost());
                email.setSmtpPort(mailConfig.getSmtpPort());
                email.setSSLOnConnect(true);
                email.setAuthenticator(new DefaultAuthenticator(mailConfig.getFromMailName().trim(), mailConfig.getFromPwd().trim()));
                email.setFrom(mailConfig.getFromMailName().trim());//发信者
                email.setSubject("[OPS预警] " + mailTitle);//标题
                email.setCharset("UTF-8");//编码格式
                email.setHtmlMsg(mailContent + content_suffix);//内容
                email.addTo(mailConfig.getToMail().trim().split(";"));
                email.setSentDate(new Date());
                email.send();//发送
            } catch (Exception e) {
                log.error("发送邮件错误：", e);
                logInfoService.save("发送邮件错误", e.toString(), StaticKeys.LOG_ERROR);
                //return "error";
            }
        });
        return null;
    }


    public static String sendMail(Long createBy, String mails, String mailTitle, String mailContent) {
        MailConfig mailConfig = mailConfigService.getByUserId(createBy);
        if (mailConfig == null) {
            return null;
        }
        if (!mailConfig.getIsSendMail()) {
            return null;
        }
        mailConfig.setToMail(mails);
        return sendMail(mailConfig, mailTitle, mailContent);
    }

    public static boolean sendDiskWarnInfo(DiskState diskState, Agent agentConfig) {
        if (diskState.getUsePer() == null) {
            return false;
        }
        try {
            if (agentConfig.getDiskWarnVal() == null) {
                return false;
            }
            if (diskState.getUsePer() < agentConfig.getDiskWarnVal()) {
                return false;
            }
            String title = "服务器磁盘告警";
            String commContent = "磁盘使用率为" +
                    diskState.getUsePer() + "%，磁盘名：" + diskState.getFileSystem();
            logInfoService.saveErrorLog(title, commContent, commContent, agentConfig);
        } catch (Exception e) {
            log.error("发送内存告警邮件失败：", e);
            logInfoService.saveErrorLog("发送磁盘告警邮件错误", e.toString(), null, agentConfig);
        }
        return true;
    }

    public static void sendHeathInfo(ApiHeathMonitor heathMonitor, String logTitle, boolean isEmail, Long responseTime) {

    }
}
