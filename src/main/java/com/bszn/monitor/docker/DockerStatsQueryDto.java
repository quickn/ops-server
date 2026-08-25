package com.bszn.monitor.docker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bszn.base.sql.IQuery;
import com.bszn.base.sql.PageForm;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author wzh
 * @date 2026/1/21 15:10
 * @description: 指令查询dto
 */
@Data
public class DockerStatsQueryDto extends PageForm<DockerStats> implements IQuery {

    @Schema(description = "环境id")
    private Long serviceId;

    @Schema(description = "服务id")
    private Long agentId;

    @Schema(description = "名称")
    private String names;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "排序字段")
    private String sortField;

    @Schema(description = "排序方向：asc/desc")
    private String sortOrder;


    @Override
    public LambdaQueryWrapper<DockerStats> buildLambda() {
        LambdaQueryWrapper<DockerStats> wrapper = super.buildLambda()
                .eq(Objects.nonNull(serviceId), DockerStats::getServiceId, serviceId)
                .eq(Objects.nonNull(agentId), DockerStats::getAgentId, agentId)
                .eq(StringUtils.isNotEmpty(names), DockerStats::getNames, names)
                .lt(endTime != null, DockerStats::getCreateTime, endTime)
                .gt(startTime != null, DockerStats::getCreateTime, startTime);

        // 动态排序：指定排序字段时按其排序，否则默认按 id 倒序
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if (StringUtils.isNotEmpty(sortField)) {
            switch (sortField) {
                case "cpu" -> wrapper.orderBy(true, asc, DockerStats::getCpu);
                case "mem" -> wrapper.orderBy(true, asc, DockerStats::getMem);
                default -> wrapper.orderByDesc(DockerStats::getId);
            }
        }
        return wrapper.orderByDesc(DockerStats::getId);
    }
}
