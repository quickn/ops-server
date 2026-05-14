package com.bszn.monitor.agent;

import cn.hutool.core.bean.BeanUtil;
import com.bszn.base.ServiceBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Liuyun on 2023-07-28 11:16
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentVo extends ServiceBaseEntity {

    public AgentVo(Agent agentConfig) {
        BeanUtil.copyProperties(agentConfig, this);
    }

    @Schema(description = "服务器id")
    private Long id;

    @Schema(description = "主机ip")
    private String hostname;

    @ApiModelProperty("远程ip")
    private String remoteIp;

    @Schema(description = "是否跳板机")
    private Boolean isJumpServer;

    @Schema(description = "是否是源服务")
    private Boolean isOriginServer;

    @Schema(description = "是否在线")
    private Boolean online;

    @Schema(description = "容器名称")
    private String dockerName;

    @Schema(description = "容器状态")
    private String status;

}
