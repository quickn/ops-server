package com.cloud.ops.taskAlert;

import com.cloud.base.sql.IQuery;
import com.cloud.base.sql.PageForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Objects;

/**
 * @author Liuyun
 */
@Data
public class TaskAlertQueryDto extends PageForm<TaskAlert> implements IQuery {

    @Schema(description = "环境id")
    private Long serviceId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "是否启用")
    private Boolean isEnabled;

    @Schema(description = "状态: 1-启动 0-停止")
    private Integer status;

    @Override
    public com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskAlert> buildLambda() {
        return super.buildLambda()
                .eq(Objects.nonNull(serviceId), TaskAlert::getServiceId, serviceId)
                .like(Objects.nonNull(taskName) && !taskName.isEmpty(), TaskAlert::getTaskName, taskName)
                .eq(Objects.nonNull(taskType) && !taskType.isEmpty(), TaskAlert::getTaskType, taskType)
                .eq(Objects.nonNull(isEnabled), TaskAlert::getIsEnabled, isEnabled)
                .eq(Objects.nonNull(status), TaskAlert::getStatus, status)
                .orderByDesc(TaskAlert::getId);
    }

}
