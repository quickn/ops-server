package com.bszn.monitor.processnetstat;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bszn.base.ServiceBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 进程统计实体
 *
 * @author Liuyun
 */
@Data
@TableName("process_stat")
public class ProcessStat extends ServiceBaseEntity {

    @Schema(description = "进程编号")
    private Integer pid;

    @Schema(description = "进程名")
    private String processName;

    @Schema(description = "容器名称")
    private String containerName;

    @Schema(description = "物理内存占用(KB)")
    private Integer rsz;

    @Schema(description = "CPU占用百分比")
    private BigDecimal pcpu;

    @Schema(description = "内存占用百分比")
    private BigDecimal pmem;

    @Schema(description = "用户")
    private String user;

    @Schema(description = "主机IP")
    private String hostname;

    @Schema(description = "默认网卡发送流量(KB)")
    private Double sentRate;

    @Schema(description = "默认网卡接收流量(KB)")
    private Double recvRate;

    @Schema(description = "TCP 连接总数")
    private Integer connTotal;

    @Schema(description = "ESTABLISHED 状态连接数")
    private Integer connEstablished;

    @Schema(description = "CLOSE_WAIT 状态连接数")
    private Integer connCloseWait;

    @Schema(description = "TIME_WAIT 状态连接数")
    private Integer connTimeWait;

    @TableField(exist = false)
    private LocalDateTime updateTime;

}
