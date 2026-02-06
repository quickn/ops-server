package com.bszn.monitor.log;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.email.MailConfig;
import com.bszn.server.SystemInfo;

/**
 * Created by Liuyun on 2023-09-09 17:03
 **/
public interface LogInfoService extends IService<LogInfo> {
    boolean saveErrorLog(String title, String commContent, String commContent1, AgentConfig agentConfig);

    void save(String title, String commContent, String logError);

    void saveErrorLog(String title, String commContent, SystemInfo systemInfo);

    boolean checkSendEmail(MailConfig mailSet, String title);
}
