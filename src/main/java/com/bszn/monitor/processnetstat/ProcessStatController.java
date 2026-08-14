package com.bszn.monitor.processnetstat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 进程统计
 *
 * @author Liuyun
 */
@Tag(name = "进程统计")
@Slf4j
@RestController
@RequestMapping("/processStat")
public class ProcessStatController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private ProcessStatService processNetStatService;

    @GetMapping("/page")
    @Operation(summary = "分页查询进程")
    public Result<Page<ProcessStat>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer serviceId,
            @RequestParam(required = false) String hostname,
            @RequestParam(required = false) String processName,
            @RequestParam(required = false) String containerName,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        Page<ProcessStat> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProcessStat> wrapper = new LambdaQueryWrapper<ProcessStat>()
                .eq(serviceId != null, ProcessStat::getServiceId, serviceId)
                .eq(hostname != null && !hostname.isEmpty(), ProcessStat::getHostname, hostname)
                .eq(processName != null && !processName.isEmpty(), ProcessStat::getProcessName, processName)
                .eq(containerName != null && !containerName.isEmpty(), ProcessStat::getContainerName, containerName)
                .orderByDesc(ProcessStat::getCreateTime);

        // 时间范围筛选：优先使用 startTime/endTime
        if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER).withSecond(59).withNano(999_999_999);
            wrapper.between(ProcessStat::getCreateTime, start, end);
        } else if (date != null && !date.isEmpty()) {
            // 日期筛选：查询某一天的数据
            LocalDate localDate = LocalDate.parse(date);
            LocalDateTime start = localDate.atStartOfDay();
            LocalDateTime end = localDate.atTime(LocalTime.MAX);
            wrapper.between(ProcessStat::getCreateTime, start, end);
        }

        return Result.success(processNetStatService.page(page, wrapper));
    }

    @GetMapping("/listByPid")
    @Operation(summary = "根据PID查询最近N条进程记录")
    public Result<List<ProcessStat>> listByPid(
            @RequestParam Integer pid,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        LambdaQueryWrapper<ProcessStat> wrapper = new LambdaQueryWrapper<ProcessStat>()
                .eq(ProcessStat::getPid, pid)
                .orderByDesc(ProcessStat::getCreateTime)
                .last("LIMIT " + limit);

        if (startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER).withSecond(59).withNano(999_999_999);
            wrapper.between(ProcessStat::getCreateTime, start, end);
        } else if (date != null && !date.isEmpty()) {
            LocalDate localDate = LocalDate.parse(date);
            LocalDateTime start = localDate.atStartOfDay();
            LocalDateTime end = localDate.atTime(LocalTime.MAX);
            wrapper.between(ProcessStat::getCreateTime, start, end);
        }

        List<ProcessStat> list = processNetStatService.list(wrapper);
        return Result.success(list);
    }

}
