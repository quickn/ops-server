package com.cloud.receiver.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Created by Liuyun on 2023-07-28 11:16
 **/

@Data
public class Agent extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Boolean isDocker;
    private Boolean isDockerStats;
    private String hostname;
    private String mac;
    private Double cpuWarnVal;
    private Double memWarnVal;
    private Double diskWarnVal;

    @ApiModelProperty("docker服务Cpu阈值")
    private Double cpuWarnValDocker;

    @ApiModelProperty("是否开启流量监控")
    private Boolean isNetwork;

    @ApiModelProperty("流量监控参数")
    private String networkParam;

    @ApiModelProperty("是否发送邮件提醒")
    private Boolean isMail;

    @ApiModelProperty("是否监控")
    private Boolean isMonitor;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @TableField(exist = false)
    private boolean isUpdate;

    @ApiModelProperty("进程流量发送接受阈值")
    private Integer netThresholdMbps;

    @ApiModelProperty("终端版本")
    private String clientVersion;

    @ApiModelProperty("是否在线")
    private Boolean online;


}
