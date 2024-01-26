package com.youlai.monitor;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.youlai.monitor.agent.AgentConfig;
import com.youlai.monitor.agent.AgentConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@Tag(name = "终端")
@RestController
@RequestMapping(value = "/agent")
@Slf4j
public class AgentController {
    @Resource
    AgentConfigService iAgentConfigService;

    @ResponseBody
    @PostMapping("/getConf")
    public JSONObject getConf(@RequestBody JSONObject jsonObject) {
        String mac = jsonObject.getStr("mac");
        String hostname = jsonObject.getStr("hostname");
        AgentConfig agentConfig = iAgentConfigService.getByMac(mac, hostname);
        if (agentConfig == null) {
            return null;
        }
        return JSONUtil.parseObj(agentConfig);
    }

    @PostMapping(value = "/sendCmd")
    public String sendCmd(Long agentId, String cmd) {
        iAgentConfigService.sendCmd(agentId, cmd);
        return cmd;
    }

    @PostMapping(value = "/heart")
    public String heart(@RequestBody String paramBean) {
        JSONObject agentJsonObject = (JSONObject) JSONUtil.parse(paramBean);
        Integer serviceId = agentJsonObject.getInt("serviceId");
        String hostname = agentJsonObject.getStr("hostname");
        if (serviceId == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        AgentConfig agentConfig = iAgentConfigService.getServiceIdAndHostname(serviceId, hostname);
        if (agentConfig == null) {
            return null;
        }
        String cmd = iAgentConfigService.getCmdById(agentConfig.getId());
        if (cmd != null) {
            jsonObject.put("cmd", cmd);
            iAgentConfigService.removeCmdById(agentConfig.getId());
        }
        jsonObject.put("result", "success");
        return jsonObject.toString();
    }
}
