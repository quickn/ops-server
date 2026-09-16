package com.cloud.ops.warnLog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.ops.agent.Agent;
import com.cloud.ops.email.MailConfig;
import com.cloud.ops.system.SystemInfo;

/**
 * Created by Liuyun on 2023-09-09 17:03
 **/
public interface WarnLogInfoService extends IService<WarnLogInfo> {
    boolean saveErrorLog(String title, String commContent, String commContent1, Agent agentConfig);

    void save(String title, String commContent, String logError);

    void saveErrorLog(String title, String commContent, SystemInfo systemInfo);

    boolean checkSendEmail(MailConfig mailSet, String title);
}
