package com.cloud.receiver.entity;

import lombok.Data;

@Data
public class WarnLogInfo extends BaseEntity {

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

}
