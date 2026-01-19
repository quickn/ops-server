package com.bszn.monitor;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bszn.monitor.agent.*;
import com.bszn.monitor.cmd.ClientMsgForm;
import com.bszn.monitor.cmd.LogCmdForm;
import com.bszn.monitor.service.ServiceInfo;
import com.bszn.monitor.service.ServiceInfoService;
import com.bszn.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "终端")
@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController {
    @Resource
    AgentConfigService iAgentConfigService;
    @Resource
    AgentConfigMapper agentConfigMapper;
    @Resource
    ServiceInfoService serviceInfoService;

    @GetMapping("/listPage")
    public Result listPage(@ParameterObject AgentConfigQuery agentConfigQuery) {
        Page<AgentConfig> list = agentConfigMapper.queryPage(agentConfigQuery, agentConfigMapper.getPage());
        return Result.success(list);
    }

    @GetMapping("/list")
    @Operation(summary = "获取服务器列表")
    public Result<List<AgentConfigVo>> list(@ParameterObject AgentConfigQuery agentConfigQuery) {
        return Result.success(iAgentConfigService.list(agentConfigQuery));
    }

    @PostMapping("/save")
    public Result save(@RequestBody AgentConfig agentConfig) {
        if (agentConfig.getServiceId() != null) {
            ServiceInfo serviceInfo = serviceInfoService.getById(agentConfig.getServiceId());
            agentConfig.setServiceName(serviceInfo.getName());
        }
        iAgentConfigService.saveOrUpdate(agentConfig);
        return Result.success();
    }


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


    @PostMapping("/receiveClientMsg")
    @Operation(summary = "接收终端消息")
    public Result receiveClientMsg(@RequestBody ClientMsgForm clientMsgForm) {
        iAgentConfigService.receiveClientMsg(clientMsgForm);
        return Result.success();
    }

    @GetMapping("/getLogsByServiceId")
    @Operation(summary = "实时日志查询")
    public Result getLogsByServiceId(@ParameterObject LogCmdForm logCmdForm) {
        String log = iAgentConfigService.getLogsByServiceId(logCmdForm);
        return Result.success(log);
    }

    @GetMapping("/handleAgentByServiceId/{serviceId}")
    public Result handleAgentByServiceId(@PathVariable Integer serviceId, @RequestParam String cmd) {
        iAgentConfigService.handleAgentByServiceId(serviceId, cmd);
        return Result.success();
    }


    @GetMapping("/handleAgent/{agentId}")
    public Result handleAgent(@PathVariable Long agentId, @RequestParam String cmd) {
        iAgentConfigService.handleAgent(agentId, cmd);
        return Result.success();
    }


    @DeleteMapping("/delete/{ids}")
    public Result delete(@PathVariable Integer ids) {
        iAgentConfigService.removeById(ids);
        return Result.success();
    }
}
