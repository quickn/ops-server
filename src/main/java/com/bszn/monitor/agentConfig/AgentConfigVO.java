package com.bszn.monitor.agentConfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 代理服务配置视图对象
 */
@Schema(description = "代理服务配置视图对象")
@Data
public class AgentConfigVO {

    @Schema(description = "主键ID")
    private Integer id;

    @Schema(description = "是否存在Nginx(1:是;0:否)")
    private Integer isNginx;

    @Schema(description = "Nginx路径")
    private String nginxPath;

    @Schema(description = "服务id")
    private Integer serviceId;

    @Schema(description = "服务名")
    private String serviceName;

    @NotBlank(message = "hostname")
    private String hostname;

    @Schema(description = "工作路径")
    private String workPath;

    @Schema(description = "日志路径")
    private String logPath;

}
