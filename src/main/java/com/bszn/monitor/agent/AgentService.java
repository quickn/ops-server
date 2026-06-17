package com.bszn.monitor.agent;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bszn.monitor.cmd.ClientMsgForm;
import com.bszn.monitor.cmd.LogCmdForm;

import java.util.List;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
public interface AgentService extends IService<Agent> {

    Agent getByMac(String mac, String hostname);

    Agent getServiceIdAndHostname(Integer serviceId, String hostname);

    Agent getByServiceIdAndHost(Integer serviceId, String hostname);

    void receiveClientMsg(ClientMsgForm clientMsgForm);

    void handleAgentByServiceId(Integer serviceId, String cmd);

    void handleAgent(Long agentId, String cmd);

    /**
     * 获取服务器列表
     *
     * @param dto 查询参数
     * @return 服务器列表
     */
    List<AgentVo> list(AgentQuery dto);

    /**
     * 获取跳板机
     *
     * @param serviceId
     * @return
     */
    Agent getjumpServers(Integer serviceId);
}
