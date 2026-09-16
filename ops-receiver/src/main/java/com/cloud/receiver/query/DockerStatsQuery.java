package com.cloud.receiver.query;

import com.cloud.receiver.sql.IQuery;
import com.cloud.receiver.sql.annotation.OrderBy;
import com.cloud.receiver.sql.annotation.SelectSql;
import com.cloud.receiver.sql.annotation.Where;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Created by Liuyun on 2023-08-26 16:22
 **/
@Data
@SelectSql(" * from docker_stats ")
@OrderBy(" id ")
public class DockerStatsQuery implements IQuery {

    @Where()
    private Long containerId;

    @ApiModelProperty(value = "开始时间", example = "2021-11-01 00:00")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Where(columnName = "create_time")
    private Date startTime;

    @ApiModelProperty(value = "结束时间", example = "2021-12-01 00:00")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Where(columnName = "create_time")
    private Date endTime;

}
