package com.cloud.msg;

import cn.hutool.extra.spring.SpringUtil;
import com.cloud.ops.agent.Agent;
import com.cloud.ops.email.MailConfig;
import com.cloud.ops.email.MailConfigService;
import com.cloud.ops.heath.ApiHeathMonitor;
import com.cloud.ops.warnLog.WarnLogInfo;
import com.cloud.ops.warnLog.WarnLogInfoService;
import com.cloud.server.StaticKeys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.HtmlEmail;

import java.util.Date;

@Slf4j
public class WarnMailUtil {

    public static final String content_suffix = "<p><a target='_blank' href='http://bisenpark.com'>百胜智能</a>敬上";

    private static WarnLogInfoService logInfoService = SpringUtil.getBean(WarnLogInfoService.class);
    private static MailConfigService mailService = SpringUtil.getBean(MailConfigService.class);

    public static void sendWarnMail(Agent agentConfig, String title, String commContent) {
        if (StaticKeys.mailConfig == null) {
            return;
        }
        if (!agentConfig.getIsMail()) {
            return;
        }
        MailConfig mailConfig = StaticKeys.mailConfig;
        sendMail(mailConfig, agentConfig.getServiceName() + " " + title, commContent);
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
            logInfoService.save(new WarnLogInfo(logTitle, commContent, heathMonitor.getServiceId(), heathMonitor.getServiceName(), isEmail));
        } catch (Exception e) {
            log.error("服务接口告警邮件失败：", e);
        }
        return isEmail;
    }


    public static String sendMail(MailConfig mailConfig, String mailTitle, String mailContent) {
        try {
            if (!mailConfig.getIsSendMail()) {
                return null;
            }
            HtmlEmail email = new HtmlEmail();
            email.setHostName(mailConfig.getSmtpHost());
            email.setSmtpPort(mailConfig.getSmtpPort());
            email.setSSLOnConnect(true);
            email.setAuthenticator(new DefaultAuthenticator(mailConfig.getFromMailName().trim(), mailConfig.getFromPwd().trim()));
            email.setFrom(mailConfig.getFromMailName().trim());//发信者
            email.setSubject("[百胜智能] " + mailTitle);//标题
            email.setCharset("UTF-8");//编码格式
            email.setHtmlMsg(mailContent + content_suffix);//内容
            email.addTo(mailConfig.getToMail().trim().split(";"));
            email.setSentDate(new Date());
            email.send();//发送
            return null;
        } catch (Exception e) {
            log.error("发送邮件错误：", e);
            logInfoService.save("发送邮件错误", e.toString(), StaticKeys.LOG_ERROR);
            return e.getMessage();
        }
    }

}
