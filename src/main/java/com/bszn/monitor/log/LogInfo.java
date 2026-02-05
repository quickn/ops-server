package com.bszn.monitor.log;

import com.bszn.base.ServiceBaseEntity;
import com.bszn.server.StaticKeys;
import lombok.Data;

@Data
public class LogInfo extends ServiceBaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = 1565538727002722890L;

    /**
     * host名称
     */
    private String hostname;

    private String title;

    /**
     * 描述
     */
    private String infoContent;

    /**
     * 0成功，1失败
     */
    private String state;

    private Boolean sendEmail;

    public LogInfo() {

    }

    public LogInfo(String title, String infoContent, Integer serviceId, String serviceName, boolean sendEmail) {
        this.title = title;
        this.infoContent = infoContent;
        this.state = StaticKeys.LOG_ERROR;
        this.sendEmail = sendEmail;
        this.setServiceId(serviceId);
        this.setServiceName(serviceName);
    }
}
