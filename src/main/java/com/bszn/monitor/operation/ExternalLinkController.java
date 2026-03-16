package com.bszn.monitor.operation;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@Tag(name = "外链")
@Slf4j
@RestController
@RequestMapping("/externalLink")
@RequiredArgsConstructor
public class ExternalLinkController {

    private final ExternalLinkService externalLinkService;

    @GetMapping("/page")
    @Operation(summary = "外链分页）")
    public Result<IPage<ExternalLink>> page(ExternalLinkQueryDto dto) {
        return Result.success(externalLinkService.page(dto.getPage(), dto.buildLambda()));
    }

    @Operation(summary = "保存外链")
    @PostMapping(value = "/save")
    public Result<Boolean> saveHeathMonitor(@RequestBody ExternalLink externalLink) {
        return Result.success(externalLinkService.saveOrUpdate(externalLink));
    }

    @Operation(summary = "删除外链")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        externalLinkService.removeById(id);
        return Result.success();
    }

}
