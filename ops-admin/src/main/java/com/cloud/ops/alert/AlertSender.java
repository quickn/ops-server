package com.cloud.ops.alert;

import com.cloud.receiver.util.msg.WarnMailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 预警发送器
 *
 * <p>根据预警通道类型分发到对应的发送实现：</p>
 * <ul>
 *   <li>EMAIL - 复用已有的 {@link WarnMailUtil} 邮件发送</li>
 *   <li>SMS  - 短信发送（预留接口，可对接短信网关）</li>
 * </ul>
 *
 * @author Liuyun
 */
@Component
@Slf4j
public class AlertSender {

    /**
     * 发送预警
     *
     * @param channel    预警通道类型
     * @param recipients 接收人列表
     * @param title      预警标题
     * @param content    预警内容
     * @return null=成功，非null=失败原因
     */
    public String send(Long createBy, AlertChannelType channel, List<String> recipients, String title, String content) {
        if (createBy == null)
            return "创建人为空";
        if (channel == null || recipients == null || recipients.isEmpty()) {
            return "预警通道或接收人配置为空";
        }
        try {
            switch (channel) {
                case EMAIL:
                    return sendEmail(createBy, recipients, title, content);
                case SMS:
                    return sendSms(recipients, title, content);
                default:
                    return "不支持的预警通道类型: " + channel;
            }
        } catch (Exception e) {
            log.error("发送预警失败 channel={} title={}", channel, title, e);
            return "发送异常: " + e.getMessage();
        }
    }

    /**
     * 邮件发送 - 复用 WarnMailUtil
     */
    private String sendEmail(Long createBy, List<String> recipients, String title, String content) {
        try {
            // 拼接收件人
            String toMail = String.join(";", recipients);
            WarnMailUtil.sendMail(createBy, toMail, title, content);
            log.info("预警邮件已发送 recipients={}", toMail);
            return null;
        } catch (Exception e) {
            log.error("预警邮件发送失败", e);
            return "邮件发送失败: " + e.getMessage();
        }
    }

    /**
     * 短信发送 - 预留实现
     *
     * <p>当前为日志模拟发送，可对接阿里云短信、腾讯云短信等网关。</p>
     */
    private String sendSms(List<String> recipients, String title, String content) {
        // TODO 对接短信网关（阿里云/腾讯云 SMS API）
        String phones = String.join(",", recipients);
        log.info("【短信预警-模拟发送】phone={} title={} content={}", phones, title, content);
        return null;
    }
}
