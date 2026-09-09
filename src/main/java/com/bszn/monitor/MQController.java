package com.bszn.monitor;

import com.bszn.monitor.msg.CmdCacheMsgService;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.mq.MsgResult;
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
    IMsgService iMsgService;

    @Autowired
    CmdCacheMsgService cmdCacheMsgService;

    @ResponseBody
    @PostMapping("/sendMsg")
    public Result sendMsg(@RequestParam Long agentId, @RequestParam String command, @RequestParam String msg, @RequestParam String msgType) {
        iMsgService.sendMsg(agentId, command, msg, msgType, null);
        return Result.success();
    }


    @ResponseBody
    @PostMapping("/sendMsgAndResponse")
    public MsgResult sendMsgAndResponse(@RequestParam Long agentId, @RequestParam String command, @RequestParam String msg, @RequestParam String msgType) {
        return iMsgService.sendMsgAndResponse(agentId, command, msg, msgType, 10);
    }

    @ResponseBody
    @PostMapping("/sendCMDMsgAndResponseNon")
    public Result sendCMDMsgAndResponseNon(@RequestParam Long agentId, @RequestParam String cmd) {
        String response = iMsgService.sendCMDMsgAndResponse(agentId, "mq", cmd, 60);
        return Result.success(response);
    }

    @GetMapping("/getMsg")
    public MsgResult getMsg(@RequestParam String msgId) {
        return cmdCacheMsgService.getMsgResult(msgId, 10);
    }
}
