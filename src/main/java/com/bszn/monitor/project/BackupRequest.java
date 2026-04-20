package com.bszn.monitor.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author wzh
 * @date 2026/4/20
 * @description: 备份恢复请求实体
 */
@Data
public class BackupRequest {

    @NotNull(message = "环境id不能为空")
    @Schema(description = "环境id")
    private Integer serviceId;

    @NotNull(message = "类型不能为空")
    @Schema(description = "类型 1 备份 2 恢复")
    private Integer type;

    @Schema(description = "用户名")
    private String user = "park";

    @Schema(description = "源目录")
    private String sourceDir = "/home/park/docker";

    @Schema(description = "目标目录")
    private String targetDir = "/home/park/docker_bak";
}