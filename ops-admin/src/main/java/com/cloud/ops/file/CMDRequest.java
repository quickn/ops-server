package com.cloud.ops.file;

import lombok.Data;

/**
 * @author wzh
 * @date 2025/12/23 15:14
 * @description:
 */
@Data
public class CMDRequest {

    private Long agentId;

    private String command;

}
