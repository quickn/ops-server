package com.bszn.monitor.cmdlog;

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
 * @description: 指定日志控制器
 */
@Tag(name = "指令日志相关接口")
@Slf4j
@RestController
@RequestMapping("/cmdLog")
@RequiredArgsConstructor
public class CmdLogInfoController {

    private final ICmdLogInfoService cmdLogInfoService;

    @GetMapping("/page")
    @Operation(summary = "获取指令列表（分页）")
    public Result<IPage<CmdLogInfo>> page(CmdLogInfoQueryDto dto) {
        return Result.success(cmdLogInfoService.page(dto.getPage(), dto.buildLambda()));
    }
}
