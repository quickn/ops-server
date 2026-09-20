package com.cloud.ops.agentConfig;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.ops.base.MonitorBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 代理服务配置实体类
 */
@Data
@TableName("agent_config")
public class AgentConfig extends MonitorBaseEntity implements Serializable {

    @Schema(description = "是否存在Nginx(1:是;0:否)")
    private Integer isNginx;

    @Schema(description = "Nginx路径")
    private String nginxPath;

    @NotBlank(message = "hostname")
    private String hostname;

    @Schema(description = "工作路径")
    private String workPath;


    @Schema(description = "日志路径")
    private String logPath;


    public void setWorkPath(String workPath) {
        this.workPath = workPath != null ? workPath.trim() : null;
    }


    public void setLogPath(String logPath) {
        this.logPath = logPath != null ? logPath.trim() : null;
    }

    public void setNginxPath(String nginxPath) {
        this.nginxPath = nginxPath != null ? nginxPath.trim() : null;
    }
}
