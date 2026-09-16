package com.cloud.ops.warnLog;

import com.cloud.base.ServiceBaseEntity;
import com.cloud.ops.server.StaticKeys;
import lombok.Data;

@Data
public class WarnLogInfo extends ServiceBaseEntity {

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

    /**
     * 阈值
     */
    private String threshold;

    public WarnLogInfo() {

    }

    public WarnLogInfo(String title, String infoContent, Integer serviceId, String serviceName, boolean sendEmail) {
        this.title = title;
        this.infoContent = infoContent;
        this.state = StaticKeys.LOG_ERROR;
        this.sendEmail = sendEmail;
        this.setServiceId(serviceId);
        this.setServiceName(serviceName);
    }
}
