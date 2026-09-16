package com.cloud.ops.job;

import com.cloud.ops.service.ServiceInfo;
import com.cloud.ops.service.ServiceInfoService;
import com.cloud.system.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "任务配置")
@Slf4j
@RestController
@RequestMapping("/jobConfig")
public class JobConfigController {
    @Resource
    JobConfigMapper jobConfigMapper;
    @Resource
    ServiceInfoService serviceInfoService;

    @GetMapping("/get/{jobId}")
    public Result get(@PathVariable Long jobId) {
        return Result.success(jobConfigMapper.selectById(jobId));
    }

    @DeleteMapping("/delete/{jobId}")
    public Result delete(@PathVariable Long jobId) {
        return Result.success(jobConfigMapper.deleteById(jobId));
    }

    @PostMapping("/save")
    public Result save(@RequestBody JobConfig jobConfig) {
        JobConfig temp = jobConfigMapper.selectById(jobConfig.getId());
        if (jobConfig.getServiceId() != null) {
            ServiceInfo serviceInfo = serviceInfoService.getById(jobConfig.getServiceId());
            jobConfig.setServiceName(serviceInfo.getName());
        }
        if (temp == null) {
            jobConfigMapper.insert(jobConfig);
        } else {
            jobConfigMapper.updateById(jobConfig);
        }
        return Result.success();
    }

}
