package com.bszn.monitor.agent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.bszn.base.ServiceBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Created by Liuyun on 2023-07-28 11:16
 **/

@Data
public class AgentConfig extends ServiceBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "是否docker")
    private Boolean isDocker;

    @Schema(description = "主机ip")
    private String hostname;

    @Schema(description = "mac地址")
    private String mac;

    @Schema(description = "cpu阈值")
    private Double cpuWarnVal;

    @Schema(description = "内存阈值")
    private Double memWarnVal;

    @Schema(description = "硬盘阈值")
    private Double diskWarnVal;

    @Schema(description = "docker服务Cpu阈值")
    private Double cpuWarnValDocker;

    @Schema(description = "是否发送邮件提醒")
    private Boolean isMail;

    @Schema(description = "是否监控")
    private Boolean isMonitor;

    @Schema(description = "终端版本")
    private String clientVersion;

    @Schema(description = "远程ip")
    private String remoteIp;

    @Schema(description = "是否跳板机")
    private Boolean isJumpServer;

    @Schema(description = "是否是源服务")
    private Boolean isOriginServer;

    @Schema(description = "是否在线")
    private Boolean online;

}
