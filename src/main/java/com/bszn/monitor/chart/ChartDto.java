package com.bszn.monitor.chart;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author wzh
 * @date 2026/1/13 13:27
 * @description: 图表查询 dto
 */
@Data
public class ChartDto {

    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("服务id")
    private Integer serviceId;

}
