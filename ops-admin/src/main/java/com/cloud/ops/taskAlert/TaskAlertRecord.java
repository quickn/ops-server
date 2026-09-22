package com.cloud.ops.taskAlert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.ops.base.MonitorBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 任务告警执行记录
 *
 * @author Liuyun
 */
@Data
@TableName("task_alert_record")
public class TaskAlertRecord extends MonitorBaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "任务告警Id")
    private Long taskId;

    @Schema(description = "执行状态: 0成功 1失败")
    private Integer state;

    @Schema(description = "执行结果")
    private String result;

    @Schema(description = "耗时(毫秒)")
    private Long timeConsuming;

    @Schema(description = "是否已发送告警")
    private Boolean isAlert;

}
