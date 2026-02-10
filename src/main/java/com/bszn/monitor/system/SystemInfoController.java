package com.bszn.monitor.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@Tag(name = "系统信息")
@RestController
@RequestMapping(value = "/monitor/systemInfo")
@Slf4j
public class SystemInfoController {
    @Resource
    SystemInfoMapper systemInfoMapper;

    @Resource
    DiskStateMapper diskStateMapper;

    @ResponseBody
    @GetMapping("/listPage")
    @Operation(summary = "系统信息分页")
    public Result<IPage<SystemInfo>> listPage(@ParameterObject SystemInfoQuery systemInfoQuery) {
        Page<SystemInfo> list = systemInfoMapper.queryPage(systemInfoQuery, systemInfoMapper.getPage());
        return Result.success(list);
    }

    @ResponseBody
    @DeleteMapping("/deleteById/{id}")
    @Operation(summary = "删除系统信息")
    public Result deleteById(@PathVariable Integer id) {
        systemInfoMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/diskPage")
    @Operation(summary = "磁盘分页")
    public Result<IPage<DiskState>> diskPage(DiskStateQuery query) {
        return Result.success(diskStateMapper.queryPage(query, diskStateMapper.getPage()));
    }
}
