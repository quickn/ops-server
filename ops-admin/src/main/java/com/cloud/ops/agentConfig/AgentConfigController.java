package com.cloud.ops.agentConfig;

import com.cloud.system.common.annotation.PreventDuplicateSubmit;
import com.cloud.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代理服务配置控制器
 */
@Tag(name = "代理服务配置管理")
@RestController
@RequestMapping("/agent-config")
@RequiredArgsConstructor
public class AgentConfigController {

    private final AgentConfigService configService;

    @Operation(summary = "获取代理服务配置列表", security = {@SecurityRequirement(name = "Authorization")})
    @GetMapping
    public Result<List<AgentConfigVO>> listConfigs(
            @ParameterObject AgentConfigQuery queryParams
    ) {
        List<AgentConfigVO> list = configService.listConfigs(queryParams);
        return Result.success(list);
    }

    @Operation(summary = "获取代理服务配置表单数据", security = {@SecurityRequirement(name = "Authorization")})
    @GetMapping("/{id}/form")
    public Result<AgentConfig> getConfigForm(
            @Parameter(description = "配置ID") @PathVariable Integer id
    ) {
        AgentConfig agentConfig = configService.getById(id);
        return Result.success(agentConfig);
    }

    @Operation(summary = "新增代理服务配置", security = {@SecurityRequirement(name = "Authorization")})
    @PostMapping
    @PreventDuplicateSubmit
    public Result saveConfig(
            @Valid @RequestBody AgentConfig formData
    ) {
        configService.saveOrUpdate(formData);
        return Result.success();
    }

    @Operation(summary = "删除代理服务配置", security = {@SecurityRequirement(name = "Authorization")})
    @DeleteMapping("/{ids}")
    public Result deleteConfigs(
            @Parameter(description = "配置ID，多个以英文逗号(,)分割") @PathVariable("ids") String ids
    ) {
        boolean result = configService.deleteByIds(ids);
        return Result.judge(result);
    }

}
