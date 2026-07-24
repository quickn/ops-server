package com.bszn.monitor.cmdlog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @author wzh
 * @date 2026/1/21 15:03
 * @description: 指定发送日志表
 */
@Data
@Builder
@TableName("cmd_log_info")
public class CmdLogInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "环境id")
    private Integer serviceId;

    @Schema(description = "环境")
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

    @Schema(description = "是否错误日志")
    private Boolean isError;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}


