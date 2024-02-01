package com.youlai.msg;

import cn.hutool.extra.spring.SpringUtil;
import com.youlai.base.util.ThreadPoolUtil;
import com.youlai.monitor.agent.AgentConfig;
import com.youlai.monitor.heath.HeathMonitor;
import com.youlai.monitor.log.LogInfo;
import com.youlai.monitor.log.LogInfoService;
import com.youlai.server.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @version v2.3
 * @ClassName:WarnMailUtil.java
 * @author: http://www.wgstart.com
 * @date: 2019年11月16日
 * @Description: WarnMailUtil.java
 * @Copyright: 2017-2022 wgcloud. All rights reserved.
 */
public class WarnMailUtil {

    private static final Logger logger = LoggerFactory.getLogger(WarnMailUtil.class);

    public static final String content_suffix = "<p><a target='_blank' href='http://bisenpark.com'>百胜智能</a>敬上";

    private static LogInfoService logInfoService = SpringUtil.getBean(LogInfoService.class);
    private static MailConfig mailConfig = SpringUtil.getBean(MailConfig.class);

    private static MailService mailService = SpringUtil.getBean(MailService.class);

    /**
     * 判断系统内存使用率是否超过98%，超过则发送告警邮件
     *
     * @param memState
     * @param agentConfig
     * @return
     */
    public static boolean sendMemWarnInfo(MemState memState, AgentConfig agentConfig) {
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
            String commContent = "服务器：" + agentConfig.getHostname() + "内存使用率为" + Double.valueOf(memState.getUsePer()) + "%" + ",阈值:" + agentConfig.getMemWarnVal() + "%";
            logInfoService.saveErrorLog(title, commContent, commContent, agentConfig);
        } catch (Exception e) {
            logger.error("发送内存告警邮件失败：", e);
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
    public static boolean sendCpuWarnInfo(CpuState cpuState, AgentConfig agentConfig) {
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
            String commContent = "服务器：" + cpuState.getHostname() + ",CPU使用率为" + Double.valueOf(cpuState.getSys()) + "%，阈值:" + agentConfig.getCpuWarnVal() + "%";
            logInfoService.saveErrorLog(title, commContent, commContent, agentConfig);
        } catch (Exception e) {
            logger.error("发送内存告警邮件失败：", e);
            logInfoService.saveErrorLog("发送CPU告警邮件错误", e.toString(), null, agentConfig);
        }
        return true;
    }

    public static void sendWarnMail(AgentConfig agentConfig, String title, String commContent) {
        if (StaticKeys.mailSet == null) {
            return;
        }
        if (!agentConfig.isMail()) {
            return;
        }
        MailSet mailSet = StaticKeys.mailSet;
        sendMail(mailSet, agentConfig.getServiceName() + " " + title, commContent);
    }


    /**
     * 服务接口不通发送告警邮件
     *
     * @param heathMonitor
     * @return
     */
    public static boolean sendHeathInfo(HeathMonitor heathMonitor, String logTitle, boolean isEmail, Long time) {
        try {
            String commContent = heathMonitor.getAppName() + "接口：" + heathMonitor.getHeathUrl()
                    + "，响应状态码为" + heathMonitor.getHeathStatus() + "，请求时长:" + time + "毫秒";
            if (isEmail) {
                MailSet mailSet = mailService.getByServiceId(heathMonitor.getServiceId());
                isEmail = logInfoService.checkSendEmail(mailSet, logTitle);
                if (isEmail) {
                    WarnMailUtil.sendMail(mailSet, heathMonitor.getServiceName() + logTitle, commContent);
                }
            }
            logInfoService.save(new LogInfo(logTitle, commContent, heathMonitor.getServiceId(), heathMonitor.getServiceName(), isEmail));
        } catch (Exception e) {
            logger.error("发送主机下线告警邮件失败：", e);
        }
        return isEmail;
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
                String commContent = "主机已经超过10分钟未上报数据，可能已经下线：" + systemInfo.getHostname() + "，备注：" + systemInfo.getRemark() + "。如果不再监控该主机在列表删除即可，同时不会再收到该主机告警邮件";
                //记录发送信息
                logInfoService.saveErrorLog("主机下线告警", commContent, systemInfo);
            } catch (Exception e) {
                logger.error("发送主机下线告警邮件失败：", e);
                logInfoService.save("发送主机下线告警邮件错误", e.toString(), StaticKeys.LOG_ERROR);
            }
        } else {
            try {
                String commContent = "主机已经恢复上线：" + systemInfo.getHostname() + "，备注：" + systemInfo.getRemark() + "。";
                //记录发送信息
                logInfoService.saveErrorLog("主机恢复上线通知", commContent, systemInfo);
            } catch (Exception e) {
                logger.error("发送主机恢复上线通知邮件失败：", e);
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
        if (StaticKeys.mailSet == null) {
            return false;
        }
        MailSet mailSet = StaticKeys.mailSet;
        if (StaticKeys.NO_SEND_WARN.equals(mailConfig.getAllWarnMail()) || StaticKeys.NO_SEND_WARN.equals(mailConfig.getAppDownWarnMail())) {
            return false;
        }
        String key = appInfo.getId().toString();
        if (isDown) {
            if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(key))) {
                return false;
            }
            try {
                String title = "进程下线告警：" + appInfo.getHostname() + "，" + appInfo.getAppName();
                String commContent = "进程已经超过10分钟未上报数据，可能已经下线：" + appInfo.getHostname() + "，" + appInfo.getAppName() + "。如果不再监控该进程在列表删除即可，同时不会再收到该进程告警邮件";
                //发送邮件
                sendMail(mailSet, title, commContent);
                //标记已发送过告警信息
                WarnPools.MEM_WARN_MAP.put(key, "1");
                //记录发送信息
                logInfoService.save(title, commContent, StaticKeys.LOG_ERROR);
            } catch (Exception e) {
                logger.error("发送进程下线告警邮件失败：", e);
                logInfoService.save("发送进程下线告警错误", e.toString(), StaticKeys.LOG_ERROR);
            }
        } else {
            WarnPools.MEM_WARN_MAP.remove(key);
            try {
                String title = "进程恢复上线通知：" + appInfo.getHostname() + "，" + appInfo.getAppName();
                String commContent = "进程恢复上线通知：" + appInfo.getHostname() + "，" + appInfo.getAppName();
                //发送邮件
                sendMail(mailSet, title, commContent);
                //记录发送信息
                logInfoService.save(title, commContent, StaticKeys.LOG_ERROR);
            } catch (Exception e) {
                logger.error("发送进程恢复上线通知邮件失败：", e);
                logInfoService.save("发送进程恢复上线通知错误", e.toString(), StaticKeys.LOG_ERROR);
            }
        }
        return false;
    }

    public static String sendMail(MailSet mailSet, String mailTitle, String mailContent) {
        ThreadPoolUtil.getInstance().getNewCachedThreadPool().execute(() -> {
            try {
                HtmlEmail email = new HtmlEmail();
                email.setHostName(mailSet.getSmtpHost());
                email.setSmtpPort(Integer.valueOf(mailSet.getSmtpPort()));
                if ("1".equals(mailSet.getSmtpSsl())) {
                    email.setSSL(true);
                }
                email.setAuthenticator(new DefaultAuthenticator(mailSet.getFromMailName(), mailSet.getFromPwd()));
                email.setFrom(mailSet.getFromMailName());//发信者
                email.setSubject("[百胜智能] " + mailTitle);//标题
                email.setCharset("UTF-8");//编码格式
                email.setHtmlMsg(mailContent + content_suffix);//内容
                email.addTo(mailSet.getToMail().split(";"));
                email.setSentDate(new Date());
                email.send();//发送
                //   return "success";
            } catch (Exception e) {
                logger.error("发送邮件错误：", e);
                logInfoService.save("发送邮件错误", e.toString(), StaticKeys.LOG_ERROR);
                //return "error";
            }
        });
        return null;
    }

    public static boolean sendDiskWarnInfo(DiskState diskState, AgentConfig agentConfig) {
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
            String commContent = "磁盘使用率为" + Double.valueOf(diskState.getUsePer()) + "%，磁盘名：" + diskState.getFileSystem();
            logInfoService.saveErrorLog(title, commContent, commContent, agentConfig);
        } catch (Exception e) {
            logger.error("发送内存告警邮件失败：", e);
            logInfoService.saveErrorLog("发送磁盘告警邮件错误", e.toString(), null, agentConfig);
        }
        return true;
    }
}
