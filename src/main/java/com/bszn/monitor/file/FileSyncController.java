package com.bszn.monitor.file;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bszn.monitor.agent.Agent;
import com.bszn.monitor.agent.AgentService;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.system.common.result.Result;
import com.bszn.system.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/file-sync")
@RequiredArgsConstructor
@Slf4j
public class FileSyncController {

    private final IMsgService msgService;

    private final AgentService agentConfigService;

    @Value("${file.upload.file-path}")
    private String filePath;

    @Value("${file.upload.down-path}")
    private String downPath;


    /**
     * 获取所有跳板机列表
     */
    @GetMapping("/jump-servers")
    public Result<List<Agent>> getJumpServers() {
        try {
            List<Agent> jumpServers = agentConfigService.list(Wrappers.<Agent>lambdaQuery().eq(Agent::getIsJumpServer, true));
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
    public Result<List<Agent>> getAllServers(@RequestParam(required = false) String hostname,
                                             @RequestParam(required = false) Integer serviceId) {
        try {
            LambdaQueryWrapper<Agent> queryWrapper = Wrappers.lambdaQuery();
            if (StrUtil.isNotBlank(hostname)) {
                queryWrapper.like(Agent::getHostname, hostname);
            }
            if (serviceId != null) {
                queryWrapper.eq(Agent::getServiceId, serviceId);
            }
            queryWrapper.orderByDesc(Agent::getId);
            List<Agent> servers = agentConfigService.list(queryWrapper);
            return Result.success(servers);
        } catch (Exception e) {
            log.error("获取服务器列表失败", e);
            return Result.failed("获取服务器列表失败");
        }
    }

    /**
     * 查看服务器目录文件列表
     */


    /**
     * 上传文件到跳板机
     */
    @PostMapping("/upload")
    public Result<String> uploadToJumpServer(@RequestParam("file") MultipartFile file,
                                             @RequestParam("agentId") Long agentId,
                                             @RequestParam("destPath") String destPath) {
        try {
            Long userId = SecurityUtils.getUserId();
            if (file.isEmpty()) {
                return Result.failed("文件不能为空");
            }
            Agent jumpServer = agentConfigService.getById(agentId);
            if (jumpServer == null || !jumpServer.getIsJumpServer()) {
                return Result.failed("指定的服务器不是跳板机");
            }
            // 创建上传目录
            File uploadDir = new File(filePath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            // 保存文件
            String saveFileName = file.getOriginalFilename();
            String filePath = this.filePath + File.separator + saveFileName;
            file.transferTo(new File(filePath));
            // 生成下载URL
            String downloadUrl = String.format(downPath + "/%s", saveFileName);

            // 构建上传命令 - 使用curl下载文件
            String uploadCmd = buildUploadCommand(downloadUrl, destPath, saveFileName);

            log.info("执行上传命令到跳板机 {}: {}", jumpServer.getHostname(), uploadCmd);
            String result = msgService.sendCMDMsgAndResponse(agentId, uploadCmd, 120);
            // 上传完成后删除临时文件
            try {
                File uploadedFile = new File(filePath);
                if (uploadedFile.exists()) {
                    uploadedFile.delete();
                }
            } catch (Exception e) {
                log.warn("删除临时文件失败: {}", filePath, e);
            }
            return Result.success("文件上传成功: " + saveFileName + "\n跳板机响应: " + result);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.failed("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 构建上传命令（使用curl下载文件） - 简洁版
     */
    private String buildUploadCommand(String downloadUrl, String destPath, String fileName) {
        // 使用 -p 参数确保目录存在，-p 参数在目录已存在时不会报错
        return String.format(
                "mkdir -p %s && curl -f -o %s/%s '%s' && echo '文件 %s 已上传到 %s'",
                destPath, destPath, fileName, downloadUrl, fileName, destPath
        );
    }

    /**
     * 直接使用rsync命令同步
     */
    @PostMapping("/sync-with-command")
    public Result<String> syncWithCommand(@RequestBody CMDRequest cmdRequest) {
        try {
            Agent jumpServer = agentConfigService.getById(cmdRequest.getAgentId());
            if (jumpServer == null || !jumpServer.getIsJumpServer()) {
                return Result.failed("指定的服务器不是跳板机");
            }
            // 直接在跳板机上执行rsync命令
            String result = msgService.sendCMDMsgAndResponse(cmdRequest.getAgentId(), cmdRequest.getCommand(), 300);
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
            List<Agent> targetServers = agentConfigService.listByIds(request.getTargetServers());
            if (targetServers.isEmpty()) {
                return Result.failed("未找到选中的服务器信息");
            }
            // 构建组合命令
            StringBuilder command = new StringBuilder();
            for (int i = 0; i < targetServers.size(); i++) {
                Agent server = targetServers.get(i);
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
}