package com.youlai.agent;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
@Service
@Slf4j
public class AgentConfigServiceImpl extends ServiceImpl<AgentConfigMapper, AgentConfig>
        implements IAgentConfigService {

    private final CopyOptions copyOption = CopyOptions.create(null, true);

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
}
