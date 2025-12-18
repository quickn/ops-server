package com.bszn.monitor;

import com.bszn.monitor.msg.MonitorCmdMsgHandle;
import com.bszn.mq.ISenderMQ;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "mq")
@RestController
@RequestMapping(value = "/mq")
@Slf4j
public class MQController {

    @Autowired
    ISenderMQ iSenderMQ;

    @Autowired
    MonitorCmdMsgHandle monitorCmdMsgHandle;

    @ResponseBody
    @PostMapping("/sendMsg")
    public Result sendMsg(@RequestParam Long agentId, @RequestParam String msg) {
        iSenderMQ.sendMsg(agentId, msg);
        return Result.success();
    }

    @GetMapping("/getMsg")
    public Result getMsg(@RequestParam String msgId) {
        String msg = monitorCmdMsgHandle.getMsg(msgId);
        return Result.success(msg);
    }
}
