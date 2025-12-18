package com.bszn.mq;

public interface ISenderMQ {

    void sendMsg(Long agentId, String msg);

}
