package com.bszn.server;

import com.bszn.base.ServiceBaseEntity;
import lombok.Data;

/**
 * @version v2.3
 * @ClassName:DeskState.java
 * @author: http://www.wgstart.com
 * @date: 2019年11月16日
 * @Description: 查看磁盘大小使用信息
 * @Copyright: 2017-2022 wgcloud. All rights reserved.
 */
@Data
public class DiskState extends ServiceBaseEntity {


    /**
     *
     */
    private static final long serialVersionUID = 879979812204191283L;


    /**
     * host名称
     */
    private String hostname;

    /**
     * 盘符类型
     */
    private String fileSystem;

    /**
     * 分区大小
     */
    private String size;

    /**
     * 已使用
     */
    private String used;

    /**
     * 可用
     */
    private String avail;

    /**
     * 已使用百分比
     */
    private Double usePer;

    /**
     * 添加时间
     * yyyy-MM-dd hh:mm:ss
     */
    private String dateStr;

}
