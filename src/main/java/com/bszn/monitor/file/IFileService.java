package com.bszn.monitor.file;

public interface IFileService {

    /**
     * 通过跳板机同步文件
     */
    Boolean syncFileByJumpServer(SyncFileParam syncFileParam);
}
