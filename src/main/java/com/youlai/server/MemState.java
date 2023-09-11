package com.youlai.server;

import com.youlai.base.ServiceBaseEntity;
import com.youlai.system.common.base.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * @version V2.3
 * @ClassName:MemState.java
 * @author: wgcloud
 * @date: 2019年11月16日
 * @Description: 查看内存使用情况
 * @Copyright: 2017-2022 www.wgstart.com. All rights reserved.
 */
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