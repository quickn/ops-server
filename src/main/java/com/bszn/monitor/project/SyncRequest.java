package com.bszn.monitor.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author wzh
 * @date 2026/1/15 13:08
 * @description: 同步请求实体
 */
@Data
public class SyncRequest {

    @Schema(description = "源服务器id")
    private Long sourceAgentId;

    @Schema(description = "跳板机ip")
    private String remoteIp;

    @Schema(description = "跳板机id")
    private Long agentId;

    @Schema(description = "jar包下载地址")
    private String jarDownloadUrl;

    @NotNull(message = "类型不能为空")
    @Schema(description = "类型 1 源服务器 2 jar包")
    private Integer type;

    @NotEmpty(message = "目标服务器ids不能为空")
    @Schema(description = "目标服务器ids")
    private List<Long> targetAgentIds;

    @Schema(description = "用户名")
    private String user = "park";

}
