package com.bszn.monitor.msg;


import com.bszn.constant.MonitorMsgType;
import com.bszn.mq.MsgResult;

public interface IMsgService {

    String sendMsg(Long userId, Long agentId, String msg, String msgType);

    MsgResult sendMsgAndResponse(Long userId, Long agentId, String msg, String msgType, Integer timeout);


    /**
     * 发送cmd指定 默认5分钟
     *
     * @param agentId 服务id
     * @param msg     消息
     * @return 结果
     */
    default String sendCMDMsgAndResponseNon(Long userId, Long agentId, String msg) {
        return sendCMDMsgAndResponse(userId, agentId, msg).replace("\n", "");
    }

    /**
     * 发送cmd指定 默认5分钟
     *
     * @param agentId 服务id
     * @param msg     消息
     * @return 结果
     */
    default String sendCMDMsgAndResponse(Long userId, Long agentId, String msg) {
        return sendCMDMsgAndResponse(userId, agentId, msg, 300);
    }

    default String sendCMDMsgAndResponse(Long userId, Long agentId, String cmd, Integer timeout) {
        return sendMsgAndResponse(userId, agentId, cmd, MonitorMsgType.CMD, timeout).getData();
    }

    /**
     * 发送指令保留原始数据响应
     *
     * @param agentId
     * @param cmd
     * @param timeout
     * @return
     */
    default MsgResult sendCMDMsgAndRawResponse(Long userId, Long agentId, String cmd, Integer timeout) {
        return sendMsgAndResponse(userId, agentId, cmd, MonitorMsgType.CMD, timeout);
    }
}
