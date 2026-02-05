package com.bszn.monitor.heath;

import com.baomidou.mybatisplus.annotation.TableField;
import com.bszn.base.ServiceBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 接口健康监控
 */
@Data
public class ApiHeathMonitor extends ServiceBaseEntity {

    private static final long serialVersionUID = -2913111613773445949L;

    @Schema(description = "接口名称")
    private String apiName;

    @Schema(description = "接口地址")
    private String apiUrl;

    @Schema(description = "请求方式 get post")
    private String requestMethod;

    @Schema(description = "内容类型 application/json,x-www-form-urlencoded 下拉")
    private String contentType;

    @Schema(description = "参数")
    private String param;

    @Schema(description = "是否开启监控")
    private Boolean isMonitor;

    @Schema(description = "超时预警时间(秒)")
    private Integer timeoutWarnTime;

    @Schema(description = "接口返回结果")
    private String body;

    @Schema(description = "健康状态")
    private Integer heathStatus;

    @Schema(description = "最新响应时间")
    private Long responseTime;

}