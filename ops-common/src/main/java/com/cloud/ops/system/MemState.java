package com.cloud.ops.system;

import com.cloud.ops.base.MonitorBaseEntity;
import lombok.Data;

import java.util.Date;

@Data
public class MemState extends MonitorBaseEntity {

    private static final long serialVersionUID = -1412473355088780549L;


    /**
     * host名称
     */
    private String hostname;

    /**
     * 总计内存，M
     */
    private String total;

    /**
     * 已使用多少，M
     */
    private String used;

    /**
     * 未使用，M
     */
    private String free;

    /**
     * 已使用百分比%
     */
    private Double usePer;


    /**
     * 创建时间
     */
    private Date createTime;


}
