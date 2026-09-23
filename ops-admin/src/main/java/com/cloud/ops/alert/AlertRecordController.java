package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 预警记录查询
 *
 * @author Liuyun
 */
@Tag(name = "预警记录")
@Slf4j
@RestController
@RequestMapping("/alertRecord")
@RequiredArgsConstructor
public class AlertRecordController {

    private final AlertRecordService alertRecordService;

    @GetMapping("/page")
    @Operation(summary = "预警记录列表（分页）")
    public Result<IPage<AlertRecord>> page(AlertRecordQueryDto dto) {
        return Result.success(alertRecordService.page(dto));
    }
}
