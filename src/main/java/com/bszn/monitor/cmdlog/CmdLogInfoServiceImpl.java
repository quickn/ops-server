package com.bszn.monitor.cmdlog;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bszn.monitor.agent.Agent;
import com.bszn.monitor.agent.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author wzh
 * @date 2026/1/21 15:08
 * @description: 指令日志业务层
 */
@Service
@RequiredArgsConstructor
public class CmdLogInfoServiceImpl extends ServiceImpl<CmdLogInfoMapper, CmdLogInfo> implements ICmdLogInfoService {

    private final AgentService agentConfigService;

    /**
     * 保存
     *
     * @param userId  用户id
     * @param agentId 服务id
     * @param cmd     指令
     * @param msgType 消息类型
     */
    @Override
    public Long save(Long userId, Long agentId, String cmd, String msgType) {
        // 拿到服务
        Agent byId = agentConfigService.getById(agentId);
        CmdLogInfo cmdLogInfo = CmdLogInfo.builder()
                .serviceId(byId.getServiceId())
                .serviceName(byId.getServiceName())
                .agentId(agentId)
                .agentIp(byId.getHostname())
                .cmd(cmd)
                .msgType(msgType)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .build();
        save(cmdLogInfo);
        return cmdLogInfo.getId();
    }

    @Override
    public boolean updateResult(Long id, String result, Integer timeConsuming, Boolean isError) {
        if (result != null && result.length() >= 2000) {
            result = result.substring(0, 2000);
        }
        updateById(CmdLogInfo.builder()
                .id(id)
                .result(result)
                .isError(isError)
                .timeConsuming(timeConsuming)
                .build());
        return true;
    }
}
