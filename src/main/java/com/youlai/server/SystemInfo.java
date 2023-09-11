package com.youlai.server;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.youlai.base.ServiceBaseEntity;
import com.youlai.system.common.base.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * @version v2.3
 * @ClassName:SystemInfo.java
 * @author: http://www.wgstart.com
 * @date: 2019年11月16日
 * @Description: 查看系统信息
 * @Copyright: 2017-2022 wgcloud. All rights reserved.
 */
@Data
public class SystemInfo extends ServiceBaseEntity {
    /**
     *
     */
    private static final long serialVersionUID = 879979812204191283L;


    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * host名称
     */
    private String hostname;

    /**
     * 系统版本信息
     */
    private String version;

    /**
     * 系统版本详细信息
     */
    private String versionDetail;

    /**
     * 内存使用率
     */
    private Double memPer;

    /**
     * core的个数(即核数)
     */
    private String cpuCoreNum;

    /**
     * cpu使用率
     */
    private Double cpuPer;

    /**
     * CPU型号信息
     */
    private String cpuXh;


    /**
     * 主机状态，1正常，2下线
     */
    private String state;

    //磁盘总使用率%
    private Double diskPer;

    /**
     * 主机备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

}
