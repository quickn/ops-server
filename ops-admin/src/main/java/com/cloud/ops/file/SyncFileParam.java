package com.cloud.ops.file;

import lombok.Data;

/**
 * 文件同步参数（通过跳板机同步文件）
 */
@Data
public class SyncFileParam {

    /**
     * 源服务器 agent ID（type=1 时，rsync 从源服务器同步到跳板机）
     */
    private Long sourceAgentId;

    /**
     * 跳板机 agent ID（执行跳板机→目标服务器的 rsync 命令）
     */
    private Long jumpAgentId;

    /**
     * 跳板机服务 ID
     */
    private Integer jumpServiceId;

    /**
     * 源文件完整路径，如 /home/park/docker/projectName
     */
    private String sourcePath;


    private String targetPath;


    /**
     * 同步类型：1=从源服务器 rsync 到跳板机，2=jar包下载 + rsync，3=仅从跳板机 rsync
     */
    private Integer syncType;


    private String remark;

    private Integer timeout = 300;

}
