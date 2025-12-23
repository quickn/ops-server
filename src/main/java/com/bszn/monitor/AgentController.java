package com.bszn.monitor;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigMapper;
import com.bszn.monitor.agent.AgentConfigQuery;
import com.bszn.monitor.agent.AgentConfigService;
import com.bszn.monitor.cmd.ClientMsgForm;
import com.bszn.monitor.cmd.LogCmdForm;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
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
