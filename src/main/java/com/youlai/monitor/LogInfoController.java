package com.youlai.monitor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.monitor.log.LogInfo;
import com.youlai.monitor.log.LogInfoMapper;
import com.youlai.monitor.log.LogInfoQuery;
import com.youlai.system.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@Tag(name = "监控日志")
@RestController
@RequestMapping(value = "/monitor/logInfo")
@Slf4j
public class LogInfoController {
    @Resource
    LogInfoMapper logInfoMapper;

    @ResponseBody
    @GetMapping("/listPage")
    public Result listPage(@ParameterObject LogInfoQuery logInfoQuery) {
        Page<LogInfo> list = logInfoMapper.queryPage(logInfoQuery, logInfoMapper.getPage());
        return Result.success(list);
    }

    @ResponseBody
    @DeleteMapping("/deleteById/{id}")
    public Result deleteById(@PathVariable Long id) {
        logInfoMapper.deleteById(id);
        return Result.success();
    }
}
