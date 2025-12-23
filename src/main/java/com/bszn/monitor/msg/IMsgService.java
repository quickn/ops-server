package com.bszn.monitor.msg;


import com.alibaba.fastjson.JSONObject;
import com.bszn.utils.IpUtil;

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
        String ip = IpUtil.getIPv4Ip();
        return sendMsg(agentId, "{\"cmd\":\"" + msg + "\",\"ip\":\"" + ip + "\"}");
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

    default String sendCMDMsgAndResponse(Long agentId, String msg, Integer timeout) {
        String ip = IpUtil.getIPv4Ip();
        String string = sendMsgAndResponse(agentId, "{\"cmd\":\"" + msg + "\",\"ip\":\"" + ip + "\"}", timeout);
        return JSONObject.parseObject(string).getString("data");
    }

    /**
     * 发送指令保留原始数据响应
     *
     * @param agentId
     * @param msg
     * @param timeout
     * @return
     */
    default String sendCMDMsgAndRawResponse(Long agentId, String msg, Integer timeout) {
        String ip = IpUtil.getIPv4Ip();
        String string = sendMsgAndResponse(agentId, "{\"cmd\":\"" + msg + "\",\"ip\":\"" + ip + "\"}", timeout);
        return JSONObject.parseObject(string).getString("data");
    }
}
