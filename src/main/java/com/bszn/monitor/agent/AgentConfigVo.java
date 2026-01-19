package com.bszn.monitor.agent;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
public class AgentConfigVo extends ServiceBaseEntity {

    public AgentConfigVo(AgentConfig agentConfig) {
        BeanUtil.copyProperties(agentConfig, this);
    }

    public AgentConfigVo(AgentConfig agentConfig, String status) {
        BeanUtil.copyProperties(agentConfig, this);
        this.status = status;
    }

    @Schema(description = "服务器id")
    private Long id;

    @Schema(description = "主机ip")
    private String hostname;

    @Schema(description = "是否跳板机")
    private Boolean isJumpServer;

    @Schema(description = "是否是源服务")
    private Boolean isOriginServer;

    @Schema(description = "是否在线")
    private Boolean online;

    @Schema(description = "部署状态 空 未部署 有值 已部署")
    private String status;

}
