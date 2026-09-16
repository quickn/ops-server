package com.cloud.ops.agentConfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 代理服务配置查询对象
 */
@Schema(description = "代理服务配置查询对象")
@Data
public class AgentConfigQuery {

    @Schema(description = "关键字(服务名)")
    private String keywords;

    @Schema(description = "是否存在Nginx(1:是;0:否)")
    private Integer isNginx;

    @Schema(description = "服务id")
    private Integer serviceId;

}
