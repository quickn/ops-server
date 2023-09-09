package com.youlai.agent;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
public interface IAgentConfigService extends IService<AgentConfig> {

    AgentConfig getByMac(String mac, String hostname);

    AgentConfig getServiceIdAndHostname(Integer serviceId, String hostname);
}
