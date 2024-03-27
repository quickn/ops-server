package com.youlai.monitor.cmd;

import lombok.Data;

/**
 * Created by Liuyun on 2024-03-27 9:24
 **/
@Data
public class LogCmdForm {
    Integer serviceId;
    String dockerName;
    String keyword;
    String createDate;
    String logLevel = "info";
    Long timeout = 5000L;
    String cmd;
}
