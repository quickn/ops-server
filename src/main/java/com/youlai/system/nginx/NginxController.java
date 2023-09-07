package com.youlai.system.nginx;

import com.youlai.system.common.nginx.NginxUtils;
import com.youlai.system.common.nginx.Vali;
import com.youlai.system.common.result.Result;
import com.youlai.system.manager.NginxManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.ValidationException;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * @Auther: liuyun
 * @Date: 2023-09-06 15:11
 * @Email 719348277@qq.com
 * @Description: nginx
 */
@Tag(name = "Nginx")
@RestController
@RequestMapping("/config/nginx")
public class NginxController {

    @Resource
    private NginxManager nginxManager;

    @Resource
    NginxFileService nginxFileService;

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
        NginxUtils.check(nginxConf.getConf(), nginxConf.getFilePath());
        return Result.success();
    }

    /**
     * 获取当前最新配置信息(刷新操作)
     *
     * @return
     */
    @RequestMapping("conf/refresh")
    @ResponseBody
    public Result refresh(@RequestBody NginxConf nginxConf) {
        NginxUtils.toString(NginxUtils.read(nginxConf.getFilePath()));
        return Result.success();
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
}