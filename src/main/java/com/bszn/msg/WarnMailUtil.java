package com.bszn.msg;

import cn.hutool.extra.spring.SpringUtil;
import com.bszn.base.util.ThreadPoolUtil;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.email.MailConfig;
import com.bszn.monitor.email.MailConfigService;
import com.bszn.monitor.heath.ApiHeathMonitor;
import com.bszn.monitor.log.LogInfo;
import com.bszn.monitor.log.LogInfoService;
import com.bszn.server.StaticKeys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.HtmlEmail;

import java.util.Date;

@Slf4j
public class WarnMailUtil {

    public static final String content_suffix = "<p><a target='_blank' href='http://bisenpark.com'>百胜智能</a>敬上";

    private static LogInfoService logInfoService = SpringUtil.getBean(LogInfoService.class);
    private static MailConfigService mailService = SpringUtil.getBean(MailConfigService.class);

    public static void sendWarnMail(AgentConfig agentConfig, String title, String commContent) {
        if (StaticKeys.mailSet == null) {
            return;
        }
        if (!agentConfig.getIsMail()) {
            return;
        }
        MailConfig mailSet = StaticKeys.mailSet;
        sendMail(mailSet, agentConfig.getServiceName() + " " + title, commContent);
    }


    /**
     * 服务接口不通发送告警邮件
     *
     * @param heathMonitor
     * @return
     */
    public static boolean sendHeathInfo(ApiHeathMonitor heathMonitor, String logTitle, boolean isEmail, Long time) {
        try {
            String commContent = heathMonitor.getApiName() + "接口：" + heathMonitor.getApiUrl()
                    + "，响应状态码为" + heathMonitor.getHeathStatus() + "，请求时长:" + time + "秒";
            if (isEmail) {
                MailConfig mailSet = mailService.getByServiceId(heathMonitor.getServiceId());
                if (mailSet == null)
                    return false;
                isEmail = logInfoService.checkSendEmail(mailSet, logTitle);
                if (isEmail) {
                    WarnMailUtil.sendMail(mailSet, heathMonitor.getServiceName() + logTitle, commContent);
                }
            }
            logInfoService.save(new LogInfo(logTitle, commContent, heathMonitor.getServiceId(), heathMonitor.getServiceName(), isEmail));
        } catch (Exception e) {
            log.error("服务接口告警邮件失败：", e);
        }
        return isEmail;
    }


    public static String sendMail(MailConfig mailSet, String mailTitle, String mailContent) {
        ThreadPoolUtil.getInstance().getNewCachedThreadPool().execute(() -> {
            try {
                HtmlEmail email = new HtmlEmail();
                email.setHostName(mailSet.getSmtpHost());
                email.setSmtpPort(mailSet.getSmtpPort());
                if (mailSet.getIsSendMail()) {
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
                log.error("发送邮件错误：", e);
                logInfoService.save("发送邮件错误", e.toString(), StaticKeys.LOG_ERROR);
                //return "error";
            }
        });
        return null;
    }


    public static String sendMail(String mails, String mailTitle, String mailContent) {
        ThreadPoolUtil.getInstance().getNewCachedThreadPool().execute(() -> {
            try {
                HtmlEmail email = new HtmlEmail();
                email.setHostName(StaticKeys.mailSet.getSmtpHost());
                email.setSmtpPort(Integer.valueOf(StaticKeys.mailSet.getSmtpPort()));
                if ((StaticKeys.mailSet.getIsSmtpSsl())) {
                    email.setSSL(true);
                }
                email.setAuthenticator(new DefaultAuthenticator(StaticKeys.mailSet.getFromMailName(), StaticKeys.mailSet.getFromPwd()));
                email.setFrom(StaticKeys.mailSet.getFromMailName());//发信者
                email.setSubject("[百胜智能] " + mailTitle);//标题
                email.setCharset("UTF-8");//编码格式
                email.setHtmlMsg(mailContent + content_suffix);//内容
                email.addTo(mails.split(";"));
                email.setSentDate(new Date());
                email.send();//发送
                //   return "success";
            } catch (Exception e) {
                log.error("发送邮件错误：", e);
                logInfoService.save("发送邮件错误", e.toString(), StaticKeys.LOG_ERROR);
                //return "error";
            }
        });
        return null;
    }
}
