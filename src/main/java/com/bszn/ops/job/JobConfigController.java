package com.bszn.ops.job;

import com.bszn.system.common.result.Result;
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
        if (temp == null) {
            jobConfigMapper.insert(jobConfig);
        } else {
            jobConfigMapper.updateById(jobConfig);
        }
        return Result.success();
    }

}
