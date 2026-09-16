package com.cloud.server;

import com.cloud.base.ServiceBaseEntity;
import lombok.Data;

@Data
public class MemState extends ServiceBaseEntity {


    /**
     *
     */
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
     * 添加时间
     * yyyy-MM-dd hh:mm:ss
     */
    private String dateStr;

}