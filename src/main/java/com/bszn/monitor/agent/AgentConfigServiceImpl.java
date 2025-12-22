package com.bszn.monitor.agent;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bszn.monitor.cmd.ClientMsgForm;
import com.bszn.monitor.cmd.LogCmdForm;
import com.bszn.monitor.constant.MonitorCmdC;
import com.bszn.monitor.msg.IMsgService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
@Service
@Slf4j
public class AgentConfigServiceImpl extends ServiceImpl<AgentConfigMapper, AgentConfig>
        implements AgentConfigService {

    private final CopyOptions copyOption = CopyOptions.create(null, true);


    @Value("${upgradeClient.url}")
    String upgradeClientUrl;

    @Resource
    IMsgService iMsgService;

    @Override
    public AgentConfig getByMac(String mac, String hostname) {
        AgentConfig config = this.baseMapper.selectOne(Wrappers.<AgentConfig>lambdaQuery().eq(AgentConfig::getMac, mac));
        if (config == null) {
            config = new AgentConfig();
            config.setMac(mac);
            config.setHostname(hostname);
            this.baseMapper.insert(config);
            return null;
        }
        if (config.getServiceId() == null) {
            return config;
        }
        AgentConfig configCommon = this.baseMapper.selectOne(Wrappers.<AgentConfig>lambdaQuery()
                .eq(AgentConfig::getServiceId, config.getServiceId()).isNull(AgentConfig::getMac));
        if (configCommon == null) {
            return config;
        }
        //null,表示无限制，true表示若父类中属性值为空则忽略，不传给子类
        BeanUtil.copyProperties(config, configCommon, copyOption);
        return configCommon;
    }

    @Override
    public AgentConfig getServiceIdAndHostname(Integer serviceId, String hostname) {
        AgentConfig config = this.baseMapper.selectOne(Wrappers.<AgentConfig>lambdaQuery().eq(AgentConfig::getServiceId, serviceId)
                .eq(AgentConfig::getHostname, hostname));
        AgentConfig configCommon = this.baseMapper.selectOne(Wrappers.<AgentConfig>lambdaQuery()
                .eq(AgentConfig::getServiceId, config.getServiceId())
                .eq(AgentConfig::getIsMonitor, true).isNull(AgentConfig::getMac));
        if (configCommon == null) {
            return config;
        }
        BeanUtil.copyProperties(config, configCommon, copyOption);
        return configCommon;
    }

    @Override
    public AgentConfig getByServiceIdAndHost(Integer serviceId, String hostname) {
        return this.baseMapper.getByServiceIdAndHost(serviceId, hostname);
    }


    @Override
    public String getLogsByServiceId(LogCmdForm logCmdForm) {
        List<AgentConfig> agentConfigs = this.baseMapper.getByServiceId(logCmdForm.getServiceId(), logCmdForm.getDockerName());
        StringBuffer logs = new StringBuffer();
        for (AgentConfig agentConfig : agentConfigs) {
            if (StringUtils.isEmpty(logCmdForm.getCmd())) {
                StringBuilder cmd = new StringBuilder();
                if (StringUtils.isNotEmpty(logCmdForm.getKeyword())) {
                    cmd.append("cat");
                } else {
                    cmd.append("tail -n200 ");
                }
                cmd.append(" /home/park/logs/");
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
                    if (StringUtils.isNotEmpty(logCmdForm.getGrepPara())) {
                        cmd.append(logCmdForm.getGrepPara());
                        cmd.append(" ");
                    }
                    cmd.append("'" + logCmdForm.getKeyword() + "'");
                }
                logs.append(agentConfig.getHostname() + "\n");
                logs.append(iMsgService.sendCMDMsgAndRawResponse(agentConfig.getId(), cmd.toString(), 20));
            } else {
                logs.append(iMsgService.sendCMDMsgAndRawResponse(agentConfig.getId(), logCmdForm.getCmd(), 20));
            }
        }
        return logs.toString();
    }

    @Override
    public void upgradeClientByServiceId(Integer serviceId) {
        List<AgentConfig> agentConfigs = this.baseMapper.getListByServiceId(serviceId);
        for (AgentConfig agentConfig : agentConfigs) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOnce("handle", "upgradeClient");
            if (serviceId == 1) {
                jsonObject.putOnce("url", "http://192.168.10.10:82/client/bs-agent-release.jar");
            } else {
                jsonObject.putOnce("url", upgradeClientUrl);
            }
        }
    }

    @Override
    public void receiveClientMsg(ClientMsgForm clientMsgForm) {
        if (MonitorCmdC.updateClientVersion.equals(clientMsgForm.getCmd())) {
            AgentConfig agentConfig = new AgentConfig();
            agentConfig.setId(clientMsgForm.getAgentId());
            agentConfig.setClientVersion(clientMsgForm.getData());
            this.baseMapper.updateById(agentConfig);
        }
    }
}
