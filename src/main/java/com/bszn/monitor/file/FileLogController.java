package com.bszn.monitor.file;

import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigService;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.monitor.service.ServiceInfo;
import com.bszn.monitor.service.ServiceInfoService;
import com.bszn.system.common.result.Result;
import com.bszn.system.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fileLog")
@RequiredArgsConstructor
@Slf4j
public class FileLogController {

    @Resource
    AgentConfigService agentConfigService;
    @Resource
    IMsgService iMsgService;

    @Resource
    ServiceInfoService serviceInfoService;

    @GetMapping("/analyzerLog")
    @Operation(summary = "分析日志")
    public Result analyzer(@RequestParam Long agentId, @RequestParam String logFilePath) {
        AgentConfig agentConfig = agentConfigService.getById(agentId);
        AgentConfig jump = agentConfigService.getjumpServers(agentConfig.getServiceId());
        String cmd = String.format("curl -F \"file=@%s\" http://%s:18080/pyApi/log/analyzer", logFilePath, jump.getHostname());
        String cmdResult = iMsgService.sendCMDMsgAndResponse(
                SecurityUtils.getUserId(),
                agentId,
                cmd,
                30
        );
        return Result.success(cmdResult);
    }

    @GetMapping("/listFiles")
    @Operation(summary = "日志列表")
    public Result<List<FileInfo>> listFiles(@RequestParam("agentId") Long agentId, @RequestParam(value = "path",
            required = false) String path) {
        try {
            Long userId = SecurityUtils.getUserId();
            AgentConfig agentConfig = agentConfigService.getById(agentId);
            if (agentConfig == null) {
                return Result.failed("Agent不存在");
            }
            ServiceInfo serviceInfo = serviceInfoService.getById(agentConfig.getServiceId());
            if (StringUtils.isEmpty(path)) {
                path = serviceInfo.getWorkPath();
            }
            // 构建查看目录的命令
            String cmd = String.format("ls -l %s", path);
            // 发送命令获取文件列表
            String result = iMsgService.sendCMDMsgAndResponse(userId, agentId, cmd, 30);
            // 解析结果
            List<FileInfo> fileList = FileUtils.parseLsResult(result);
            return Result.success(fileList);
        } catch (Exception e) {
            log.error("查看文件列表失败", e);
            return Result.failed("查看文件列表失败: " + e.getMessage());
        }
    }
}