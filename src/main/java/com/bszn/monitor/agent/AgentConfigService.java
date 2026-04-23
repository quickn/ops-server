package com.bszn.monitor.agent;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bszn.monitor.cmd.ClientMsgForm;
import com.bszn.monitor.cmd.LogCmdForm;

import java.util.List;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
public interface AgentConfigService extends IService<AgentConfig> {

    AgentConfig getByMac(String mac, String hostname);

    AgentConfig getServiceIdAndHostname(Integer serviceId, String hostname);

    AgentConfig getByServiceIdAndHost(Integer serviceId, String hostname);

    String getLogsByServiceId(LogCmdForm logCmdForm);

    void receiveClientMsg(ClientMsgForm clientMsgForm);

    void handleAgentByServiceId(Integer serviceId, String cmd);

    void handleAgent(Long agentId, String cmd);

    /**
     * 获取服务器列表
     *
     * @param dto 查询参数
     * @return 服务器列表
     */
    List<AgentConfigVo> list(AgentConfigQuery dto);

    /**
     * 获取跳板机
     *
     * @param serviceId
     * @return
     */
    AgentConfig getjumpServers(Integer serviceId);
}
