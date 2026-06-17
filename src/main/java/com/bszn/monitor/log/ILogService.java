package com.bszn.monitor.log;

import com.bszn.monitor.cmd.LogCmdForm;
import com.bszn.mq.MsgResult;

import java.util.List;

public interface ILogService {

     List<MsgResult> analysis(LogCmdForm logCmdForm);
     String getLogsByServiceId(LogCmdForm logCmdForm);

}
