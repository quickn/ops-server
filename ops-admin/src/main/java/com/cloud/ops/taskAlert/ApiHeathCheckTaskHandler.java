package com.cloud.ops.taskAlert;

import com.alibaba.fastjson2.JSONObject;
import com.cloud.base.http.RestUtil;
import com.cloud.ops.alert.AlertRule;
import com.cloud.ops.alert.AlertRuleService;
import com.cloud.ops.heath.ApiHeathMonitor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * 接口检测任务处理器
 *
 * <p>对指定接口发起 HTTP 请求，根据响应状态码、超时时间、断言规则判断接口是否健康，
 * 同时提供 {@link #check(ApiHeathMonitor)} 供外部调用。</p>
 *
 * <p>断言方式（assertType）：</p>
 * <ul>
 *   <li>none — 不校验，仅判断状态码是否为 200</li>
 *   <li>status — 期望状态码匹配，expectValue 填数字</li>
 *   <li>body — 响应内容包含期望字符串，expectValue 填字符串</li>
 *   <li>json — JSON 字段断言，expectValue 支持 {@code $.field=value} 或完整 JSON 对象</li>
 * </ul>
 *
 * <p>执行成功后，若任务关联了预警规则（ruleId），则校验响应内容是否满足
 * 预警规则的触发条件，满足则推送预警信息。</p>
 *
 * <p>任务参数约定为 JSON：
 * <pre>
 * {
 *   "apiUrl": "https://example.com/api",
 *   "requestMethod": "GET",
 *   "contentType": "application/json",
 *   "param": "",
 *   "timeoutWarnTime": 3000,
 *   "assertType": "status",
 *   "expectValue": "200"
 * }
 * </pre></p>
 *
 * @author Liuyun
 */
@Component
@Slf4j
public class ApiHeathCheckTaskHandler implements TaskTypeHandler {

    /**
     * 触发类型：超过阈值触发
     */
    private static final String TRIGGER_TYPE_THRESHOLD = "THRESHOLD";

    /**
     * 结果数值提取正则（阈值匹配用）
     */
    private static final java.util.regex.Pattern NUMBER_PATTERN =
            java.util.regex.Pattern.compile("-?\\d+(\\.\\d+)?");

    @Resource
    private RestUtil restUtil;

    @Resource
    private AlertRuleService alertRuleService;

    @Override
    public String type() {
        return API_CHECk_TYPE;
    }

    @Override
    public void handle(TaskAlert taskAlert, TaskAlertRecord record) {
        if (StringUtils.isBlank(taskAlert.getTaskParams())) {
            record.setState(1);
            record.setResult("接口检测配置为空");
            return;
        }

        JSONObject params;
        try {
            params = JSONObject.parseObject(taskAlert.getTaskParams());
        } catch (Exception e) {
            record.setState(1);
            record.setResult("接口检测配置解析失败：" + e.getMessage());
            return;
        }

        String apiUrl = params.getString("apiUrl");
        if (StringUtils.isBlank(apiUrl)) {
            record.setState(1);
            record.setResult("接口地址为空");
            return;
        }

        // 将任务参数映射为 ApiHeathMonitor 对象
        ApiHeathMonitor monitor = new ApiHeathMonitor();
        monitor.setApiUrl(apiUrl);
        monitor.setRequestMethod(params.getString("requestMethod"));
        monitor.setContentType(params.getString("contentType"));
        monitor.setParam(params.getString("param"));
        monitor.setTimeoutWarnTime(params.getInteger("timeoutWarnTime"));
        monitor.setAssertType(params.getString("assertType"));
        monitor.setExpectValue(params.getString("expectValue"));

        ApiHeathMonitor result = this.check(monitor);

        StringBuilder sb = new StringBuilder();
        sb.append("接口：").append(apiUrl).append("\n");
        sb.append("状态码：").append(result.getHeathStatus()).append("\n");
        if (result.getResponseTime() != null) {
            sb.append("响应时间：").append(result.getResponseTime()).append("ms\n");
        }
        if (result.getBody() != null) {
            String body = result.getBody();
            sb.append("响应内容：").append(body.length() > 500 ? body.substring(0, 500) + "..." : body);
        }

        record.setResult(sb.toString());
        // 状态码非200 或 断言失败(被标记为500) 或 响应超时 → 标记失败
        if (result.getHeathStatus() == null || result.getHeathStatus() != 200) {
            record.setState(1);
        } else if (result.getResponseTime() != null && monitor.getTimeoutWarnTime() != null
                && result.getResponseTime() > monitor.getTimeoutWarnTime()) {
            record.setState(1);
        }

        // ========== 预警规则匹配：执行成功但响应内容满足条件则推送预警 ==========
        this.checkAndTriggerAlert(taskAlert, record, result.getBody());
    }

    /**
     * 对指定接口发起 HTTP 请求并检测健康状态
     *
     * <p>检测逻辑：发起请求 → 获取状态码 → 断言判断 → 超时判断，
     * 返回填充了检测结果的 {@link ApiHeathMonitor} 对象。</p>
     *
     * @param monitor 接口检测配置（apiUrl、requestMethod、assertType 等）
     * @return 检测结果（heathStatus、responseTime、body）
     */
    public ApiHeathMonitor check(ApiHeathMonitor monitor) {
        ApiHeathMonitor result = new ApiHeathMonitor();
        result.setId(monitor.getId());
        long start = System.currentTimeMillis();
        int status;
        String responseBody = "";

        try {
            if ("POST".equals(monitor.getRequestMethod())) {
                ResponseEntity<String> response = restUtil.post(
                        monitor.getApiUrl(), monitor.getContentType(), monitor.getParam());
                status = response.getStatusCode().value();
                if (response.getBody() != null) {
                    responseBody = response.getBody();
                }
                result.setBody(responseBody);
            } else {
                status = restUtil.get(monitor.getApiUrl());
            }
        } catch (Exception e) {
            log.error("接口检测请求异常 apiUrl:{}", monitor.getApiUrl(), e);
            status = 500;
        }

        result.setHeathStatus(status);
        long responseTime = System.currentTimeMillis() - start;
        result.setResponseTime(responseTime);

        if (status == 200) {
            if (monitor.getTimeoutWarnTime() == null) {
                monitor.setTimeoutWarnTime(3000);
            }
            // 断言判断（在超时判断之前执行，断言失败也归类为状态异常）
            String assertResult = checkAssert(
                    monitor.getAssertType(),
                    monitor.getExpectValue(),
                    status,
                    responseBody
            );
            if (assertResult != null) {
                // 断言失败，将 heathStatus 标记为 500
                result.setHeathStatus(500);
            }
            // 超时判断由调用方根据 responseTime 与 timeoutWarnTime 对比
        }

        return result;
    }

    /**
     * 断言判断
     *
     * @param assertType  断言方式（status/body/json/none）
     * @param expectValue 期望值
     * @param status      HTTP 响应状态码
     * @param body        HTTP 响应内容
     * @return 失败原因；返回 null 表示断言通过
     */
    private String checkAssert(String assertType, String expectValue, int status, String body) {
        if (assertType == null || "none".equalsIgnoreCase(assertType)) {
            return null;
        }
        if ("status".equalsIgnoreCase(assertType)) {
            if (expectValue == null || expectValue.trim().isEmpty()) {
                return "未填写期望状态码";
            }
            try {
                int expected = Integer.parseInt(expectValue.trim());
                if (status != expected) {
                    return "状态码不匹配：期望 " + expected + "，实际 " + status;
                }
                return null;
            } catch (NumberFormatException e) {
                return "期望状态码格式错误：" + expectValue;
            }
        }
        if ("body".equalsIgnoreCase(assertType)) {
            if (expectValue == null || expectValue.isEmpty()) {
                return "未填写包含字符串";
            }
            if (body == null || !body.contains(expectValue)) {
                return "响应内容不包含「" + expectValue + "」";
            }
            return null;
        }
        if ("json".equalsIgnoreCase(assertType)) {
            if (expectValue == null || expectValue.trim().isEmpty()) {
                return "未填写 JSON 字段断言";
            }
            if (body == null || body.trim().isEmpty()) {
                return "响应内容为空，无法进行 JSON 断言";
            }
            // 支持 $.path=value 形式
            String expr = expectValue.trim();
            if (expr.startsWith("$.")) {
                int eqIdx = expr.indexOf('=');
                if (eqIdx <= 2) {
                    return "JSON 表达式格式错误：" + expectValue;
                }
                String path = expr.substring(2, eqIdx);
                String expected = expr.substring(eqIdx + 1).trim();
                Object actual = readJsonPath(body, path);
                if (actual == null) {
                    return "字段不存在：" + path;
                }
                if (!String.valueOf(actual).equals(expected)) {
                    return "字段 " + path + " 不匹配：期望 " + expected + "，实际 " + actual;
                }
                return null;
            }
            // 否则按整体 JSON 对象匹配
            try {
                JSONObject expectedJson = JSONObject.parseObject(expr);
                JSONObject actualJson = JSONObject.parseObject(body);
                if (!expectedJson.equals(actualJson)) {
                    return "JSON 不匹配";
                }
                return null;
            } catch (Exception e) {
                return "JSON 解析失败：" + e.getMessage();
            }
        }
        return null;
    }

    /**
     * 按路径读取 JSON 字段（支持多级嵌套，如 data.code）
     */
    private Object readJsonPath(String json, String path) {
        try {
            JSONObject obj = JSONObject.parseObject(json);
            String[] parts = path.split("\\.");
            Object cur = obj;
            for (String p : parts) {
                if (cur == null) {
                    return null;
                }
                if (cur instanceof JSONObject) {
                    cur = ((JSONObject) cur).get(p);
                } else {
                    return null;
                }
            }
            return cur;
        } catch (Exception e) {
            return null;
        }
    }

    // ========== 预警规则匹配 ==========

    /**
     * 校验响应内容是否满足预警规则配置的触发条件，满足则推送预警
     *
     * <p>触发类型由 {@link AlertRule#getTriggerType()} 决定：
     * KEYWORD 按关键字匹配（逗号分隔，不区分大小写）；
     * THRESHOLD 按阈值匹配（响应中的任一数值超过阈值即触发）。
     * 未配置触发类型时默认按关键字处理（兼容旧数据）。</p>
     *
     * <p>仅处理「执行成功但响应内容满足条件」的场景；
     * 执行失败（state=1）的告警由上层 {@link TaskAlertServiceImpl}
     * 按连续失败阈值机制处理，避免重复预警。</p>
     */
    private void checkAndTriggerAlert(TaskAlert taskAlert, TaskAlertRecord record, String responseBody) {
        Long ruleId = taskAlert.getRuleId();
        if (ruleId == null) {
            return;
        }
        // 执行失败的场景由上层 runTask() 的连续失败阈值机制处理，此处不重复触发
        if (record.getState() != null && record.getState() == 1) {
            return;
        }

        AlertRule rule = alertRuleService.getById(ruleId);
        if (rule == null) {
            log.warn("接口检测任务关联的预警规则不存在 taskId:{} ruleId:{}", taskAlert.getId(), ruleId);
            return;
        }
        if (!Boolean.TRUE.equals(rule.getIsEnabled())) {
            log.info("预警规则已禁用，跳过 taskId:{} ruleId:{}", taskAlert.getId(), ruleId);
            return;
        }

        String triggerType = rule.getTriggerType();
        String matchedReason;
        if (TRIGGER_TYPE_THRESHOLD.equals(triggerType) && rule.getConditionThreshold() != null) {
            matchedReason = this.matchThreshold(responseBody, rule.getConditionThreshold());
        } else {
            matchedReason = this.matchKeyword(responseBody, rule.getConditionKeyword());
        }

        if (matchedReason == null) {
            return;
        }

        try {
            String alertContent = buildAlertContent(taskAlert, record, matchedReason);
            alertRuleService.triggerAlert(ruleId, alertContent);
            record.setIsAlert(true);
            log.info("接口检测结果满足预警规则，已触发预警 taskId:{} ruleId:{} reason:{}",
                    taskAlert.getId(), ruleId, matchedReason);
        } catch (Exception e) {
            log.error("触发预警规则失败 taskId:{} ruleId:{}", taskAlert.getId(), ruleId, e);
        }
    }

    private String matchKeyword(String responseBody, String keywords) {
        if (StringUtils.isBlank(keywords) || StringUtils.isBlank(responseBody)) {
            return null;
        }
        String bodyLower = responseBody.toLowerCase();
        for (String keyword : keywords.split(",")) {
            String kw = keyword.trim();
            if (StringUtils.isNotBlank(kw) && bodyLower.contains(kw.toLowerCase())) {
                return "命中关键字【" + kw + "】";
            }
        }
        return null;
    }

    private String matchThreshold(String responseBody, Double threshold) {
        if (StringUtils.isBlank(responseBody)) {
            return null;
        }
        java.util.regex.Matcher matcher = NUMBER_PATTERN.matcher(responseBody);
        while (matcher.find()) {
            double value = Double.parseDouble(matcher.group());
            if (value > threshold) {
                return "数值【" + matcher.group() + "】超过阈值【" + threshold + "】";
            }
        }
        return null;
    }

    private String buildAlertContent(TaskAlert taskAlert, TaskAlertRecord record, String matchedReason) {
        StringBuilder sb = new StringBuilder();
        sb.append("任务【").append(taskAlert.getTaskName()).append("】触发预警规则。\n");
        sb.append("执行状态：成功，").append(matchedReason).append("\n");
        sb.append("任务类型：").append(taskAlert.getTaskType()).append("\n");
        String result = record.getResult();
        if (result != null && result.length() > 500) {
            sb.append("执行结果（摘要）：").append(result, 0, 500).append("...\n");
        } else {
            sb.append("执行结果：").append(result).append("\n");
        }
        return sb.toString();
    }

}
