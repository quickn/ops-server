package com.bszn.monitor.email;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bszn.monitor.log.LogInfoService;
import com.bszn.msg.WarnMailUtil;
import com.bszn.server.StaticKeys;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "邮件")
@RestController
@RequestMapping(value = "/monitor/mailConfig")
@Slf4j
public class MailConfigController {

    @Resource
    private MailConfigService mailService;
    @Resource
    private LogInfoService logInfoService;


    @Operation(summary = "获取最新的数据")
    @GetMapping(value = "/getLastData")
    public Result<MailConfig> getLastData() {
        MailConfig mailSet = mailService.getOne(Wrappers.<MailConfig>lambdaQuery().
                orderByDesc(MailConfig::getId).last(" limit 1 "));
        return Result.success(mailSet);
    }

    /**
     * 根据条件查询列表
     *
     * @param model
     * @param request
     * @return
     */
    @GetMapping(value = "list")
    @Operation(summary = "列表")
    public String MailSetList(MailConfig MailSet, Model model, HttpServletRequest request) {
        Map<String, Object> params = new HashMap<String, Object>();
        try {
            List<MailConfig> list = mailService.listByMap(params);
            if (list.size() > 0) {
                model.addAttribute("mailSet", list.get(0));
            }
        } catch (Exception e) {
            log.error("查询邮件设置错误", e);
            logInfoService.save("查询邮件设置错误：", e.toString(), StaticKeys.LOG_ERROR);

        }
        String msg = request.getParameter("msg");
        if (!StringUtils.isEmpty(msg)) {
            if (msg.equals("save")) {
                model.addAttribute("msg", "保存成功");
            } else if (msg.equals("test")) {
                String result = request.getParameter("result");
                if ("success".equals(result)) {
                    model.addAttribute("msg", "测试发送成功");
                } else {
                    model.addAttribute("msg", "测试发送失败，请查看日志");
                }
            } else {
                model.addAttribute("msg", "删除成功");
            }
        } else {
            model.addAttribute("msg", "");
        }
        return "mail/view";
    }


    /**
     * 保存邮件设置信息
     */
    @PostMapping(value = "/save")
    @Operation(summary = "保存邮件")
    public Result saveMailSet(@RequestBody MailConfig mailConfig) {
        try {
            if (mailConfig.getId() == null) {
                mailService.saveNew(mailConfig);
            } else {
                mailService.updateById(mailConfig);
            }
            StaticKeys.mailConfig = mailConfig;
        } catch (Exception e) {
            log.error("保存邮件设置信息错误：", e);
            logInfoService.save("邮件设置信息错误", e.toString(), StaticKeys.LOG_ERROR);
        }
        return Result.success();
    }

    @PostMapping(value = "/test")
    public Result test(@RequestBody MailConfig mailConfig) {
        StaticKeys.mailConfig = mailConfig;
        String msg = WarnMailUtil.sendMail(mailConfig, "测试邮件发送", "测试邮件发送");
        if (msg == null) {
            return Result.success();
        }
        return Result.failed(msg);
    }


    @Operation(summary = "删除邮件")
    @DeleteMapping(value = "del")
    public String delete(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String errorMsg = "删除告警邮件设置错误：";
        try {
            if (!StringUtils.isEmpty(request.getParameter("id"))) {
                List<String> list = Arrays.asList(request.getParameter("id").split(","));
                mailService.removeByIds(list);
                StaticKeys.mailConfig = null;
            }
        } catch (Exception e) {
            log.error(errorMsg, e);
            logInfoService.save(errorMsg, e.toString(), StaticKeys.LOG_ERROR);
        }
        return null;
    }

}
