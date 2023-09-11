package com.youlai.server;

import com.youlai.base.ServiceBaseEntity;
import lombok.Data;

/**
 * @version v2.3
 * @ClassName:AppInfo.java
 * @author: http://www.wgstart.com
 * @date: 2019年11月16日
 * @Description: app端口信息
 * @Copyright: 2017-2022 wgcloud. All rights reserved.
 */
@Data
public class AppInfo extends ServiceBaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = -2913111613773445949L;


    /**
     * host名称
     */
    private String hostname;

    /**
     * 应用进程ID
     */
    private String appPid;

    /**
     * 进程获取途径，1进程id号，2进程pid文件
     */
    private String appType;

    /**
     * 进程名称
     */
    private String appName;

    /**
     * 内存使用率%
     */
    private Double memPer;

    /**
     * cpu使用率%
     */
    private Double cpuPer;


    /**
     * 进程状态，1正常，2下线
     */
    private String state;



}