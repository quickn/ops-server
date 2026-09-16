package com.cloud.ops.msg;


import com.cloud.constant.MonitorMsgType;
import com.cloud.ops.cmdlog.CmdLogInfo;
import com.cloud.mq.MsgResult;

public interface IMsgService {

    String sendMsg(Long agentId, String command, String script, String msgType, Integer timeout);

    MsgResult sendMsgAndResponse(Long agentId, String command, String script, String msgType, Integer timeout);


    default String sendCMDMsgAndResponse(Long agentId, String command, String script, Integer timeout) {
        return sendMsgAndResponse(agentId, command, script, MonitorMsgType.CMD, timeout).getData();
    }

    default String sendCMDMsgAndResponse(Long agentId, String command, String script) {
        return sendMsgAndResponse(agentId, command, script, MonitorMsgType.CMD, 10).getData();
    }

    /**
     * 发送指令保留原始数据响应
     *
     * @param agentId
     * @param script
     * @param timeout
     * @return
     */
    default MsgResult sendCMDMsgAndRawResponse(Long agentId, String command, String script, Integer timeout, String remark) {
        CmdLogInfo cmdLogInfo = CmdLogInfo.builder().agentId(agentId).command
                (command).script(script).msgType(MonitorMsgType.CMD).timeout(timeout).remark(remark).build();
        return sendMsgAndResponse(cmdLogInfo);
    }

    default MsgResult sendTaskMsgResponse(Long agentId, String command, String msg, Integer timeout) {
        return sendMsgAndResponse(agentId, command, msg, MonitorMsgType.TASK, timeout);
    }

    MsgResult sendMsgAndResponse(CmdLogInfo cmdLogInfo);
}
