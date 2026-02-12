package com.bszn.monitor.docker;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wzh
 * @date 2026/1/21 15:09
 * @description: 容器统计控制器
 */
@Tag(name = "容器统计相关接口")
@Slf4j
@RestController
@RequestMapping("/dockerStats")
@RequiredArgsConstructor
public class DockerStatsController {

    private final IDockerStatsService dockerStatsService;

    @GetMapping("/page")
    @Operation(summary = "获取容器统计列表（分页）")
    public Result<IPage<DockerStats>> page(DockerStatsQueryDto dto) {
        return Result.success(dockerStatsService.page(dto.getPage(), dto.buildLambda()));
    }
}
