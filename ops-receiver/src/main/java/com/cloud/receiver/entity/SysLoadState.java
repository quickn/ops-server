package com.cloud.receiver.entity;

import lombok.Data;

@Data
public class SysLoadState extends BaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = -4863071148000213553L;

    /**
     * host名称
     */
    private String hostname;

    /**
     * 1分钟之前到现在的负载
     */
    private Double oneLoad;

    /**
     * 5分钟之前到现在的负载
     */
    private Double fiveLoad;

    /**
     * 15分钟之前到现在的负载
     */
    private Double fifteenLoad;

}
