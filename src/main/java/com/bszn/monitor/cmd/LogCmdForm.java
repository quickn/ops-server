package com.bszn.monitor.cmd;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Created by Liuyun on 2024-03-27 9:24
 **/
@Data
public class LogCmdForm {
    @ApiModelProperty("服务Id")
    Integer serviceId;

    @ApiModelProperty("容器名称")
    String dockerName;

    @ApiModelProperty("文件路径")
    String filePath;

    @ApiModelProperty("关键字")
    String keyword;

    @ApiModelProperty("关键字1")
    String keyword1;

    @ApiModelProperty("agentId")
    Long agentId;

    String createDate;
    @ApiModelProperty("日志级别")
    @NotBlank(message = "日志级别不能为空")
    String logLevel = "info";

    String cmd;
    String grepPara;

    @ApiModelProperty("超时时间")
    Integer timeout = 60;
}
