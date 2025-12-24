package com.bszn.monitor;

import com.bszn.monitor.msg.CmdCacheMsgService;
import com.bszn.monitor.msg.IMsgService;
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
    public Result sendMsg(@RequestParam Long agentId, @RequestParam String msg) {
        iMsgService.sendMsg(agentId, msg);
        return Result.success();
    }


    @ResponseBody
    @PostMapping("/sendMsgAndResponse")
    public Result sendMsgAndResponse(@RequestParam Long agentId, @RequestParam String msg) {
        String response = iMsgService.sendMsgAndResponse(agentId, msg, 10);
        return Result.success(response);
    }

    @ResponseBody
    @PostMapping("/sendCMDMsgAndResponseNon")
    public Result sendCMDMsgAndResponseNon(@RequestParam Long agentId, @RequestParam String cmd) {
        String response = iMsgService.sendCMDMsgAndResponseNon(agentId, cmd);
        return Result.success(response);
    }

    @GetMapping("/getMsg")
    public Result getMsg(@RequestParam String msgId) {
        String msg = cmdCacheMsgService.getMsg(msgId);
        return Result.success(msg);
    }
}
