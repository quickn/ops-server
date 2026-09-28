package com.cloud.ops.heath;

import com.cloud.ops.base.MonitorBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 接口健康监控
 */
@Data
public class ApiHeathMonitor extends MonitorBaseEntity {

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

    @Schema(description = "超时预警时间(毫秒)")
    private Integer timeoutWarnTime;

    @Schema(description = "断言方式：status-响应码 / body-响应内容 / json-响应JSON字段 / none-不校验")
    private String assertType;

    @Schema(description = "断言期望值：响应码（数字字符串）或包含的字符串或JSON表达式")
    private String expectValue;

    @Schema(description = "接口返回结果")
    private String body;

    @Schema(description = "健康状态（200=成功，其它=失败）")
    private Integer heathStatus;

    @Schema(description = "最新响应时间(毫秒)")
    private Long responseTime;

}