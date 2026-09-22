package com.cloud.ops.taskAlert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.ops.base.MonitorBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 任务告警配置
 *
 * <p> 计划任务：定义一个可动态添加需要在指定周期执行的计划任务，
 * 并对其执行结果进行监控，任务失败时触发告警。</p>
 *
 * @author Liuyun
 */
@Data
@TableName("task_alert")
public class TaskAlert extends MonitorBaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务类型: shell脚本 / 接口检测 / 数据库备份 等")
    private String taskType;

    @Schema(description = "脚本内容")
    private String script;

    @Schema(description = "cron 表达式")
    private String cron;

    @Schema(description = "关联主机Id")
    private Long agentId;

    @Schema(description = "是否启用告警")
    private Boolean isEnabled;

    @Schema(description = "状态: 1-启动 0-停止")
    private Integer status;

    @Schema(description = "是否发送邮件告警")
    private Boolean isEmail;

    @Schema(description = "连续失败次数阈值，超过则告警")
    private Integer failThreshold;

    @Schema(description = "备注")
    private String remark;

}
