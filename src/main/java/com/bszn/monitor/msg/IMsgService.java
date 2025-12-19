package com.bszn.monitor.msg;

public interface IMsgService {

    String sendMsg(Long agentId, String msg);

    String sendMsgAndResponse(Long agentId, String msg, Integer timeout);
}
