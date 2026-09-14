package com.bszn.monitor.warnLog;

import com.alibaba.fastjson2.JSONObject;
import com.bszn.monitor.agent.Agent;
import com.bszn.monitor.agent.AgentMapper;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.monitor.service.ServiceInfo;
import com.bszn.monitor.service.ServiceInfoService;
import com.bszn.mq.MsgResult;
import com.bszn.ops.cmd.LogCmdForm;
import com.bszn.system.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LogServiceImpl implements ILogService {

    @Resource
    private IMsgService iMsgService;
    @Resource
    private AgentMapper agentMapper;

    @Resource
    ServiceInfoService serviceInfoService;


    public List<MsgResult> analysis(LogCmdForm logCmdForm) {
        List<MsgResult> list = new ArrayList<>();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("handle", "logAnalysisHandle");
        jsonObject.put("action", logCmdForm.getAction());
        jsonObject.put("startTime", logCmdForm.getStartTime());
        jsonObject.put("endTime", logCmdForm.getEndTime());
        String command = "日志分析";
        if (StringUtils.isEmpty(logCmdForm.getFilePath())) {
            if (StringUtils.isEmpty(logCmdForm.getDockerName())) {
                throw new BusinessException("dockerName不能为空");
            }
            ServiceInfo serviceInfo = serviceInfoService.getById(logCmdForm.getServiceId());
            List<Agent> agentConfigs = agentMapper.getByServiceId(logCmdForm.getServiceId(), logCmdForm.getDockerName());
            for (Agent agentConfig : agentConfigs) {
                StringBuilder cmd = new StringBuilder();
                cmd.append(" ");
                cmd.append(serviceInfo.getWorkPath().trim());
                cmd.append("/logs/");
                cmd.append(logCmdForm.getDockerName());
                cmd.append("/");
                if (StringUtils.isNotEmpty(logCmdForm.getCreateDate())) {
                    cmd.append(logCmdForm.getLogLevel() + "/");
                }
                cmd.append(logCmdForm.getLogLevel());
                if (StringUtils.isEmpty(logCmdForm.getCreateDate())) {
                    cmd.append(".log");
                } else {
                    String createDate = logCmdForm.getCreateDate().substring(0, 10);
                    cmd.append("-" + createDate + ".*.log");
                }
                jsonObject.put("filePath", cmd.toString());
                list.add(iMsgService.sendTaskMsgResponse(agentConfig.getId(), command, jsonObject.toJSONString(), logCmdForm.getTimeout()));
            }
        } else {
            jsonObject.put("filePath", logCmdForm.getFilePath());
            list.add(iMsgService.sendTaskMsgResponse(logCmdForm.getAgentId(), command, jsonObject.toJSONString(), logCmdForm.getTimeout()));
        }
        return list;
    }

    public String getLogsByServiceId(LogCmdForm logCmdForm) {
        ServiceInfo serviceInfo = serviceInfoService.getById(logCmdForm.getServiceId());
        List<Agent> agentConfigs = this.agentMapper.getByServiceId(logCmdForm.getServiceId(), logCmdForm.getDockerName());
        StringBuilder logs = new StringBuilder();
        String command = "查询日志文件";
        for (Agent agentConfig : agentConfigs) {
            if (StringUtils.isEmpty(logCmdForm.getCmd())) {
                if (StringUtils.isEmpty(logCmdForm.getDockerName())) {
                    throw new BusinessException("dockerName不能为空");
                }
                StringBuilder cmd = new StringBuilder();
                if (StringUtils.isEmpty(logCmdForm.getKeyword()) && StringUtils.isEmpty(logCmdForm.getKeyword1())) {
                    cmd.append("cat ");
                }
                if (StringUtils.isNotEmpty(logCmdForm.getKeyword())) {
                    cmd.append("grep ");
                    if (StringUtils.isNotEmpty(logCmdForm.getGrepPara()) && StringUtils.isEmpty(logCmdForm.getKeyword1())) {
                        cmd.append(logCmdForm.getGrepPara());
                        cmd.append(" ");
                    }
                    cmd.append("'").append(logCmdForm.getKeyword()).append("' ");
                }

                String logPath = serviceInfo.getWorkPath().trim() + "/logs/" + logCmdForm.getDockerName() + "/";
                if (StringUtils.isNotEmpty(logCmdForm.getCreateDate())) {
                    logPath = logPath + logCmdForm.getLogLevel() + "/";
                }

                if (StringUtils.isEmpty(logCmdForm.getCreateDate())) {
                    cmd.append(logPath);
                    cmd.append(logCmdForm.getLogLevel());
                    cmd.append(".log");
                } else {
                    if (StringUtils.isNotEmpty(logCmdForm.getStartHourMinute()) || StringUtils.isNotEmpty(logCmdForm.getEndHourMinute())) {
                        String startHm = StringUtils.isNotEmpty(logCmdForm.getStartHourMinute()) ? logCmdForm.getStartHourMinute() : "00:00";
                        String endHm = StringUtils.isNotEmpty(logCmdForm.getEndHourMinute()) ? logCmdForm.getEndHourMinute() : "23:59";
                        // 查询当前目录下所有文件名称，并根据文件名称中的时间戳范围筛选
                        String lsCmd = String.format(
                                "find %s -maxdepth 1 -name \"%s-*.log\" -newermt \"%s %s\" ! -newermt \"%s %s:59\" -printf \"%%f\\\\n\"", logPath,
                                logCmdForm.getLogLevel(), logCmdForm.getCreateDate(), startHm, logCmdForm.getCreateDate(), endHm);
                        MsgResult msgResult = iMsgService.sendCMDMsgAndRawResponse(agentConfig.getId(), command, lsCmd, logCmdForm.getTimeout(), logCmdForm.getDockerName());
                        if (StringUtils.isEmpty(msgResult.getData())) {
                            throw new BusinessException("未找到日志文件");
                        }
                        String[] fileNames = msgResult.getData().split("\n");
                        for (String fileName : fileNames) {
                            cmd.append(logPath);
                            cmd.append(fileName);
                            cmd.append(" ");
                        }
                        //cmd.append("|awk '{split($2,t,\":\"); hm=t[1]\":\"t[2]; if(hm>=\"" + startHm + "\" && hm<=\"" + endHm + "\") print}'");
                    } else {
                        cmd.append(logPath);
                        cmd.append(logCmdForm.getLogLevel());
                        String createDate = logCmdForm.getCreateDate().substring(0, 10);
                        cmd.append("-").append(createDate).append(".*.log");
                    }
                }

                if (StringUtils.isNotEmpty(logCmdForm.getKeyword1())) {
                    cmd.append("| grep ");
                    if (StringUtils.isNotEmpty(logCmdForm.getGrepPara())) {
                        cmd.append(logCmdForm.getGrepPara());
                        cmd.append(" ");
                    }
                    cmd.append("'").append(logCmdForm.getKeyword1()).append("' ");
                }

                if (logCmdForm.getLimit() > 0) {
                    cmd.append(" | tail -n");
                    cmd.append(logCmdForm.getLimit());
                }

                logs.append("<div class='hostname'>");
                logs.append(agentConfig.getHostname()).append("\n");
                logs.append("</div>");
                logs.append("\n");
                if (logCmdForm.isOnlyCount()) {
                    cmd.append("|wc -l");
                }
                logs.append(iMsgService.sendCMDMsgAndRawResponse(agentConfig.getId(), command, cmd.toString(), logCmdForm.getTimeout(), logCmdForm.getDockerName()));
                logs.append("\n");
            } else {
                logs.append(iMsgService.sendCMDMsgAndRawResponse(agentConfig.getId(), command, logCmdForm.getCmd(), logCmdForm.getTimeout(), logCmdForm.getDockerName()));
            }
        }
        return logs.toString();
    }

    @Override
    public MsgResult detail(LogCmdForm logCmdForm) {
        if (StringUtils.isEmpty(logCmdForm.getFilePath())) {
            throw new BusinessException("filePath不能为空");
        }
        if (StringUtils.isEmpty(logCmdForm.getKeyword())) {
            throw new BusinessException("关键字不能为空");
        }
        StringBuilder cmd = new StringBuilder();
        cmd.append("cat ");
        cmd.append(logCmdForm.getFilePath());
        cmd.append("|grep ");
        if (StringUtils.isNotEmpty(logCmdForm.getGrepPara()) && StringUtils.isEmpty(logCmdForm.getKeyword1())) {
            cmd.append(logCmdForm.getGrepPara());
            cmd.append(" ");
        }
        cmd.append("'" + logCmdForm.getKeyword() + "'");
        return iMsgService.sendCMDMsgAndRawResponse(logCmdForm.getAgentId(), "日志详情", cmd.toString(), logCmdForm.getTimeout(),logCmdForm.getDockerName());
    }

}
