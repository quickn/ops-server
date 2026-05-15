package com.bszn.monitor.file;

import com.bszn.monitor.msg.IMsgService;
import com.bszn.system.common.nginx.Vali;
import com.bszn.system.common.result.Result;
import com.bszn.system.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.ValidationException;
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


    @PostMapping("/saveFile")
    @Operation(summary = "保存文件")
    @ResponseBody
    public Result saveFile(@RequestBody FileForm fileForm) {
        String confText = fileForm.getFileContent();
        if (Vali.isEpt(confText)) {
            throw new ValidationException("配置文件内容不能为空");
        }
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("cat > ");
        stringBuffer.append(fileForm.getFilePath() + "/" + fileForm.getFileName());
        stringBuffer.append(" <<'saveFile'");
        stringBuffer.append("\n");
        stringBuffer.append(confText);
        stringBuffer.append("\n");
        stringBuffer.append("saveFile");
        String str = iMsgService.sendCMDMsgAndResponse(null, fileForm.getAgentId(), stringBuffer.toString());
        if (str.contains("语法错误") || str.contains("test failed")) {
            return Result.failed(str);
        }
        return Result.success(str);
    }

}