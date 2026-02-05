package com.bszn.monitor.docker;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bszn.base.ServiceBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Created by Liuyun on 2023-07-27 15:16
 **/
@Data
@TableName("docker_stats")
public class DockerStats extends ServiceBaseEntity {

    @Schema(description = "名称")
    private String names;

    @Schema(description = "服务器id")
    private Long agentId;

    @Schema(description = "服务器地址")
    private String hostname;

    @Schema(description = "cpu使用率")
    private Double cpu;

    @Schema(description = "内存使用率")
    private Double mem;

    @Schema(description = "内存使用及限制")
    private String memUsage;

    @Schema(description = "磁盘进出量")
    private String blockIo;

    @Schema(description = "网络进出量")
    private String netIo;

    @TableField(exist = false)
    private LocalDateTime updateTime;

}
