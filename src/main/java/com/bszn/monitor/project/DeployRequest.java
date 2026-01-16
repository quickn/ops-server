package com.bszn.monitor.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @author wzh
 * @date 2026/1/15 13:08
 * @description: 部署请求实体
 */
@Data
public class DeployRequest {

    @NotEmpty(message = "服务器id不能为空")
    @Schema(description = "服务器id")
    private List<Long> agentIds;

}
