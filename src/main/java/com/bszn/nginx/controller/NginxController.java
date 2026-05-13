package com.bszn.nginx.controller;

import com.bszn.monitor.msg.IMsgService;
import com.bszn.nginx.NginxConf;
import com.bszn.nginx.NginxFile;
import com.bszn.nginx.NginxFileQuery;
import com.bszn.nginx.NginxFileService;
import com.bszn.system.common.nginx.Vali;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.ValidationException;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Nginx")
@RestController
@RequestMapping("/nginx")
public class NginxController {

    @Resource
    NginxFileService nginxFileService;

    @Resource
    IMsgService iMsgService;

    @Operation(summary = "nginx文件列表")
    @GetMapping("/listFiles")
    public Result<List<NginxFile>> listFiles(@ParameterObject NginxFileQuery nginxFileQuery) {
        List<NginxFile> list = nginxFileService.listFiles(nginxFileQuery);
        return Result.success(list);
    }

    @PostMapping("/test")
    @Operation(summary = "测试配置文件")
    @ResponseBody
    public Result test(@RequestBody NginxConf nginxConf) {
        if (Vali.isEpt(nginxConf.getConf())) {
            throw new ValidationException("配置文件内容不能为空");
        }
        String confText = nginxConf.getConf();
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("cat > " + nginxConf.getFileNamePath() + " <<'ENDOFSTRING'");
        stringBuffer.append(" && sudo /usr/local/nginx/sbin/nginx -t ");
        stringBuffer.append("\n");
        stringBuffer.append(confText);
        stringBuffer.append("ENDOFSTRING");
        String str = iMsgService.sendCMDMsgAndResponse(null, nginxConf.getAgentId(), stringBuffer.toString());
        if (str.contains("语法错误") || str.contains("test failed")) {
            return Result.failed(str);
        }
        return Result.success(str);
    }

    @PostMapping("/reload")
    @Operation(summary = "重新加载配置文件")
    @ResponseBody
    public Result reload(@RequestBody NginxConf nginxConf) {
        Result result = test(nginxConf);
        if (result.getCode().equals(Result.failed().getCode())) {
            return result;
        }
        String str = iMsgService.sendCMDMsgAndResponse(null, nginxConf.getAgentId(), "sudo /usr/local/nginx/sbin/nginx -s reload");
        return Result.success(str);
    }

    @PostMapping("/stop")
    @Operation(summary = "停止Nginx")
    @ResponseBody
    public Result stop(@RequestBody NginxConf nginxConf) {
        String str = iMsgService.sendCMDMsgAndResponse(null, nginxConf.getAgentId(), "sudo /usr/local/nginx/sbin/nginx -s stop");
        return Result.success(str);
    }

    @PostMapping("/start")
    @Operation(summary = "启动Nginx")
    @ResponseBody
    public Result start(@RequestBody NginxConf nginxConf) {
        String str = iMsgService.sendCMDMsgAndResponse(null, nginxConf.getAgentId(), "sudo /usr/local/nginx/sbin/nginx");
        return Result.success(str);
    }

    @PostMapping("/addConfig")
    @ResponseBody
    public Result addConfig(@RequestBody NginxFile nginxFile) {
        nginxFileService.save(nginxFile);
        return Result.success();
    }

    @DeleteMapping("/delete/{ids}")
    @ResponseBody
    public Result delete(@PathVariable Integer ids) {
        nginxFileService.removeById(ids);
        return Result.success();
    }
}