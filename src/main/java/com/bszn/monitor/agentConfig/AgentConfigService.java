package com.bszn.monitor.agentConfig;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 代理服务配置 Service
 */
public interface AgentConfigService extends IService<AgentConfig> {

    /**
     * 获取代理服务配置列表
     */
    List<AgentConfigVO> listConfigs(AgentConfigQuery query);

    /**
     * 删除代理服务配置
     */
    boolean deleteByIds(String ids);

}
