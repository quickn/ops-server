package com.cloud.ops.taskAlert;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cloud.ops.alert.AlertRule;
import com.cloud.ops.alert.AlertRuleService;
import com.cloud.ops.constant.MonitorMsgType;
import com.cloud.ops.mq.MsgResult;
import com.cloud.ops.msg.IMsgService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认脚本任务处理器（兜底）
 *
 * <p>作为未匹配到具体类型处理器时的默认实现，通过 {@link IMsgService}
 * 在目标主机上执行脚本/命令。</p>
 *
 * <p>脚本执行后，若任务关联了预警规则（ruleId），则校验执行结果是否满足
 * 预警规则的触发条件，满足则推送预警信息。触发类型：</p>
 * <ul>
 *   <li>KEYWORD（关键字触发）：conditionKeyword 为逗号分隔的关键字，结果中包含任一关键字即触发，
 *       例如：{@code OutOfMemoryError,Full GC}</li>
 *   <li>THRESHOLD（超过阈值触发）：conditionThreshold 为阈值，结果中的任一数值超过该阈值即触发，
 *       例如阈值为 90，脚本输出 95.5 则触发</li>
 * </ul>
 *
 * <p>任务参数约定为 JSON：
 * <pre>
 * {
 *   "agentIds": [1, 2],
 *   "script": "echo hello"
 * }
 * </pre>
 * 兼容旧格式：任务参数为纯脚本文本，执行主机取任务关联主机（agentId）。</p>
 *
 * @author Liuyun
 */
@Component
@Order(Integer.MAX_VALUE)
@Slf4j
public class ShellScriptTaskHandler implements TaskTypeHandler {

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
    private IMsgService iMsgService;

    @Resource
    private AlertRuleService alertRuleService;

    @Override
    public String type() {
        return DEFAULT_TYPE;
    }

    @Override
    public void handle(TaskAlert taskAlert, TaskAlertRecord record) {
        Integer timeout = 10;
        String taskParams = taskAlert.getTaskParams();
        if (StringUtils.isBlank(taskParams)) {
            record.setState(1);
            record.setResult("任务参数为空");
            return;
        }

        // 解析任务参数：兼容 JSON（agentIds + script）与纯文本脚本两种格式
        String script = taskParams;
        List<Long> agentIds = new ArrayList<>();
        String trimmed = taskParams.trim();
        if (trimmed.startsWith("{")) {
            try {
                JSONObject json = JSONObject.parseObject(trimmed);
                String s = json.getString("script");
                if (StringUtils.isNotBlank(s)) {
                    script = s;
                }
                JSONArray ids = json.getJSONArray("agentIds");
                if (ids != null) {
                    agentIds.addAll(ids.toJavaList(Long.class));
                }
            } catch (Exception e) {
                log.warn("任务参数 JSON 解析失败，按纯文本脚本处理 taskId:{}", taskAlert.getId());
            }
        }
        // JSON 未指定主机时，取任务关联主机（兼容旧格式）
        if (agentIds.isEmpty() && taskAlert.getAgentId() != null) {
            agentIds.add(taskAlert.getAgentId());
        }
        if (agentIds.isEmpty()) {
            record.setState(1);
            record.setResult("未指定执行主机");
            return;
        }
        if (StringUtils.isBlank(script)) {
            record.setState(1);
            record.setResult("脚本内容为空");
            return;
        }

        // 依次在各主机执行脚本
        boolean allSuccess = true;
        StringBuilder output = new StringBuilder();
        StringBuilder results = new StringBuilder();
        for (Long agentId : agentIds) {
            try {
                MsgResult msgResult = iMsgService.sendMsgAndResponse(
                        agentId, taskAlert.getTaskName(), script, MonitorMsgType.CMD, timeout);
                String result = msgResult != null && StringUtils.isNotEmpty(msgResult.getData())
                        ? msgResult.getData() : "执行成功";
                results.append(result).append(",");
                output.append("[agent-").append(agentId).append("] ").append(result).append("\n");
            } catch (Exception e) {
                allSuccess = false;
                log.error("脚本任务执行异常 agentId:{}", agentId, e);
                output.append("[agent-").append(agentId).append("] 执行失败：")
                        .append(e.getMessage()).append("\n");
            }
        }
        record.setResult(output.toString());
        if (!allSuccess) {
            record.setState(1);
        }

        // ========== 预警规则匹配：结果满足规则条件则推送警告信息 ==========
        this.checkAndTriggerAlert(taskAlert, record, results.toString().trim());
    }

    /**
     * 校验执行结果是否满足预警规则配置的触发条件，满足则推送预警
     *
     * <p>触发类型由 {@link AlertRule#getTriggerType()} 决定：
     * KEYWORD 按关键字匹配（逗号分隔，不区分大小写）；
     * THRESHOLD 按阈值匹配（结果中的任一数值超过阈值即触发）。
     * 未配置触发类型时默认按关键字处理（兼容旧数据）。</p>
     *
     * <p>仅处理「执行成功但结果内容满足条件」的场景；
     * 执行失败（state=1）的告警由上层 {@link TaskAlertServiceImpl}
     * 按连续失败阈值机制处理，避免重复预警。</p>
     */
    private void checkAndTriggerAlert(TaskAlert taskAlert, TaskAlertRecord record, String results) {
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
            log.warn("任务关联的预警规则不存在 taskId:{} ruleId:{}", taskAlert.getId(), ruleId);
            return;
        }
        if (!Boolean.TRUE.equals(rule.getIsEnabled())) {
            log.info("预警规则已禁用，跳过 taskId:{} ruleId:{}", taskAlert.getId(), ruleId);
            return;
        }

        String triggerType = rule.getTriggerType();

        // 按触发类型匹配，返回命中原因描述（未命中返回 null）
        String matchedReason;
        if (TRIGGER_TYPE_THRESHOLD.equals(triggerType) && rule.getConditionThreshold() != null) {
            matchedReason = this.matchThreshold(results, rule.getConditionThreshold());
        } else {
            matchedReason = this.matchKeyword(results, rule.getConditionKeyword());
        }

        if (matchedReason == null) {
            return;
        }

        // 构建预警内容并触发
        try {
            String alertContent = buildAlertContent(taskAlert, record, matchedReason);
            alertRuleService.triggerAlert(ruleId, alertContent);
            record.setIsAlert(true);
            log.info("脚本结果满足预警规则，已触发预警 taskId:{} ruleId:{} reason:{}",
                    taskAlert.getId(), ruleId, matchedReason);
        } catch (Exception e) {
            log.error("触发预警规则失败 taskId:{} ruleId:{}", taskAlert.getId(), ruleId, e);
        }
    }

    /**
     * 关键字匹配：keywords 为逗号分隔的关键字列表，结果中包含任一关键字（不区分大小写）即命中
     *
     * @return 命中原因描述，未命中返回 null
     */
    private String matchKeyword(String result, String keywords) {
        if (StringUtils.isBlank(keywords) || StringUtils.isBlank(result)) {
            return null;
        }
        String resultLower = result.toLowerCase();
        for (String keyword : keywords.split(",")) {
            String kw = keyword.trim();
            if (StringUtils.isNotBlank(kw) && resultLower.contains(kw.toLowerCase())) {
                return "命中关键字【" + kw + "】";
            }
        }
        return null;
    }

    /**
     * 阈值匹配：从结果中提取所有数值，任一数值超过阈值即命中
     *
     * @return 命中原因描述，未命中返回 null
     */
    private String matchThreshold(String result, Double threshold) {
        if (StringUtils.isBlank(result)) {
            return null;
        }
        java.util.regex.Matcher matcher = NUMBER_PATTERN.matcher(result);
        while (matcher.find()) {
            double value = Double.parseDouble(matcher.group());
            if (value > threshold) {
                return "数值【" + matcher.group() + "】超过阈值【" + threshold + "】";
            }
        }
        return null;
    }

    /**
     * 构建预警内容
     */
    private String buildAlertContent(TaskAlert taskAlert, TaskAlertRecord record, String matchedReason) {
        StringBuilder sb = new StringBuilder();
        sb.append("任务【").append(taskAlert.getTaskName()).append("】触发预警规则。\n");
        sb.append("执行状态：成功，").append(matchedReason).append("\n");
        sb.append("任务类型：").append(taskAlert.getTaskType()).append("\n");

        // 截取结果摘要（避免预警内容过长）
        String result = record.getResult();
        if (result != null && result.length() > 500) {
            sb.append("执行结果（摘要）：").append(result, 0, 500).append("...\n");
        } else {
            sb.append("执行结果：").append(result).append("\n");
        }

        return sb.toString();
    }

}
