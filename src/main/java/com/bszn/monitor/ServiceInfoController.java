package com.bszn.monitor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bszn.monitor.service.ServiceInfo;
import com.bszn.monitor.service.ServiceInfoMapper;
import com.bszn.monitor.service.ServiceInfoQuery;
import com.bszn.monitor.service.ServiceInfoService;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "服务")
@RestController
@RequestMapping(value = "/serviceInfo")
@Slf4j
public class ServiceInfoController {
    @Resource
    ServiceInfoMapper serviceInfoMapper;

    @Resource
    ServiceInfoService serviceInfoService;

    @ResponseBody
    @GetMapping("/listAll")
    public Result listAll() {
        List<ServiceInfo> list = serviceInfoMapper.query(new ServiceInfoQuery());
        return Result.success(list);
    }

    @ResponseBody
    @GetMapping("/listPage")
    public Result listPage(@ParameterObject ServiceInfoQuery systemInfoQuery) {
        Page<ServiceInfo> list = serviceInfoMapper.queryPage(systemInfoQuery, serviceInfoMapper.getPage());
        return Result.success(list);
    }

    @ResponseBody
    @PostMapping("/save")
    public Result save(@RequestBody ServiceInfo serviceInfo) {
        serviceInfoService.saveOrUpdate(serviceInfo);
        return Result.success();
    }


    @DeleteMapping("/delete/{ids}")
    @ResponseBody
    public Result delete(@PathVariable Integer ids) {
        serviceInfoService.removeById(ids);
        return Result.success();
    }

}
