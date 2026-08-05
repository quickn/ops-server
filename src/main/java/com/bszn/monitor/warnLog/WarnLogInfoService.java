package com.bszn.monitor.warnLog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bszn.monitor.agent.Agent;
import com.bszn.monitor.email.MailConfig;
import com.bszn.server.SystemInfo;

/**
 * Created by Liuyun on 2023-09-09 17:03
 **/
public interface WarnLogInfoService extends IService<WarnLogInfo> {
    boolean saveErrorLog(String title, String commContent, String commContent1, Agent agentConfig);

    void save(String title, String commContent, String logError);

    void saveErrorLog(String title, String commContent, SystemInfo systemInfo);

    boolean checkSendEmail(MailConfig mailSet, String title);
}
