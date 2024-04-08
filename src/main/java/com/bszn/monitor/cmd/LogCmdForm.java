package com.bszn.monitor.cmd;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Created by Liuyun on 2024-03-27 9:24
 **/
@Data
public class LogCmdForm {
    Integer serviceId;

    @ApiModelProperty("容器名称")
    @NotNull(message = "容器名称不能为空")
    String dockerName;

    @ApiModelProperty("关键字")
    @NotNull(message = "关键字不能为空")
    String keyword;

    String createDate;
    @ApiModelProperty("日志级别")
    @NotNull(message = "日志级别不能为空")
    String logLevel = "info";

    Long timeout = 9000L;

    String cmd;
    String grepPara;
}
