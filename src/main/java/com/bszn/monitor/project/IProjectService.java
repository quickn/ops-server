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

    /**
     * 部署
     *
     * @param projectId 项目id
     * @param agentIds  服务器id
     * @param userId    用户id
     * @return 结果
     */
    CompletableFuture<Boolean> deploy(Long projectId, List<Long> agentIds, Long userId);


    /**
     * 重新部署（只替换JAR包）
     *
     * @param projectId 项目id
     * @param agentIds  服务器id
     * @param userId    用户id
     * @return 结果
     */
    CompletableFuture<Boolean> redeploy(Long projectId, List<Long> agentIds, Long userId);

}
