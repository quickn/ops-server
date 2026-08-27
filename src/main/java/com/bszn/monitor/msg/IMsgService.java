package com.bszn.monitor.msg;


import com.bszn.constant.MonitorMsgType;
import com.bszn.monitor.cmdlog.CmdLogInfo;
import com.bszn.mq.MsgResult;

public interface IMsgService {

    String sendMsg(Long agentId, String command, String script, String msgType, Integer timeout);

    MsgResult sendMsgAndResponse(Long agentId, String command, String script, String msgType, Integer timeout);


    /**
     * 发送cmd指定 默认5分钟
     *
     * @param agentId 服务id
     * @param msg     消息
     * @return 结果
     */
    default String sendCMDMsgAndResponseNon(Long agentId, String msg) {
        return sendCMDMsgAndResponse(agentId, msg).replace("\n", "");
    }

    /**
     * 发送cmd指定 默认5分钟
     *
     * @param agentId 服务id
     * @param msg     消息
     * @return 结果
     */
    default String sendCMDMsgAndResponse(Long agentId, String msg) {
        return sendCMDMsgAndResponse(agentId, null, msg, 300);
    }


    default String sendCMDMsgAndResponse(Long agentId, String command, String script, Integer timeout) {
        return sendMsgAndResponse(agentId, command, script, MonitorMsgType.CMD, timeout).getData();
    }

    /**
     * 发送指令保留原始数据响应
     *
     * @param agentId
     * @param script
     * @param timeout
     * @return
     */
    default MsgResult sendCMDMsgAndRawResponse(Long agentId, String command, String script, Integer timeout) {
        return sendMsgAndResponse(agentId, command, script, MonitorMsgType.CMD, timeout);
    }

    default MsgResult sendTaskMsgResponse(Long agentId, String command, String msg, Integer timeout) {
        return sendMsgAndResponse(agentId, command, msg, MonitorMsgType.TASK, timeout);
    }

    MsgResult sendMsgAndResponse(CmdLogInfo cmdLogInfo);
}
