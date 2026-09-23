package com.cloud.ops.alert;

import com.cloud.base.sql.IQuery;
import com.cloud.base.sql.PageForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Objects;

/**
 * @author Liuyun
 */
@Data
public class AlertRuleQueryDto extends PageForm<AlertRule> implements IQuery {

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "预警通道")
    private String alertChannels;

    @Schema(description = "预警级别")
    private Integer alertLevel;

    @Schema(description = "是否启用")
    private Boolean isEnabled;

    @Override
    public com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AlertRule> buildLambda() {
        return super.buildLambda()
                .like(Objects.nonNull(ruleName) && !ruleName.isEmpty(), AlertRule::getRuleName, ruleName)
                .like(Objects.nonNull(alertChannels) && !alertChannels.isEmpty(), AlertRule::getAlertChannels, alertChannels)
                .eq(Objects.nonNull(alertLevel), AlertRule::getAlertLevel, alertLevel)
                .eq(Objects.nonNull(isEnabled), AlertRule::getIsEnabled, isEnabled)
                .orderByDesc(AlertRule::getId);
    }
}
