package com.bszn.monitor.msg;


import com.alibaba.fastjson.JSONObject;

public interface IMsgService {

    String sendMsg(Long agentId, String msg);

    String sendMsgAndResponse(Long agentId, String msg, Integer timeout);


    /**
     * 发送cmd消息
     *
     * @param agentId 服务id
     * @param msg     消息
     * @return 消息id
     */
    default String sendCMDMsg(Long agentId, String msg) {
        return sendMsg(agentId, "{\"cmd\":\"" + msg + "\"}");
    }

    /**
     * 发送cmd指定 默认5分钟
     *
     * @param agentId 服务id
     * @param msg     消息
     * @return 结果
     */
    default String sendCMDMsgAndResponse(Long agentId, String msg) {
        String string = sendMsgAndResponse(agentId, "{\"cmd\":\"" + msg + "\"}", 300);
        return JSONObject.parseObject(string).getString("data").replace("\n", "");
    }
}
