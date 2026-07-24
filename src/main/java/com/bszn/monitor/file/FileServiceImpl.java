package com.bszn.monitor.file;

import com.bszn.monitor.agent.Agent;
import com.bszn.monitor.agent.AgentQuery;
import com.bszn.monitor.agent.AgentService;
import com.bszn.monitor.agent.AgentVo;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.system.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    private final IMsgService msgService;

    @Resource
    AgentService agentService;

    @Override
    public Boolean syncFileByJumpServer(SyncFileParam syncFileParam) {
        if (StringUtils.isEmpty(syncFileParam.getTargetPath())) {
            throw new RuntimeException("目标路径不能为空");
        }
        Path parentPath = Paths.get(syncFileParam.getTargetPath()).getParent();
        if (parentPath == null) {
            throw new RuntimeException("文件路径不能为空");
        }
        // 获取目录路径的上级目录
        String targetPath = parentPath.toString();
        // 获取跳板机
        Agent jumpServerAgent = null;
        if (syncFileParam.getJumpServiceId() != null) {
            jumpServerAgent = agentService.getjumpServers(syncFileParam.getJumpServiceId());
        }
        if (jumpServerAgent == null && syncFileParam.getJumpAgentId() != null) {
            jumpServerAgent = agentService.getById(syncFileParam.getJumpAgentId());
        }
        if (Objects.isNull(jumpServerAgent)) {
            throw new BusinessException("该环境没有设置跳板机！");
        }
        // 获取跳板机
        if (syncFileParam.getJumpServiceId() != null) {
            jumpServerAgent = agentService.getjumpServers(syncFileParam.getJumpServiceId());
        }
        if (jumpServerAgent == null && syncFileParam.getJumpAgentId() != null) {
            jumpServerAgent = agentService.getById(syncFileParam.getJumpAgentId());
        }
        if (Objects.isNull(jumpServerAgent)) {
            throw new BusinessException("该环境没有设置跳板机！");
        }
        // 手动同步没有源服务
        if (Objects.nonNull(syncFileParam.getSourceAgentId())) {
            Agent agent = agentService.getById(syncFileParam.getSourceAgentId());
            if (agent == null) {
                throw new BusinessException("Agent不存在");
            }
            if (!agent.getOnline()) {
                throw new BusinessException("源服务Agent不在线");
            }
            // 源服务跟同步服务一样，无需同步
            if (agent.getServiceId().equals(jumpServerAgent.getServiceId())) {
                return true;
            }
        }
        String user = jumpServerAgent.getUser();
        if (StringUtils.isEmpty(user)) {
            user = "park";
        }
        StringBuilder result = new StringBuilder();
        if (syncFileParam.getSyncType() == 1) {
            // 从源服务器 rsync 到跳板机
            String rsyncCmd = String.format("rsync -azv -e 'ssh -o StrictHostKeyChecking=no' %s %s@%s:%s",
                    syncFileParam.getSourcePath().trim(),
                    user.trim(),
                    jumpServerAgent.getRemoteIp().trim(),
                    targetPath.trim() + File.separator);
            result.append(msgService.sendCMDMsgAndResponse(syncFileParam.getSourceAgentId(), "跳板机同步", rsyncCmd, 300))
                    .append(" === 第一段结果集结束 === ");
        }
        AgentQuery agentQuery = new AgentQuery(jumpServerAgent.getServiceId());
        agentQuery.setIsJumpServer(false);
        List<AgentVo> list = agentService.list(agentQuery);
        // 2. 跳板机 → 目标服务器
        if (!CollectionUtils.isEmpty(list)) {
            StringBuilder command = new StringBuilder();
            // 构建 rsync 命令：跳板机 → 每个目标服务器
            for (int i = 0; i < list.size(); i++) {
                AgentVo agentVo = list.get(i);
                String hostname = agentVo.getHostname();
                String rsyncCmd = String.format("rsync -azv  -e 'ssh -o StrictHostKeyChecking=no' %s %s@%s:%s",
                        syncFileParam.getTargetPath().trim(),
                        user.trim(),
                        hostname.trim(),
                        targetPath.trim() + File.separator);
                command.append(rsyncCmd);
                if (i < list.size() - 1) {
                    command.append(" && ");
                }
            }
            result.append(msgService.sendCMDMsgAndResponse(jumpServerAgent.getId(), "跳板机同步", command.toString(), 120));
            log.info("同步结果：{}", result);
        }
        return true;
    }
}