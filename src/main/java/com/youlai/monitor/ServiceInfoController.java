package com.youlai.monitor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.monitor.service.ServiceInfo;
import com.youlai.monitor.service.ServiceInfoMapper;
import com.youlai.monitor.service.ServiceInfoQuery;
import com.youlai.system.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "服务")
@RestController
@RequestMapping(value = "/serviceInfo")
@Slf4j
public class ServiceInfoController {
    @Resource
    ServiceInfoMapper serviceInfoMapper;


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

}
