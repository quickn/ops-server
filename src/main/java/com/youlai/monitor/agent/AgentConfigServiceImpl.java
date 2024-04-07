package com.youlai.monitor.agent;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.monitor.cmd.CmdDataForm;
import com.youlai.monitor.cmd.LogCmdForm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
@Service
@Slf4j
public class AgentConfigServiceImpl extends ServiceImpl<AgentConfigMapper, AgentConfig>
        implements AgentConfigService {

    private final CopyOptions copyOption = CopyOptions.create(null, true);

    static Map<Long, String> cmdMap = new HashMap<>();


    @Value("${upgradeClient.url}")
    String upgradeClientUrl;

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
    public void sendCmd(Long agentId, String cmd) {
        cmdMap.put(agentId, cmd);
    }

    @Override
    public String getCmdById(Long id) {
        return cmdMap.get(id);
    }

    @Override
    public void removeCmdById(Long id) {
        cmdMap.remove(id);
    }

    @Override
    public AgentConfig getByServiceIdAndHost(Integer serviceId, String hostname) {
        return this.baseMapper.getByServiceIdAndHost(serviceId, hostname);
    }

    private Map<Long, String> cmdData = new HashMap();

    @Override
    public String getLogsByServiceId(LogCmdForm logCmdForm) {
        List<AgentConfig> agentConfigs = this.baseMapper.getByServiceId(logCmdForm.getServiceId(), logCmdForm.getDockerName());
        for (AgentConfig agentConfig : agentConfigs) {
            if (StringUtils.isEmpty(logCmdForm.getCmd())) {
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append("cat /home/park/logs/");
                stringBuffer.append(logCmdForm.getDockerName());
                stringBuffer.append("/");
                stringBuffer.append(logCmdForm.getLogLevel());
                if (StringUtils.isEmpty(logCmdForm.getCreateDate())) {
                    stringBuffer.append(".log");
                } else {
                    stringBuffer.append("-" + logCmdForm.getCreateDate() + ".*.log");
                }
                stringBuffer.append("|grep ");
                if (StringUtils.isNotEmpty(logCmdForm.getGrepPara())) {
                    stringBuffer.append(logCmdForm.getGrepPara());
                    stringBuffer.append(" ");
                }
                stringBuffer.append("'" + logCmdForm.getKeyword() + "'");
                cmdMap.put(agentConfig.getId(), stringBuffer.toString());
            } else {
                cmdMap.put(agentConfig.getId(), logCmdForm.getCmd());
            }
        }
        StringBuffer stringBuffer = new StringBuffer();
        long startTime = System.currentTimeMillis(); // 记录开始时间
        while (true) {
            // 检查是否已经超时
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime >= logCmdForm.getTimeout()) {
                break; // 超时退出循环
            }
            Iterator<AgentConfig> iterator = agentConfigs.iterator();
            while (iterator.hasNext()) {
                AgentConfig agentConfig = iterator.next();
                if (cmdData.get(agentConfig.getId()) != null) {
                    stringBuffer.append(cmdData.get(agentConfig.getId()));
                    cmdData.remove(agentConfig.getId());
                    iterator.remove();
                }
            }
            if (agentConfigs.size() == 0) {
                break;
            }
            // 可选择性让线程暂停一段时间，防止CPU全速运行
            try {
                Thread.sleep(100); // 暂停100毫秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 重新设置线程的中断状态
            }
        }
        return stringBuffer.toString();
    }

    @Override
    public void cmdData(CmdDataForm cmdDataForm) {
        cmdData.put(cmdDataForm.getAgentId(), cmdDataForm.getData());
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
            cmdMap.put(agentConfig.getId(), jsonObject.toString());
        }
    }
}
