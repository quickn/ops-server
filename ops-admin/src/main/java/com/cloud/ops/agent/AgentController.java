package com.cloud.ops.agent;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.constant.MonitorMsgType;
import com.cloud.ops.msg.IMsgService;
import com.cloud.ops.service.ServiceInfo;
import com.cloud.ops.service.ServiceInfoService;
import com.cloud.mq.MsgResult;
import com.cloud.ops.cmd.ClientMsgForm;
import com.cloud.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@Tag(name = "Agent管理")
@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController {
    @Resource
    AgentService iAgentConfigService;
    @Resource
    AgentMapper agentMapper;
    @Resource
    ServiceInfoService serviceInfoService;

    @Resource
    IMsgService iMsgService;

    @GetMapping("/listPage")
    public Result listPage(@ParameterObject AgentQuery agentQuery) {
        Page<Agent> list = agentMapper.queryPage(agentQuery, agentMapper.getPage());
        return Result.success(list);
    }

    @GetMapping("/list")
    @Operation(summary = "获取服务器列表")
    @McpTool(name = "agentList", description = "agent列表")
    public Result<List<AgentVo>> list(@ParameterObject AgentQuery agentQuery) {
        log.info("list: {}", agentQuery);
        return Result.success(iAgentConfigService.list(agentQuery));
    }

    @PostMapping("/save")
    public Result save(@RequestBody Agent agentConfig) {
        if (agentConfig.getServiceId() != null) {
            ServiceInfo serviceInfo = serviceInfoService.getById(agentConfig.getServiceId());
            agentConfig.setServiceName(serviceInfo.getName());
        }
        iAgentConfigService.saveOrUpdate(agentConfig);
        iAgentConfigService.handleAgent(agentConfig.getId(), "restart");
        return Result.success();
    }


    @PostMapping("/getConf")
    @Operation(summary = "获取配置文件")
    public JSONObject getConf(@RequestBody JSONObject jsonObject) {
        String mac = jsonObject.getStr("mac");
        String hostname = jsonObject.getStr("hostname");
        Agent agentConfig = iAgentConfigService.getByMac(mac, hostname);
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


    @GetMapping("/sendCmd")
    @Operation(summary = "发送指令")
    public Result sendCmd(@RequestParam Long agentId, @RequestParam String cmd,
                          @RequestParam(required = false) Integer timeout) {
        if (timeout == null) {
            timeout = 60;
        }
        if (timeout > 300) {
            timeout = 300;
        }
        String msgType = MonitorMsgType.CMD;
        if (cmd.startsWith("{")) {
            msgType = MonitorMsgType.TASK;
        }
        return Result.success(iMsgService.sendMsgAndResponse(agentId, "发送指令", cmd, msgType, timeout));
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

    @PostMapping("/batchHandleAgent")
    public Result batchHandleAgent(@RequestBody JSONObject jsonObject) {
        Long[] ids = jsonObject.getJSONArray("ids").toArray(new Long[0]);
        String cmd = jsonObject.getStr("cmd");
        List<MsgResult> result = new ArrayList<>();
        for (Long agentId : ids) {
            MsgResult msgResult = iAgentConfigService.handleAgent(agentId, cmd);
            result.add(msgResult);
        }
        return Result.success(result);
    }


    @DeleteMapping("/delete/{ids}")
    public Result delete(@PathVariable Integer ids) {
        iAgentConfigService.removeById(ids);
        return Result.success();
    }
}
