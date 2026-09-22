package com.cloud.ops.taskAlert;

import com.cloud.ops.mq.MsgResult;
import com.cloud.ops.msg.IMsgService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 默认脚本任务处理器（兜底）
 *
 * <p>作为未匹配到具体类型处理器时的默认实现，通过 {@link IMsgService}
 * 在目标主机上执行脚本/命令。</p>
 *
 * @author Liuyun
 */
@Component
@Order(Integer.MAX_VALUE)
public class ScriptTaskHandler implements TaskTypeHandler {

    @Resource
    private IMsgService iMsgService;

    @Override
    public String type() {
        return DEFAULT_TYPE;
    }

    @Override
    public void handle(TaskAlert taskAlert, TaskAlertRecord record) {
        Integer timeout = 10;
        MsgResult msgResult = iMsgService.sendMsgAndResponse(taskAlert.getAgentId(), taskAlert.getTaskName(),
                taskAlert.getScript(), null, timeout);
        if (msgResult != null && StringUtils.isNotEmpty(msgResult.getData())) {
            record.setResult(msgResult.getData());
        } else {
            record.setResult("任务执行成功");
        }
    }

}
