package com.bszn.monitor.msg;


import com.bszn.constant.MonitorMsgType;
import com.bszn.mq.MsgResult;

public interface IMsgService {

    String sendMsg(Long agentId, String msg, String msgType);

    MsgResult sendMsgAndResponse(Long agentId, String msg, String msgType, Integer timeout);


    /**
     * 发送cmd消息
     *
     * @param agentId 服务id
     * @param msg     消息
     * @return 消息id
     */
    default String sendCMDMsg(Long agentId, String msg) {
        return sendMsg(agentId, msg, MonitorMsgType.CMD);
    }

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
        return sendCMDMsgAndResponse(agentId, msg, 300);
    }

    default String sendCMDMsgAndResponse(Long agentId, String cmd, Integer timeout) {
        return sendMsgAndResponse(agentId, cmd, MonitorMsgType.CMD, timeout).getData();
    }

    /**
     * 发送指令保留原始数据响应
     *
     * @param agentId
     * @param cmd
     * @param timeout
     * @return
     */
    default MsgResult sendCMDMsgAndRawResponse(Long agentId, String cmd, Integer timeout) {
        return sendMsgAndResponse(agentId, cmd, MonitorMsgType.CMD, timeout);
    }
}
