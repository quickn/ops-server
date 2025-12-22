package com.bszn.monitor.cmd;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Created by Liuyun on 2024-03-27 9:24
 **/
@Data
public class LogCmdForm {
    Integer serviceId;

    @ApiModelProperty("容器名称")
    @NotBlank(message = "容器名称不能为空")
    String dockerName;

    @ApiModelProperty("关键字")
    @NotBlank(message = "关键字不能为空")
    String keyword;

    String createDate;
    @ApiModelProperty("日志级别")
    @NotBlank(message = "日志级别不能为空")
    String logLevel = "info";

    String cmd;
    String grepPara;
}
