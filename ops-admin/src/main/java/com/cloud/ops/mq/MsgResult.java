package com.cloud.ops.mq;

import lombok.Data;

@Data
public class MsgResult {
    Long msgId;
    Integer code;
    String data;
    Long agentId;
    String msgType;
}
