package com.bszn.monitor.service;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class ServiceInfo {
    /**
     *
     */
    private static final long serialVersionUID = 879979812204191283L;


    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 服务名称
     */
    private String name;

    /**
     * 排序
     */
    private Integer sort;


    /**
     * 是否开启监控
     */
    private Boolean isMonitor;

}
