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
public class AlertRecordQueryDto extends PageForm<AlertRecord> implements IQuery {

    @Schema(description = "环境id")
    private Long serviceId;

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "预警通道")
    private String alertChannel;

    @Schema(description = "发送状态: 0-成功 1-失败")
    private Integer sendStatus;

    @Schema(description = "是否被静默")
    private Integer isSilenced;

    @Override
    public com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AlertRecord> buildLambda() {
        return super.buildLambda()
                .eq(Objects.nonNull(ruleId), AlertRecord::getRuleId, ruleId)
                .eq(Objects.nonNull(alertChannel) && !alertChannel.isEmpty(), AlertRecord::getAlertChannel, alertChannel)
                .eq(Objects.nonNull(sendStatus), AlertRecord::getSendStatus, sendStatus)
                .eq(Objects.nonNull(isSilenced), AlertRecord::getIsSilenced, isSilenced)
                .orderByDesc(AlertRecord::getId);
    }
}
