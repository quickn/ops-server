package com.cloud.receiver.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class SystemInfo extends BaseEntity {

    private static final long serialVersionUID = 879979812204191283L;


    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "IP")
    private String hostname;


    @Schema(description = "cpu使用率")
    private Double cpuPer;


    @Schema(description = "内存使用率")
    private Double memPer;


    @Schema(description = "磁盘总使用率")
    private Double diskPer;


    @Schema(description = "cpu核数")
    private Integer cpuCoreNum;


    @Schema(description = "CPU型号信息")
    private String cpuXh;


    @Schema(description = "系统版本信息")
    private String version;

    @Schema(description = "系统版本详细信息")
    private String versionDetail;


    @Schema(description = "主机状态，1正常 0下线")
    private Integer state;

    @Schema(description = "备注")
    private String remark;


    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;


}
