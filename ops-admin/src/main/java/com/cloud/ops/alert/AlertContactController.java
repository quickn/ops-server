package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloud.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报警联系人管理
 *
 * @author Liuyun
 */
@Tag(name = "报警联系人")
@Slf4j
@RestController
@RequestMapping("/alertContact")
@RequiredArgsConstructor
public class AlertContactController {

    private final AlertContactService alertContactService;

    @GetMapping("/page")
    @Operation(summary = "联系人列表（分页）")
    public Result<IPage<AlertContact>> page(AlertContactQueryDto dto) {
        return Result.success(alertContactService.page(dto));
    }

    @GetMapping("/list")
    @Operation(summary = "联系人列表（全部）")
    public Result<List<AlertContact>> list() {
        return Result.success(alertContactService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "联系人详情")
    public Result<AlertContact> get(@PathVariable Long id) {
        return Result.success(alertContactService.getById(id));
    }

    @Operation(summary = "保存联系人")
    @PostMapping("/save")
    public Result<Boolean> save(@RequestBody AlertContact alertContact) {
        return Result.success(alertContactService.saveOrUpdate(alertContact));
    }

    @Operation(summary = "删除联系人")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        alertContactService.removeById(id);
        return Result.success();
    }
}
