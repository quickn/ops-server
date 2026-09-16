package com.cloud.receiver.sql;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 自定义查询语句
 * Created by Liuyun on 2023-06-19 11:43
 **/
@Data
public class QueryParam {

    @ApiModelProperty("自定义sql")
    private String selectSql;

    @ApiModelProperty("是否有多个参数")
    private boolean isMultiplePara = true;
}
