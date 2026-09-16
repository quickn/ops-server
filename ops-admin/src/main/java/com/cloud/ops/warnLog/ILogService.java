package com.cloud.ops.warnLog;

import com.cloud.ops.cmd.LogCmdForm;
import com.cloud.ops.mq.MsgResult;

import java.util.List;

public interface ILogService {

     List<MsgResult> analysis(LogCmdForm logCmdForm);
     String getLogsByServiceId(LogCmdForm logCmdForm);

     MsgResult detail(LogCmdForm logCmdForm);
}
