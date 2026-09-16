package com.cloud.ops.processnetstat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.system.common.result.Result;
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

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {

        Page<ProcessStat> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProcessStat> wrapper = new LambdaQueryWrapper<ProcessStat>()
                .eq(serviceId != null, ProcessStat::getServiceId, serviceId)
                .eq(hostname != null && !hostname.isEmpty(), ProcessStat::getHostname, hostname)
                .eq(processName != null && !processName.isEmpty(), ProcessStat::getProcessName, processName)
                .eq(containerName != null && !containerName.isEmpty(), ProcessStat::getContainerName, containerName);

        // 动态排序：指定排序字段时按其排序，否则默认按创建时间倒序
        if (sortField != null && !sortField.isEmpty()) {
            boolean asc = "asc".equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "rsz" -> wrapper.orderBy(true, asc, ProcessStat::getRsz);
                case "pcpu" -> wrapper.orderBy(true, asc, ProcessStat::getPcpu);
                case "pmem" -> wrapper.orderBy(true, asc, ProcessStat::getPmem);
                case "sentRate" -> wrapper.orderBy(true, asc, ProcessStat::getSentRate);
                case "recvRate" -> wrapper.orderBy(true, asc, ProcessStat::getRecvRate);
                case "connTotal" -> wrapper.orderBy(true, asc, ProcessStat::getConnTotal);
                case "connEstablished" -> wrapper.orderBy(true, asc, ProcessStat::getConnEstablished);
                case "pid" -> wrapper.orderBy(true, asc, ProcessStat::getPid);
                default -> wrapper.orderByDesc(ProcessStat::getCreateTime);
            }
            // 指定字段排序时，次排序按创建时间倒序
            wrapper.orderByDesc(ProcessStat::getCreateTime);
        } else {
            wrapper.orderByDesc(ProcessStat::getCreateTime);
        }

        // 时间筛选：日期（date）+ 时分区间（startTime/endTime，格式 HH:mm）
        if (date != null && !date.isEmpty()) {
            LocalDate localDate = LocalDate.parse(date);
            LocalDateTime start = localDate.atStartOfDay();
            LocalDateTime end = localDate.atTime(LocalTime.MAX);
            if (startTime != null && !startTime.isEmpty()) {
                start = localDate.atTime(LocalTime.parse(startTime, TIME_FORMATTER));
            }
            if (endTime != null && !endTime.isEmpty()) {
                end = localDate.atTime(LocalTime.parse(endTime, TIME_FORMATTER)).withSecond(59).withNano(999_999_999);
            }
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

        // 时间筛选：日期（date）+ 时分区间（startTime/endTime，格式 HH:mm）
        if (date != null && !date.isEmpty()) {
            LocalDate localDate = LocalDate.parse(date);
            LocalDateTime start = localDate.atStartOfDay();
            LocalDateTime end = localDate.atTime(LocalTime.MAX);
            if (startTime != null && !startTime.isEmpty()) {
                start = localDate.atTime(LocalTime.parse(startTime, TIME_FORMATTER));
            }
            if (endTime != null && !endTime.isEmpty()) {
                end = localDate.atTime(LocalTime.parse(endTime, TIME_FORMATTER)).withSecond(59).withNano(999_999_999);
            }
            wrapper.between(ProcessStat::getCreateTime, start, end);
        }

        List<ProcessStat> list = processNetStatService.list(wrapper);
        return Result.success(list);
    }

}
