package com.cloud.ops.chart;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author wzh
 * @date 2026/1/14 13:16
 * @description: 图表业务对象
 */
@Data
public class ChartBO {

    @ApiModelProperty("主机地址")
    private String hostname;

    @ApiModelProperty("值")
    private String value;

    @ApiModelProperty("时间")
    private String time;

}
