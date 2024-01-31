package com.youlai.monitor.heath;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.youlai.base.ServiceBaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @version v2.3
 * @ClassName:HeathMonitor.java
 * @author: http://www.wgstart.com
 * @date: 2019年11月16日
 * @Description: app端口信息
 * @Copyright: 2017-2022 wgcloud. All rights reserved.
 */
@Data
public class HeathMonitor extends ServiceBaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = -2913111613773445949L;

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

    private Boolean isMonitor;

    @TableField(fill = FieldFill.INSERT)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}