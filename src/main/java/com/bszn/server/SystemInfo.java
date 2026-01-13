package com.bszn.server;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.bszn.base.ServiceBaseEntity;
import lombok.Data;


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

}
