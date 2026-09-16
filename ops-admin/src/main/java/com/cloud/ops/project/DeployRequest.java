package com.cloud.ops.project;

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

    @Schema(description = "项目id")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;


    @NotEmpty(message = "服务器id不能为空")
    @Schema(description = "服务器id")
    private List<Long> agentIds;

    @Schema(description = "部署方式 1:重新构建 2:直接替换jar")
    private Integer deployType = 1;

    @Schema(description = "是否同步部署 true:同步 false:异步 ")
    private Boolean sync = true;

    @Schema(description = "时间间隔(秒)")
    private Integer interval = 0;

}
