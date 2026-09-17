package com.cloud.ops.system;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.cloud.system.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;


/**
 * 磁盘状态
 */
@Data
public class DiskState extends BaseEntity {

    private static final long serialVersionUID = 879979812204191283L;


    @Schema(description = "IP")
    private String hostname;

    @Schema(description = "盘符类型")
    private String fileSystem;

    @Schema(description = "分区大小")
    private String size;

    @Schema(description = "已使用")
    private String used;

    @Schema(description = "可用")
    private String avail;

    @Schema(description = "已使用百分比")
    private Double usePer;

    @TableField(exist = false)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Integer serviceId;

    private String serviceName;

}
