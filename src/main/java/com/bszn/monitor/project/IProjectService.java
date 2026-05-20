package com.bszn.monitor.project;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author wzh
 * @date 2026/1/14 13:41
 * @description: 项目接口
 */
public interface IProjectService extends IService<Project> {

    CompletableFuture<Boolean> deploy(DeployRequest deployRequest);

    /**
     * 获取部署记录
     *
     * @param projectId 项目id
     * @return 部署记录
     */
    List<ProjectDeployRecord> getDeployRecords(Long projectId);

    /**
     * 同步
     *
     * @param syncRequest 请求参数
     * @return 结果
     */
    Boolean sync(SyncRequest syncRequest);

    /**
     * 备份
     *
     * @param userId        用户id
     * @param backupRequest 请求参数
     * @return 结果
     */
    Boolean backup(Long userId, BackupRequest backupRequest);
}
