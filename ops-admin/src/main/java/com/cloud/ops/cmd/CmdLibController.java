package com.cloud.ops.cmd;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指令库管理
 *
 * @author Liuyun
 */
@Tag(name = "指令库管理")
@Slf4j
@RestController
@RequestMapping("/cmdLib")
public class CmdLibController {

    @Resource
    private CmdLibService cmdLibService;

    @GetMapping("/list")
    @Operation(summary = "查询指令列表")
    public Result<List<CmdLib>> list() {
        List<CmdLib> list = cmdLibService.list(
                new LambdaQueryWrapper<CmdLib>()
                        .orderByDesc(CmdLib::getUpdateTime)
        );
        return Result.success(list);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询指令")
    public Result<Page<CmdLib>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(required = false) String keyword) {
        Page<CmdLib> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CmdLib> wrapper = new LambdaQueryWrapper<CmdLib>()
                .eq(type != null && !type.isEmpty(), CmdLib::getType, type)
                .and(keyword != null && !keyword.isEmpty(),
                        w -> w.like(CmdLib::getCmd, keyword))
                .orderByDesc(CmdLib::getUpdateTime);
        return Result.success(cmdLibService.page(page, wrapper));
    }

    @PostMapping("/save")
    @Operation(summary = "新增指令")
    public Result save(@RequestBody CmdLib cmdLib) {
        cmdLib.setUpdateTime(LocalDateTime.now());
        cmdLibService.save(cmdLib);
        return Result.success();
    }

    @PutMapping("/update")
    @Operation(summary = "修改指令")
    public Result update(@RequestBody CmdLib cmdLib) {
        cmdLib.setUpdateTime(LocalDateTime.now());
        cmdLibService.updateById(cmdLib);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除指令")
    public Result delete(@PathVariable Integer id) {
        cmdLibService.removeById(id);
        return Result.success();
    }

    @DeleteMapping("/batchDelete")
    @Operation(summary = "批量删除指令")
    public Result batchDelete(@RequestBody List<Integer> ids) {
        cmdLibService.removeBatchByIds(ids);
        return Result.success();
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "查询指令详情")
    public Result<CmdLib> detail(@PathVariable Integer id) {
        return Result.success(cmdLibService.getById(id));
    }

}
