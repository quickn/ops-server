package com.bszn.monitor.file;

import com.bszn.monitor.msg.IMsgService;
import com.bszn.system.common.result.Result;
import com.bszn.system.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fileCmd")
@RequiredArgsConstructor
@Slf4j
public class FileCmdController {

    @Resource
    IMsgService iMsgService;

    @GetMapping("/getFileByPath")
    @Operation(summary = "获取文件通过路径")
    public Result getFileByPath(@RequestParam Long agentId, @RequestParam String filePath, @RequestParam String fileName) {
        String cmd = String.format("cat %s/%s", filePath, fileName);
        String cmdResult = iMsgService.sendCMDMsgAndResponse(
                SecurityUtils.getUserId(),
                agentId,
                cmd,
                30
        );
        return Result.success(cmdResult);
    }

    @PostMapping("/createFile")
    @Operation(summary = "创建目录文件")
    public Result createFile(@RequestParam Long agentId, @RequestParam String filePath, @RequestParam String fileName) {
        String cmd = String.format("mkdir -p %s/%s", filePath, fileName);
        if (fileName.contains(".")) {
            cmd = String.format("touch %s/%s", filePath, fileName);
        }
        String cmdResult = iMsgService.sendCMDMsgAndResponse(
                SecurityUtils.getUserId(),
                agentId,
                cmd,
                30
        );
        return Result.success(cmdResult);
    }

    @DeleteMapping("/deleteFile")
    @Operation(summary = "删除目录文件")
    public Result deleteFile(@RequestParam Long agentId, @RequestParam String filePath, @RequestParam String fileName) {
        String cmd = String.format("rm -rf %s/%s", filePath, fileName);
        if (fileName.contains(".")) {
            cmd = String.format("rm %s/%s", filePath, fileName);
        }
        String cmdResult = iMsgService.sendCMDMsgAndResponse(
                SecurityUtils.getUserId(),
                agentId,
                cmd,
                30
        );
        return Result.success(cmdResult);
    }

}