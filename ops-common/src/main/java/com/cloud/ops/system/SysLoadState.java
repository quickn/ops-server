package com.cloud.ops.system;

import com.cloud.ops.base.MonitorBaseEntity;
import lombok.Data;

@Data
public class SysLoadState extends MonitorBaseEntity {

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
