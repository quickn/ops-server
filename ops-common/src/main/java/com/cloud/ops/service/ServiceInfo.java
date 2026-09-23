package com.cloud.ops.service;

import com.cloud.ops.base.MultiTenantCreateByEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceInfo extends MultiTenantCreateByEntity {
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


}
