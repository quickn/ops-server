package com.bszn.monitor.file;

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
@RequestMapping("/fileCmd")
@RequiredArgsConstructor
@Slf4j
public class FileCmdController {

    @Resource
    IMsgService iMsgService;

    @GetMapping("/getByPath")
    @Operation(summary = "获取文件通过路径")
    public Result getByPath(@RequestParam Long agentId, @RequestParam String fileNamePath) {
        String cmd = String.format("cat %s", fileNamePath);
        String cmdResult = iMsgService.sendCMDMsgAndResponse(
                SecurityUtils.getUserId(),
                agentId,
                cmd,
                30
        );
        return Result.success(cmdResult);
    }

}