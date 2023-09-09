package com.youlai.agent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.youlai.base.ServiceBaseEntity;
import com.youlai.system.common.base.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by Liuyun on 2023-07-28 11:16
 **/

@Data
public class AgentConfig extends ServiceBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private boolean isDocker;
    private String hostname;
    private String mac;
    private Double cpuWarnVal;
    private Double memWarnVal;
    private Double diskWarnVal;

    @ApiModelProperty("docker服务Cpu阈值")
    private Double cpuWarnValDocker;

    @ApiModelProperty("是否发送邮件提醒")
    private boolean isMail;

    @ApiModelProperty("是否监控")
    private Boolean isMonitor;
}
