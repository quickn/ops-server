package com.cloud.receiver.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


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


}
