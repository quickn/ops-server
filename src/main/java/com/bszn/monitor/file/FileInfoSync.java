package com.bszn.monitor.file;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author wzh
 * @date 2025/12/23 15:01
 * @description:
 */
@Data
public class FileInfoSync {

    @ApiModelProperty("服务器id")
    Long agentId;
    @ApiModelProperty("命令")
    String command;
}
