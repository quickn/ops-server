package com.youlai.monitor.agent;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
public interface AgentConfigService extends IService<AgentConfig> {

    AgentConfig getByMac(String mac, String hostname);

    AgentConfig getServiceIdAndHostname(Integer serviceId, String hostname);

    void sendCmd(Long agentId, String cmd);

    String getCmdById(Long id);

    void removeCmdById(Long id);

    AgentConfig getByServiceIdAndHost(Integer serviceId, String hostname);


}
