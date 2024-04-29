package com.bszn.monitor;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigMapper;
import com.bszn.monitor.agent.AgentConfigQuery;
import com.bszn.monitor.agent.AgentConfigService;
import com.bszn.monitor.cmd.ClientMsgForm;
import com.bszn.monitor.cmd.CmdDataForm;
import com.bszn.monitor.cmd.HeartCmdForm;
import com.bszn.monitor.cmd.LogCmdForm;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;


@Tag(name = "终端")
@RestController
@RequestMapping(value = "/agent")
@Slf4j
public class AgentController {
    @Resource
    AgentConfigService iAgentConfigService;
    @Resource
    AgentConfigMapper agentConfigMapper;

    @ResponseBody
    @GetMapping("/listPage")
    public Result listPage(@ParameterObject AgentConfigQuery agentConfigQuery) {
        Page<AgentConfig> list = agentConfigMapper.queryPage(agentConfigQuery, agentConfigMapper.getPage());
        return Result.success(list);
    }

    @ResponseBody
    @PostMapping("/save")
    public Result save(@RequestBody AgentConfig agentConfig) {
        iAgentConfigService.saveOrUpdate(agentConfig);
        return Result.success();
    }


    @ResponseBody
    @PostMapping("/getConf")
    @Operation(summary = "获取配置文件")
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
    @Operation(summary = "心跳")
    public String heart(@RequestBody HeartCmdForm heartCmdForm) {
        Integer serviceId = heartCmdForm.getServiceId();
        String hostname = heartCmdForm.getHostname();
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
            if (cmd.startsWith("{")) {
                jsonObject.put("data", cmd);
            } else {
                jsonObject.put("cmd", cmd);
            }
            iAgentConfigService.removeCmdById(agentConfig.getId());
        }
        jsonObject.put("result", "success");
        return jsonObject.toString();
    }

    @ResponseBody
    @PostMapping("/cmdData")
    @Operation(summary = "接收指令执行数据")
    public Result cmdData(@RequestBody CmdDataForm cmdDataForm) {
        iAgentConfigService.cmdData(cmdDataForm);
        return Result.success();
    }


    @ResponseBody
    @PostMapping("/receiveClientMsg")
    @Operation(summary = "接收终端消息")
    public Result receiveClientMsg(@RequestBody ClientMsgForm clientMsgForm) {
        iAgentConfigService.receiveClientMsg(clientMsgForm);
        return Result.success();
    }

    @ResponseBody
    @GetMapping("/getLogsByServiceId")
    @Operation(summary = "实时日志查询")
    public Result getLogsByServiceId(@ParameterObject LogCmdForm logCmdForm) {
        String log = iAgentConfigService.getLogsByServiceId(logCmdForm);
        return Result.success(log);
    }

    @ResponseBody
    @GetMapping("/upgradeClientByServiceId/{serviceId}")
    public Result upgradeClientByServiceId(@PathVariable Integer serviceId) {
        iAgentConfigService.upgradeClientByServiceId(serviceId);
        return Result.success();
    }
}
