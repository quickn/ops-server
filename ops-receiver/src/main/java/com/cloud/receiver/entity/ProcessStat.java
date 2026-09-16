package com.cloud.receiver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 进程统计
 * 对应 ops-agent-go 的 ProcessNetStat 结构体
 * Created by Liuyun on 2024-01-04 11:12
 **/
@Data
public class ProcessStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("进程id")
    private Integer pid;

    @ApiModelProperty("进程名")
    private String processName;

    @ApiModelProperty("容器名称")
    private String containerName;

    @ApiModelProperty("用户")
    private String user;


    @ApiModelProperty("主机IP")
    private String hostname;

    @ApiModelProperty("默认网卡发送流量(KB)")
    private Double sentRate;

    @ApiModelProperty("默认网卡接收流量(KB)")
    private Double recvRate;


    @ApiModelProperty("所有非lo网卡发送流量(KB)")
    private Double totalRecvRate;

    @ApiModelProperty("所有非lo网卡接收流量(KB)")
    private Double totalSentRate;

    @ApiModelProperty("服务/环境ID")
    private Integer serviceId;

    @ApiModelProperty("服务/环境名称")
    private String serviceName;

    private Date createTime;
}
