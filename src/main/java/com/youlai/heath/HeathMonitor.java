package com.youlai.heath;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.youlai.system.common.base.BaseEntity;
import lombok.Data;

/**
 * @version v2.3
 * @ClassName:HeathMonitor.java
 * @author: http://www.wgstart.com
 * @date: 2019年11月16日
 * @Description: app端口信息
 * @Copyright: 2017-2022 wgcloud. All rights reserved.
 */
@Data
public class HeathMonitor extends BaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = -2913111613773445949L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 应用服务名称
     */
    private String appName;

    /**
     * 心跳检测Url
     */
    private String heathUrl;

    /**
     * 状态
     */
    private String heathStatus;


}