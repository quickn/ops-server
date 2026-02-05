package com.bszn.monitor.heath;

import com.baomidou.mybatisplus.annotation.TableField;
import com.bszn.base.ServiceBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 */
@Data
public class ApiHeathMonitor extends ServiceBaseEntity {

    private static final long serialVersionUID = -2913111613773445949L;

    @Schema(description = "接口名称")
    private String apiName;

    @Schema(description = "接口地址")
    private String apiUrl;

    @Schema(description = "请求方式")
    private String requestMethod;

    @Schema(description = "是否开启监控")
    private Boolean isMonitor;

    @Schema(description = "超时预警时间(秒)")
    private Long timeoutWarnTime;

    @Schema(description = "健康状态")
    private Integer heathStatus;

    @Schema(description = "最新响应时间")
    private Long responseTime;

    @TableField(exist = false)
    String serviceName;


}