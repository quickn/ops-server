package com.cloud.server;

import com.cloud.base.ServiceBaseEntity;
import lombok.Data;


@Data
public class CpuState extends ServiceBaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = -2913111613773445949L;


    /**
     * host名称
     */
    private String hostname;

    /**
     * cpu使用率
     */
    private Double sys;

    /**
     * 当前空闲率
     */
    private Double idle;

    /**
     * cpu当前等待率
     */
    private Double iowait;


    /**
     * 添加时间
     * MM-dd hh:mm:ss
     */
    private String dateStr;


}