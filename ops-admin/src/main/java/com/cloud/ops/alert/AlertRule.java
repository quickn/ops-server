package com.cloud.ops.alert;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.ops.base.MultiTenantCreateByEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预警规则配置
 *
 * <p>定义预警的触发条件、通知通道、降噪策略等。</p>
 *
 * @author Liuyun
 */
@Data
@TableName("alert_rule")
@EqualsAndHashCode(callSuper = true)
public class AlertRule extends MultiTenantCreateByEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "预警通道，多选逗号分隔: EMAIL,SMS")
    private String alertChannels;

    @Schema(description = "预警级别: 1-通知 2-警告 3-严重")
    private Integer alertLevel;

    @Schema(description = "触发类型: KEYWORD-关键字触发 THRESHOLD-超过阈值触发")
    private String triggerType;

    @Schema(description = "触发关键字，逗号分隔，结果包含任一关键字即触发")
    private String conditionKeyword;

    @Schema(description = "触发阈值，结果中的数值超过该阈值即触发")
    private Double conditionThreshold;

    @Schema(description = "报警联系人组ID")
    private Long contactGroupId;

    @Schema(description = "报警联系人组名称")
    private String contactGroupName;

    @Schema(description = "是否启用")
    private Boolean isEnabled;

    // ========== 降噪配置 ==========

    @Schema(description = "静默期（分钟）：同一规则触发后，在此时间内不重复发送预警")
    private Integer silencePeriod;

    @Schema(description = "连续触发阈值：连续触发 N 次后才发送预警")
    private Integer triggerThreshold;

    @Schema(description = "聚合窗口（分钟）：窗口内多次触发合并为一条预警")
    private Integer aggregateWindow;

    @Schema(description = "创建人名称")
    private String createByName;

    @Schema(description = "备注")
    private String remark;

}
