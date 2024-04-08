package com.bszn.monitor.system;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.bszn.base.ServiceBaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * 系统信息
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
     * cpu使用率
     */
    private Double cpuPer;


    /**
     * 内存使用率
     */
    private Double memPer;



    //磁盘总使用率%
    private Double diskPer;


    /**
     * core的个数(即核数)
     */
    private String cpuCoreNum;


    /**
     * CPU型号信息
     */
    private String cpuXh;


    /**
     * 系统版本信息
     */
    private String version;

    /**
     * 系统版本详细信息
     */
    private String versionDetail;


    /**
     * 主机状态，1正常，2下线
     */
    private String state;

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
