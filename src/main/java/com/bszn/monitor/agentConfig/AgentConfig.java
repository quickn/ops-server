package com.bszn.monitor.agentConfig;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代理服务配置实体类
 */
@Data
@TableName("agent_config")
public class AgentConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @Schema(description = "是否存在Nginx(1:是;0:否)")
    private Integer isNginx;

    @Schema(description = "Nginx路径")
    private String nginxPath;

    @Schema(description = "服务id")
    private Integer serviceId;

    @Schema(description = "服务名")
    @NotBlank(message = "服务名不能为空")
    private String serviceName;

    @NotBlank(message = "hostname")
    private String hostname;

    @Schema(description = "工作路径")
    private String workPath;


    @Schema(description = "日志路径")
    private String logPath;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


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
