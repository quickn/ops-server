package com.cloud.ops.job;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("job_config")
public class JobConfig {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "任务Id")
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    Integer serviceId;

    @Schema(description = "服务名")
    String serviceName;

    @Schema(description = "指令")
    private String command;

    @Schema(description = "脚本")
    private String script;

    @Schema(description = "类型")
    private String type;

    private Integer timeout;

    @TableField("`interval`")
    private Integer interval;

    private Long agentId;

}
