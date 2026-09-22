package com.cloud.ops.cmd;

import com.baomidou.mybatisplus.annotation.*;
import com.cloud.ops.base.MultiTenantEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 指令库实体
 *
 * @author Liuyun
 */
@Data
@TableName("cmd_lib")
public class CmdLib implements Serializable, MultiTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Integer id;


    @Schema(description = "指令名称")
    private String cmdName;

    @Schema(description = "指令")
    private String cmd;


    @Schema(description = "类型:shell python")
    private String type;

    /**
     * 创建人ID(用于多租户数据隔离)
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;


    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
