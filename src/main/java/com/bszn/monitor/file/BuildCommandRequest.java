package com.bszn.monitor.file;

import lombok.Data;

import java.util.List;

/**
 * @author wzh
 * @date 2025/12/23 15:14
 * @description:
 */
@Data
public class BuildCommandRequest {

    private List<Long> targetServers;
    private String sourcePath;
    private String targetPath = "/home/park";
}
