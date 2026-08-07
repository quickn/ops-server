package com.bszn.monitor.warnLog;

import com.bszn.ops.cmd.LogCmdForm;
import com.bszn.mq.MsgResult;

import java.util.List;

public interface ILogService {

     List<MsgResult> analysis(LogCmdForm logCmdForm);
     String getLogsByServiceId(LogCmdForm logCmdForm);

     MsgResult detail(LogCmdForm logCmdForm);
}
