package com.cloud.ops.agent;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.mq.MsgResult;
import com.cloud.ops.cmd.ClientMsgForm;

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

    MsgResult handleAgent(Long agentId, String cmd);

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

    List<Agent> getListByServiceId(Integer serviceId);
}
