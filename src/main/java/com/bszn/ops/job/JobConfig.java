package com.bszn.ops.job;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("job_config")
public class JobConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    Integer serviceId;

    Integer jobId;

    @Schema(description = "CMD")
    private String cmd;

    @Schema(description = "类型")
    private String type;

    private Integer timeout;

}
