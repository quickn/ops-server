package com.cloud.ops.agentConfig;

import com.cloud.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 代理服务配置 Mapper
 */
@Mapper
public interface AgentConfigMapper extends BaseQueryMapper<AgentConfig,AgentConfig> {

}
