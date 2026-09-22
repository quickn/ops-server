package com.cloud.ops.taskAlert;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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

    @Resource
    private IMsgService iMsgService;

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
        for (Long agentId : agentIds) {
            try {
                MsgResult msgResult = iMsgService.sendMsgAndResponse(
                        agentId, taskAlert.getTaskName(), script, MonitorMsgType.CMD, timeout);
                String result = msgResult != null && StringUtils.isNotEmpty(msgResult.getData())
                        ? msgResult.getData() : "执行成功";
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
    }

}
