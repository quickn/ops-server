package com.youlai.monitor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.youlai.monitor.email.MailService;
import com.youlai.monitor.email.MailSet;
import com.youlai.monitor.log.LogInfoService;
import com.youlai.msg.WarnMailUtil;
import com.youlai.server.StaticKeys;
import com.youlai.system.common.result.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/monitor/mailset")
public class MailSetController {

    private static final Logger logger = LoggerFactory.getLogger(MailSetController.class);

    @Resource
    private MailService mailService;
    @Resource
    private LogInfoService logInfoService;

    @GetMapping(value = "/getLastData")
    public Result<MailSet> getLastData() {
        MailSet mailSet = mailService.getOne(Wrappers.<MailSet>lambdaQuery().
                orderByDesc(MailSet::getId).last(" limit 1 "));
        return Result.success(mailSet);
    }

    /**
     * 根据条件查询列表
     *
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "list")
    public String MailSetList(MailSet MailSet, Model model, HttpServletRequest request) {
        Map<String, Object> params = new HashMap<String, Object>();
        try {
            List<MailSet> list = mailService.listByMap(params);
            if (list.size() > 0) {
                model.addAttribute("mailSet", list.get(0));
            }
        } catch (Exception e) {
            logger.error("查询邮件设置错误", e);
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
     *
     * @param MailSet
     * @return
     */
    @PostMapping(value = "/save")
    public Result saveMailSet(@RequestBody MailSet mailSet) {
        try {
            if (mailSet.getId() == null) {
                mailService.saveNew(mailSet);
            } else {
                mailService.updateById(mailSet);
            }
            StaticKeys.mailSet = mailSet;
        } catch (Exception e) {
            logger.error("保存邮件设置信息错误：", e);
            logInfoService.save("邮件设置信息错误", e.toString(), StaticKeys.LOG_ERROR);
        }
        return Result.success();
    }

    @PostMapping(value = "/test")
    public Result test(@RequestBody MailSet mailSet) {
        String result = "success";
        try {
            StaticKeys.mailSet = mailSet;
            result = WarnMailUtil.sendMail(mailSet.getToMail(), "测试邮件发送", "测试邮件发送");
        } catch (Exception e) {
            logger.error("测试邮件设置信息错误：", e);
            logInfoService.save("测试邮件设置信息错误", e.toString(), StaticKeys.LOG_ERROR);
        }
        return Result.success();
    }

    /**
     * 删除告警邮件信息
     *
     * @param id
     * @param model
     * @param request
     * @param redirectAttributes
     * @return
     */
    @RequestMapping(value = "del")
    public String delete(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String errorMsg = "删除告警邮件设置错误：";
        try {
            if (!StringUtils.isEmpty(request.getParameter("id"))) {
                List<String> list = Arrays.asList(request.getParameter("id").split(","));
                mailService.removeByIds(list);
                StaticKeys.mailSet = null;
            }
        } catch (Exception e) {
            logger.error(errorMsg, e);
            logInfoService.save(errorMsg, e.toString(), StaticKeys.LOG_ERROR);
        }
        return null;
    }

}
