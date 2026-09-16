package com.cloud.ops.chart;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author wzh
 * @date 2026/1/14 15:53
 * @description:
 */
@Data
@Builder
public class ChartIpVO {

    @ApiModelProperty("服务器地址")
    private String ip;

    @ApiModelProperty("图表列表")
    private List<ChartVO> list;

}
