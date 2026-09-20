package com.cloud.ops.cmdlog;

import com.baomidou.mybatisplus.annotation.*;
import com.cloud.ops.base.MonitorBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * @author wzh
 * @date 2026/1/21 15:03
 * @description: 指定发送日志表
 */
@Data
@Builder
@TableName("cmd_log_info")
public class CmdLogInfo extends MonitorBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Integer serviceId;

    private String serviceName;

    @Schema(description = "服务器id")
    private Long agentId;

    @Schema(description = "服务器ip")
    private String agentIp;

    @Schema(description = "指令")
    private String command;

    @Schema(description = "指令")
    private String script;

    @Schema(description = "结果集")
    private String result;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "消息类型")
    private String msgType;

    @Schema(description = "耗时")
    private Integer timeConsuming;

    @Schema(description = "设置超时时间")
    private Integer timeout;

    @Schema(description = "定时任务ID")
    private Long jobId;

    @Schema(description = "是否错误日志")
    private Boolean isSuccess;

    @Schema(description = "备注")
    private String remark;

}


