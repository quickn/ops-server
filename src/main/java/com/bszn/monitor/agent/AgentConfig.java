package com.bszn.monitor.agent;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.bszn.base.ServiceBaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Created by Liuyun on 2023-07-28 11:16
 **/

@Data
public class AgentConfig extends ServiceBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Boolean isDocker;
    private String hostname;
    private String mac;
    private Double cpuWarnVal;
    private Double memWarnVal;
    private Double diskWarnVal;

    @ApiModelProperty("docker服务Cpu阈值")
    private Double cpuWarnValDocker;

    @ApiModelProperty("是否发送邮件提醒")
    private Boolean isMail;

    @ApiModelProperty("是否监控")
    private Boolean isMonitor;

    @ApiModelProperty("终端版本")
    private String clientVersion;

    @TableField(fill = FieldFill.UPDATE)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
