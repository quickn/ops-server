package com.bszn.monitor.agent;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bszn.monitor.cmd.ClientMsgForm;
import com.bszn.monitor.cmd.LogCmdForm;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
public interface AgentConfigService extends IService<AgentConfig> {

    AgentConfig getByMac(String mac, String hostname);

    AgentConfig getServiceIdAndHostname(Integer serviceId, String hostname);

    AgentConfig getByServiceIdAndHost(Integer serviceId, String hostname);

    String getLogsByServiceId(LogCmdForm logCmdForm);

    void upgradeClientByServiceId(Integer serviceId);

    void receiveClientMsg(ClientMsgForm clientMsgForm);

}
