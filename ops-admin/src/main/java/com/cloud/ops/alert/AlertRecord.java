package com.cloud.ops.alert;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.ops.base.MultiTenantCreateByEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 预警发送记录
 *
 * @author Liuyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert_record")
public class AlertRecord extends MultiTenantCreateByEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关联预警规则ID")
    private Long ruleId;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "预警级别: 1-通知 2-警告 3-严重")
    private Integer alertLevel;

    @Schema(description = "预警通道: EMAIL / SMS")
    private String alertChannel;

    @Schema(description = "预警内容")
    private String alertContent;

    @Schema(description = "接收人")
    private String recipient;

    @Schema(description = "发送状态: 0-成功 1-失败")
    private Integer sendStatus;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "是否被降噪静默: 0-否 1-是")
    private Integer isSilenced;

    @Schema(description = "连续触发次数（降噪统计）")
    private Integer triggerCount;

    /**
     * 服务ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer serviceId;

    @Schema(description = "服务名称")
    private String serviceName;

    @TableField(exist = false)
    private LocalDateTime updateTime;
}
