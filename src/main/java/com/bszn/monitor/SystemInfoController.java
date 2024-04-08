package com.bszn.monitor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bszn.monitor.system.SystemInfo;
import com.bszn.monitor.system.SystemInfoMapper;
import com.bszn.monitor.system.SystemInfoQuery;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;


@Tag(name = "系统服务")
@RestController
@RequestMapping(value = "/monitor/systemInfo")
@Slf4j
public class SystemInfoController {
    @Resource
    SystemInfoMapper systemInfoMapper;

    @ResponseBody
    @GetMapping("/listPage")
    public Result listPage(@ParameterObject SystemInfoQuery systemInfoQuery) {
        Page<SystemInfo> list = systemInfoMapper.queryPage(systemInfoQuery, systemInfoMapper.getPage());
        return Result.success(list);
    }

    @ResponseBody
    @DeleteMapping("/deleteById/{id}")
    public Result deleteById(@PathVariable Integer id) {
        systemInfoMapper.deleteById(id);
        return Result.success();
    }
}
