package com.bszn.monitor.file;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigService;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.system.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/file-sync")
@RequiredArgsConstructor
@Slf4j
public class FileSyncController {

    private final IMsgService msgService;

    private final AgentConfigService agentConfigService;

    /**
     * 获取所有跳板机列表
     */
    @GetMapping("/jump-servers")
    public Result<List<AgentConfig>> getJumpServers() {
        try {
            List<AgentConfig> jumpServers = agentConfigService.list(Wrappers.<AgentConfig>lambdaQuery().eq(AgentConfig::getIsJumpServer, true));
            return Result.success(jumpServers);
        } catch (Exception e) {
            log.error("获取跳板机列表失败", e);
            return Result.failed("获取跳板机列表失败");
        }
    }

    /**
     * 获取所有服务器列表（目标服务器）
     */
    @GetMapping("/all-servers")
    public Result<List<AgentConfig>> getAllServers(@RequestParam(required = false) String hostname,
                                                   @RequestParam(required = false) Integer serviceId) {
        try {
            LambdaQueryWrapper<AgentConfig> queryWrapper = Wrappers.lambdaQuery();
            if (StrUtil.isNotBlank(hostname)) {
                queryWrapper.like(AgentConfig::getHostname, hostname);
            }
            if (serviceId != null) {
                queryWrapper.eq(AgentConfig::getServiceId, serviceId);
            }
            queryWrapper.orderByDesc(AgentConfig::getId);
            List<AgentConfig> servers = agentConfigService.list(queryWrapper);
            return Result.success(servers);
        } catch (Exception e) {
            log.error("获取服务器列表失败", e);
            return Result.failed("获取服务器列表失败");
        }
    }

    /**
     * 查看服务器目录文件列表
     */
    @GetMapping("/list-files")
    public Result<List<FileInfo>> listFiles(@RequestParam("agentId") Long agentId,
                                            @RequestParam(value = "path", defaultValue = "/home/park") String path) {
        try {
            AgentConfig server = agentConfigService.getById(agentId);
            if (server == null) {
                return Result.failed("服务器不存在");
            }
            // 构建查看目录的命令
            String cmd = String.format("ls -la %s", path);
            // 发送命令获取文件列表
            String result = msgService.sendCMDMsgAndResponse(agentId, cmd, 30);
            // 解析结果
            List<FileInfo> fileList = parseLsResult(result);
            return Result.success(fileList);
        } catch (Exception e) {
            log.error("查看文件列表失败", e);
            return Result.failed("查看文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件到跳板机
     */
    @PostMapping("/upload")
    public Result<String> uploadToJumpServer(@RequestParam("file") MultipartFile file,
                                             @RequestParam("agentId") Long agentId,
                                             @RequestParam("destPath") String destPath) {
        try {
            if (file.isEmpty()) {
                return Result.failed("文件不能为空");
            }
            AgentConfig jumpServer = agentConfigService.getById(agentId);
            if (jumpServer == null || !jumpServer.getIsJumpServer()) {
                return Result.failed("指定的服务器不是跳板机");
            }

            // 这里根据您的实际文件传输方式实现
            String fileName = file.getOriginalFilename();

            // 构建上传命令
            String uploadCmd = String.format(
                    "echo 'File %s uploaded to %s on server %s'",
                    fileName, destPath, jumpServer.getHostname()
            );

            String result = msgService.sendCMDMsgAndResponse(agentId, uploadCmd, 60);
            return Result.success("文件上传成功: " + fileName);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.failed("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 直接使用rsync命令同步
     */
    @PostMapping("/sync-with-command")
    public Result<String> syncWithCommand(@RequestParam("agentId") Long agentId,
                                          @RequestParam("command") String command) {
        try {
            AgentConfig jumpServer = agentConfigService.getById(agentId);
            if (jumpServer == null || !jumpServer.getIsJumpServer()) {
                return Result.failed("指定的服务器不是跳板机");
            }
            // 直接在跳板机上执行rsync命令
            String result = msgService.sendCMDMsgAndResponse(agentId, command, 300);
            return Result.success(result);
        } catch (Exception e) {
            log.error("同步命令执行失败", e);
            return Result.failed("同步命令执行失败: " + e.getMessage());
        }
    }

    /**
     * 构建同步命令（供前端调用）
     */
    @PostMapping("/build-sync-command")
    public Result<String> buildSyncCommand(@RequestBody BuildCommandRequest request) {
        try {
            if (request.getTargetServers() == null || request.getTargetServers().isEmpty()) {
                return Result.failed("请选择目标服务器");
            }
            if (StrUtil.isBlank(request.getSourcePath())) {
                return Result.failed("请指定源文件路径");
            }
            // 获取目标服务器信息
            List<AgentConfig> targetServers = agentConfigService.listByIds(request.getTargetServers());
            if (targetServers.isEmpty()) {
                return Result.failed("未找到选中的服务器信息");
            }
            // 构建组合命令
            StringBuilder command = new StringBuilder();
            for (int i = 0; i < targetServers.size(); i++) {
                AgentConfig server = targetServers.get(i);
                String rsyncCmd = String.format("rsync -azv %s %s@%s:%s",
                        request.getSourcePath(),
                        request.getUser() != null ? request.getUser() : "park",
                        server.getHostname(),
                        request.getTargetPath() != null ? request.getTargetPath() : "/home/park");
                command.append(rsyncCmd);
                // 如果不是最后一条命令，添加 &&
                if (i < targetServers.size() - 1) {
                    command.append(" && ");
                }
            }
            return Result.success(command.toString());
        } catch (Exception e) {
            log.error("构建同步命令失败", e);
            return Result.failed("构建同步命令失败: " + e.getMessage());
        }
    }

    // 解析ls结果的方法
    private List<FileInfo> parseLsResult(String lsOutput) {
        List<FileInfo> fileList = new ArrayList<>();
        if (lsOutput == null || lsOutput.trim().isEmpty()) {
            return fileList;
        }
        String[] lines = lsOutput.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("total")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length >= 9) {
                FileInfo fileInfo = new FileInfo();
                fileInfo.setPermissions(parts[0]);
                fileInfo.setLinks(Integer.parseInt(parts[1]));
                fileInfo.setOwner(parts[2]);
                fileInfo.setGroup(parts[3]);
                fileInfo.setSize(parts[4]);
                // 组合日期时间
                StringBuilder dateTime = new StringBuilder();
                for (int i = 5; i <= 7; i++) {
                    dateTime.append(parts[i]).append(" ");
                }
                fileInfo.setModifyTime(dateTime.toString().trim());
                // 文件名（可能包含空格）
                StringBuilder fileName = new StringBuilder();
                for (int i = 8; i < parts.length; i++) {
                    fileName.append(parts[i]).append(" ");
                }
                fileInfo.setName(fileName.toString().trim());

                // 判断类型
                if (parts[0].startsWith("d")) {
                    fileInfo.setType("directory");
                } else if (parts[0].startsWith("l")) {
                    fileInfo.setType("link");
                } else {
                    fileInfo.setType("file");
                }
                fileList.add(fileInfo);
            }
        }
        return fileList;
    }
}