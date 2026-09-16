package com.cloud.receiver.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.nacos.api.model.v2.Result;
import com.cloud.receiver.cmd.ClientMsgForm;
import com.cloud.receiver.dto.AgentData;
import com.cloud.receiver.entity.Agent;
import com.cloud.receiver.entity.AgentConfig;
import com.cloud.receiver.service.AgentConfigServiceImpl;
import com.cloud.receiver.service.AgentServiceImpl;
import com.cloud.receiver.service.CmdLogInfoServiceImpl;
import com.cloud.receiver.service.DockerContainerServiceImpl;
import com.cloud.receiver.util.CamelCaseUtil;
import com.cloud.receiver.util.TokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@Slf4j
@RequestMapping("/agent")
public class AgentController {

    @Resource
    private AgentServiceImpl agentService;
    @Resource
    private AgentConfigServiceImpl agentConfigService;
    @Resource
    DockerContainerServiceImpl dockerContainerService;
    @Resource
    CmdLogInfoServiceImpl cmdLogInfoService;
    @Resource
    private TokenUtils tokenUtils;
    @Value("${spring.profiles.active}")
    String profiles;


    @ResponseBody
    @Operation(summary = "接收Agent消息")
    @PostMapping("/minTask")
    public JSONObject collect(@RequestBody String paramBean, @RequestHeader(value = "agentToken", required = false) String agentToken) {
        return agentService.collect(paramBean, agentToken);
    }

    @ResponseBody
    @PostMapping("/getConf")
    public JSONObject getConf(@RequestBody JSONObject jsonObject) {
        String mac = jsonObject.getString("mac");
        String hostname = jsonObject.getString("hostname");
        JSONObject agentJsonObject = agentConfigService.getByMac(mac, hostname);
        if (agentJsonObject == null) {
            return null;
        }
        Long agentId = agentJsonObject.getLong("id");
        AgentConfig agentConfig = agentConfigService.getAgentConfig(agentId);
        String workPath = "/home/park";
        if (agentConfig != null && StringUtils.isNotEmpty(agentConfig.getWorkPath())) {
            workPath = agentConfig.getWorkPath();
        }
        agentJsonObject.put("workPath", workPath);
        String version = jsonObject.getString("version");
        if (StringUtils.isNotEmpty(version) && !version.equals(agentJsonObject.getString("clientVersion"))) {
            Agent agentUpdate = new Agent();
            agentUpdate.setId(agentId);
            agentUpdate.setClientVersion(version);
            agentConfigService.updateById(agentUpdate);
        }
        CamelCaseUtil.underlineToCamelCase(agentJsonObject);
        return agentJsonObject;
    }


    @ResponseBody
    @PostMapping("/receive")
    public String receive(@RequestBody String paramBean) {
        log.debug(paramBean);
        JSONObject agentJsonObject = JSON.parseObject(paramBean);
        AgentData agentData = agentJsonObject.toJavaObject(AgentData.class);
        if ("docker".equals(agentData.getDataType())) {
            dockerContainerService.saveStr(agentData);
        }
        return null;
    }

    @ResponseBody
    @PostMapping("/receiveClientMsg")
    @Operation(summary = "接收终端消息")
    public Result receiveClientMsg(@RequestBody ClientMsgForm clientMsgForm) {
        agentConfigService.receiveClientMsg(clientMsgForm);
        return Result.success();
    }

    @ResponseBody
    @PostMapping("/receiveCmdLog")
    @Operation(summary = "接收Agent指令日志")
    public JSONObject receiveCmdLog(@RequestBody String paramBean, @RequestHeader(value = "agentToken", required = false) String agentToken) {
        log.debug(paramBean);
        JSONObject agentJsonObject = JSONObject.parseObject(paramBean);
        JSONObject resultJson = new JSONObject();

        // 验证token
        if (!tokenUtils.checkAgentToken(agentToken) && !"dev".equals(profiles)) {
            log.error("token is invalidate");
            resultJson.put("result", "error：token is invalidate");
            return resultJson;
        }

        try {
            // 获取必要参数
            Integer serviceId = agentJsonObject.getInteger("serviceId");
            String hostname = agentJsonObject.getString("hostname");
            Integer agentId = agentJsonObject.getInteger("agentId");

            if (serviceId == null) {
                resultJson.put("result", "error: serviceId is required");
                return resultJson;
            }

            // 获取agent配置
            Agent agentConfig = null;
            if (agentId != null) {
                agentConfig = agentConfigService.getById(agentId);
            } else {
                agentConfig = agentConfigService.getServiceIdAndHostname(serviceId, hostname);
            }

            if (agentConfig == null) {
                log.error("agentConfig is null" + serviceId + " " + hostname + agentId);
                resultJson.put("result", "error: agentConfig not found");
                return resultJson;
            }

            // 保存指令日志
            cmdLogInfoService.saveAgentJsonObject(agentJsonObject, agentConfig);
            resultJson.put("result", "success");
        } catch (Exception e) {
            log.error("接收指令日志异常", e);
            resultJson.put("result", "error：" + e.getMessage());
        }

        return resultJson;
    }

}
