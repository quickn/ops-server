package com.cloud.ops.email;

import com.cloud.base.ServiceBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
public class MailConfig extends ServiceBaseEntity {

    private static final long serialVersionUID = -8284741180883299533L;

    @Schema(description = "是否发送邮件告警")
    private Boolean isSendMail;

    @Schema(description = "邮箱的帐号")
    private String fromMailName;

    @Schema(description = "邮箱的密码")
    private String fromPwd;

    @Schema(description = "邮箱的SMTP服务器")
    private String smtpHost;

    @Schema(description = "邮箱的SMTP端口")
    private Integer smtpPort;

    @Schema(description = "邮箱是否启用安全链接(SSL),1启用,0不启用")
    private Boolean isSmtpSsl;

    @Schema(description = "接受告警信息的邮件")
    private String toMail;

    @Schema(description = "发送时间间隔(分)")
    private Integer timeInterval;

}