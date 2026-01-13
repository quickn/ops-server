package com.bszn.monitor.chart;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author wzh
 * @date 2026/1/13 14:11
 * @description: 图表VO
 */
@Data
public class ChartVO {

    @ApiModelProperty("值")
    private String value;

    @ApiModelProperty("时间")
    private String time;

}
