package com.bszn.monitor.cmdlog;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigService;
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

    private final AgentConfigService agentConfigService;

    /**
     * 保存
     *
     * @param userId  用户id
     * @param agentId 服务id
     * @param cmd     指令
     * @param result  结果
     * @return 结果
     */
    @Override
    public boolean save(Long userId, Long agentId, String cmd, String result) {
        if (result != null && result.length() >= 200) {
            result = result.substring(0, 200);
        }
        // 拿到服务
        AgentConfig byId = agentConfigService.getById(agentId);
        return save(CmdLogInfo.builder()
                .serviceId(byId.getServiceId())
                .serviceName(byId.getServiceName())
                .agentId(agentId)
                .agentIp(byId.getHostname())
                .cmd(cmd)
                .result(result)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .build());
    }
}
