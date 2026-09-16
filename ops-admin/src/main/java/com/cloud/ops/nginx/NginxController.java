package com.cloud.ops.nginx;

import com.cloud.ops.msg.IMsgService;
import com.cloud.ops.file.FileForm;
import com.cloud.system.common.nginx.Vali;
import com.cloud.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.ValidationException;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Nginx")
@RestController
@RequestMapping("/nginx")
public class NginxController {

    @Resource
    IMsgService iMsgService;


    @PostMapping("/test")
    @Operation(summary = "测试Nginx文件")
    @ResponseBody
    public Result test(@RequestBody FileForm fileForm) {
        if (Vali.isEpt(fileForm.getFileContent())) {
            throw new ValidationException("配置文件内容不能为空");
        }
        String confText = fileForm.getFileContent();
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("cat > ");
        stringBuffer.append(fileForm.getFilePath() + "/" + fileForm.getFileName());
        stringBuffer.append(" <<'NGINX_TEST_CONFIG'");
        stringBuffer.append(" && sudo /usr/local/nginx/sbin/nginx -t ");
        stringBuffer.append("\n");
        stringBuffer.append(confText);
        stringBuffer.append("\n");
        stringBuffer.append("NGINX_TEST_CONFIG");
        String str = iMsgService.sendCMDMsgAndResponse(fileForm.getAgentId(), "测试Nginx文件", stringBuffer.toString());
        if (str.contains("语法错误") || str.contains("test failed")) {
            return Result.failed(str);
        }
        return Result.success(str);
    }

    @PostMapping("/reload")
    @Operation(summary = "重新加载Nginx文件")
    @ResponseBody
    public Result reload(@RequestBody FileForm fileForm) {
        Result result = test(fileForm);
        if (result.getCode().equals(Result.failed().getCode())) {
            return result;
        }
        String str = iMsgService.sendCMDMsgAndResponse(fileForm.getAgentId(), "重新加载Nginx文件", "sudo /usr/local/nginx/sbin/nginx -s reload");
        return Result.success(str);
    }

    @PostMapping("/stop")
    @Operation(summary = "停止Nginx")
    @ResponseBody
    public Result stop(@RequestBody FileForm fileForm) {
        String str = iMsgService.sendCMDMsgAndResponse(fileForm.getAgentId(), "停止Nginx", "sudo /usr/local/nginx/sbin/nginx -s stop");
        return Result.success(str);
    }

    @PostMapping("/start")
    @Operation(summary = "启动Nginx")
    @ResponseBody
    public Result start(@RequestBody FileForm fileForm) {
        String str = iMsgService.sendCMDMsgAndResponse(fileForm.getAgentId(), "启动Nginx", "sudo /usr/local/nginx/sbin/nginx");
        return Result.success(str);
    }

}