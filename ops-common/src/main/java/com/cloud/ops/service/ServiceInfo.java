package com.cloud.ops.service;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.cloud.ops.base.MultiTenantEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ServiceInfo implements Serializable, MultiTenantEntity {
    /**
     *
     */
    private static final long serialVersionUID = 879979812204191283L;


    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 创建人ID(用于多租户数据隔离)
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

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


    @Schema(description = "标签")
    private String label;

    @Schema(description = "工作目录")
    private String workPath;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
