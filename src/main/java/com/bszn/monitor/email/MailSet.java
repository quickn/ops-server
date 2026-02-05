package com.bszn.monitor.email;

import com.bszn.base.ServiceBaseEntity;
import lombok.Data;


@Data
public class MailSet extends ServiceBaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = -8284741180883299533L;


    /**
     * 是否发送邮件告警,1发送0不发送
     */
    private String sendMail;

    /**
     * 发送邮箱的帐号
     */
    private String fromMailName;

    /**
     * 发送邮箱的密码
     */
    private String fromPwd;

    /**
     * 发送邮箱的SMTP服务器
     */
    private String smtpHost;

    /**
     * 发送邮箱的SMTP端口,25或465
     */
    private String smtpPort;

    /**
     * 发送邮箱是否启用安全链接(SSL),1启用,0不启用
     */
    private String smtpSsl;

    /**
     * 接受告警信息的邮件
     */
    private String toMail;

    /**
     * cpu使用率告警值
     */
    private String cpuPer;
    /**
     * mem使用率告警值
     */
    private String memPer;

    /**
     * 接口告警间隔
     */
    private Integer heathInterval;

}