package com.bszn.monitor.warnLog;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@Tag(name = "预警日志")
@RestController
@RequestMapping(value = "/monitor/logInfo")
@Slf4j
public class WarnLogInfoController {
    @Resource
    WarnLogInfoMapper logInfoMapper;

    @ResponseBody
    @GetMapping("/listPage")
    @Operation(summary = "文件日志查询")
    public Result listPage(@ParameterObject WarnLogInfoQuery logInfoQuery) {
        Page<WarnLogInfo> list = logInfoMapper.queryPage(logInfoQuery, logInfoMapper.getPage());
        return Result.success(list);
    }

    @ResponseBody
    @DeleteMapping("/deleteById/{id}")
    @Operation(summary = "删除日志文件")
    public Result deleteById(@PathVariable Long id) {
        logInfoMapper.deleteById(id);
        return Result.success();
    }
}
