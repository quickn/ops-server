package com.youlai.log;

import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.agent.AgentConfig;
import com.youlai.server.SystemInfo;

/**
 * Created by Liuyun on 2023-09-09 17:03
 **/
public interface LogInfoService extends IService<LogInfo> {
    boolean saveErrorLog(String title, String commContent, String commContent1, AgentConfig agentConfig);

    void save(String title, String commContent, String logError);

    void saveErrorLog(String title, String commContent, SystemInfo systemInfo);
}
