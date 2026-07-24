package com.bszn.monitor.cmdlog;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author wzh
 * @date 2026/1/21 15:08
 * @description: 指定日志接口
 */
public interface ICmdLogInfoService extends IService<CmdLogInfo> {

    /**
     * 保存
     *
     * @param userId  用户id
     * @param agentId 服务id
     * @param command 指令
     * @return 结果
     */
    Long save(Long userId, Long agentId, String command, String script, String msgType);

    boolean updateResult(Long id, String result, Integer timeConsuming, Boolean isError);

}
