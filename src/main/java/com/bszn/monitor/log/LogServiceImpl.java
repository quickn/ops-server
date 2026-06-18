package com.bszn.monitor.log;

import com.alibaba.fastjson.JSONObject;
import com.bszn.monitor.agent.Agent;
import com.bszn.monitor.agent.AgentMapper;
import com.bszn.monitor.cmd.LogCmdForm;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.monitor.service.ServiceInfo;
import com.bszn.monitor.service.ServiceInfoService;
import com.bszn.mq.MsgResult;
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
                list.add(iMsgService.sendTaskMsgResponse(agentConfig.getId(), jsonObject.toJSONString(), logCmdForm.getTimeout()));
            }
        } else {
            jsonObject.put("filePath", logCmdForm.getFilePath());
            list.add(iMsgService.sendTaskMsgResponse(logCmdForm.getAgentId(), jsonObject.toJSONString(), logCmdForm.getTimeout()));
        }
        return list;
    }

    public String getLogsByServiceId(LogCmdForm logCmdForm) {
        ServiceInfo serviceInfo = serviceInfoService.getById(logCmdForm.getServiceId());
        List<Agent> agentConfigs = this.agentMapper.getByServiceId(logCmdForm.getServiceId(), logCmdForm.getDockerName());
        StringBuffer logs = new StringBuffer();
        for (Agent agentConfig : agentConfigs) {
            if (StringUtils.isEmpty(logCmdForm.getCmd())) {
                if (StringUtils.isEmpty(logCmdForm.getDockerName())) {
                    throw new BusinessException("dockerName不能为空");
                }
                StringBuilder cmd = new StringBuilder();
                if (StringUtils.isNotEmpty(logCmdForm.getKeyword())) {
                    cmd.append("cat");
                } else {
                    cmd.append("tail -n200 ");
                }
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
                if (StringUtils.isNotEmpty(logCmdForm.getKeyword())) {
                    cmd.append("|grep ");
                    if (StringUtils.isNotEmpty(logCmdForm.getGrepPara()) && StringUtils.isEmpty(logCmdForm.getKeyword1())) {
                        cmd.append(logCmdForm.getGrepPara());
                        cmd.append(" ");
                    }
                    cmd.append("'" + logCmdForm.getKeyword() + "'");
                }
                if (StringUtils.isNotEmpty(logCmdForm.getKeyword1())) {
                    cmd.append("|grep ");
                    if (StringUtils.isNotEmpty(logCmdForm.getGrepPara())) {
                        cmd.append(logCmdForm.getGrepPara());
                        cmd.append(" ");
                    }
                    cmd.append("'" + logCmdForm.getKeyword1() + "'");
                }
                logs.append("<div class='hostname'>");
                logs.append(agentConfig.getHostname() + "\n");
                logs.append("</div>");
                logs.append("\n");
                logs.append(iMsgService.sendCMDMsgAndRawResponse(null, agentConfig.getId(), cmd.toString(), logCmdForm.getTimeout()));
                logs.append("\n");
            } else {
                logs.append(iMsgService.sendCMDMsgAndRawResponse(null, agentConfig.getId(), logCmdForm.getCmd(), logCmdForm.getTimeout()));
            }
        }
        return logs.toString();
    }

}
