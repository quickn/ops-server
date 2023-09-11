package com.youlai.server;

import com.youlai.base.ServiceBaseEntity;
import lombok.Data;

/**
 * @version V2.3
 * @ClassName:CpuState.java
 * @author: wgcloud
 * @date: 2019年11月16日
 * @Description: 查看CPU使用情况
 * @Copyright: 2017-2022 www.wgstart.com. All rights reserved.
 */
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
     * 用户态的CPU时间（%）废弃
     */
    private String user;

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
     * 硬中断时间（%） 废弃
     */
    private String irq;

    /**
     * 软中断时间（%） 废弃
     */
    private String soft;

    /**
     * 添加时间
     * MM-dd hh:mm:ss
     */
    private String dateStr;


}