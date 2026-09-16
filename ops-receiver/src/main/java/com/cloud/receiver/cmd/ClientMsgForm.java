package com.cloud.receiver.cmd;

import lombok.Data;

/**
 * Created by Liuyun on 2024-03-27 9:24
 **/
@Data
public class ClientMsgForm {
    String cmd;
    Long agentId;
    String hostname;
    String data;
}
