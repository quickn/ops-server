package com.bszn.nginx.controller;

import com.bszn.monitor.msg.IMsgService;
import com.bszn.nginx.*;
import com.bszn.system.common.nginx.Vali;
import com.bszn.system.common.result.Result;
import com.bszn.system.manager.NginxManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    private NginxManager nginxManager;

    @Resource
    NginxFileService nginxFileService;

    @Resource
    IMsgService iMsgService;

    @Operation(summary = "nginx文件列表", security = {@SecurityRequirement(name = "Authorization")})
    @GetMapping("/listFiles")
    public Result<List<NginxFile>> listFiles(@ParameterObject NginxFileQuery nginxFileQuery) {
        List<NginxFile> list = nginxFileService.listFiles(nginxFileQuery);
        return Result.success(list);
    }

    @GetMapping("/getConfig")
    public Result<String> getConfig(@ParameterObject NginxFileQuery nginxFileQuery) {
        String str = NginxUtils.toString(NginxUtils.read(nginxFileQuery.getFilePath()));
        return Result.success(str);
    }

    @PostMapping("/check")
    @ResponseBody
    public Result check(@RequestBody NginxConf nginxConf) {
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

    @PutMapping("/save")
    @ResponseBody
    public Result save(@RequestBody NginxConf conf) {
        String confStr = conf.getConf();
        String backConf = NginxUtils.toString(NginxUtils.read(conf.getFilePath()));
        if (Vali.isEpt(confStr)) {
            throw new ValidationException("配置文件内容不能为空");
        }
        //尝试写到Nginx配置文件
        try {
            NginxUtils.save(confStr, conf.getFilePath());
            //重启Nginx
            nginxManager.reload();
        } catch (Exception e) {
            //恢复到上一次配置
            NginxUtils.save(backConf, conf.getFilePath());
            throw new ValidationException("已取消保存操作:" + e.getMessage(), e);
        }
        return Result.success();
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