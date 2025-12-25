package com.bszn.mq;

import lombok.Data;

@Data
public class MsgResult {
    Integer code;
    String data;
    Long agentId;
    String msgType;
}
