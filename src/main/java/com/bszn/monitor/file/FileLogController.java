package com.bszn.monitor.file;

import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigService;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.system.common.result.Result;
import com.bszn.system.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fileLog")
@RequiredArgsConstructor
@Slf4j
public class FileLogController {

    @Resource
    AgentConfigService agentConfigService;
    @Resource
    IMsgService iMsgService;

    @GetMapping("/analyzer")
    @Operation(summary = "分析日志")
    public Result analyzer(@RequestParam Integer serviceId, @RequestParam Long agentId, @RequestParam String logFilePath) {
        AgentConfig jump = agentConfigService.getjumpServers(serviceId);
        String cmd = String.format("curl -F \"file=@%s\" http://%s:18080/pyApi/log/analyzer", logFilePath, jump.getHostname());
        String cmdResult = iMsgService.sendCMDMsgAndResponse(
                SecurityUtils.getUserId(),
                agentId,
                cmd,
                30
        );
        return Result.success(cmdResult);
    }
}