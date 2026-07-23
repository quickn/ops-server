package com.bszn.monitor.agent;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bszn.constant.MonitorMsgType;
import com.bszn.monitor.cmd.ClientMsgForm;
import com.bszn.monitor.constant.MonitorCmdC;
import com.bszn.monitor.docker.DockerContainer;
import com.bszn.monitor.docker.DockerContainerMapper;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.system.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by Liuyun on 2023-07-26 11:16
 **/
@Service
@Slf4j
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent>
        implements AgentService {

    private final CopyOptions copyOption = CopyOptions.create(null, true);


    @Value("${upgradeClient.url}")
    String upgradeClientUrl;

    @Resource
    IMsgService iMsgService;

    @Resource
    DockerContainerMapper dockerContainerMapper;

    /**
     * 获取服务器列表
     *
     * @param dto 查询参数
     * @return 服务器列表
     */
    @Override
    public List<AgentVo> list(AgentQuery dto) {
        List<Agent> query = getBaseMapper().query(dto);
        // 设置部署状态
        if (StrUtil.isNotEmpty(dto.getDockerName())) {
            // 拿到容器
            List<DockerContainer> dockerContainers =
                    dockerContainerMapper.selectList(Wrappers.<DockerContainer>lambdaQuery().eq(DockerContainer::getServiceId, dto.getServiceId()).eq(DockerContainer::getNames,
                            dto.getDockerName()));
            // 设置状态
            if (CollUtil.isNotEmpty(dockerContainers)) {
                Map<String, DockerContainer> sourceMap = dockerContainers.stream()
                        .collect(Collectors.toMap(DockerContainer::getHostname, user -> user));
                return query.stream().map(agentConfig -> {
                    AgentVo agentConfigVo = new AgentVo(agentConfig);
                    if (sourceMap.containsKey(agentConfig.getHostname())) {
                        DockerContainer source = sourceMap.get(agentConfig.getHostname());
                        agentConfigVo.setUpdateTime(source.getUpdateTime());
                        agentConfigVo.setStatus(source.getStatus());
                        agentConfigVo.setDockerName(source.getNames());
                    } else {
                        agentConfigVo.setStatus(null);
                    }
                    return agentConfigVo;
                }).sorted(Comparator.comparing(vo -> vo.getStatus() == null ? 1 : 0, Comparator.naturalOrder())).collect(Collectors.toList());
            }
        }
        return query.stream().map(AgentVo::new).collect(Collectors.toList());
    }

    @Override
    public Agent getjumpServers(Integer serviceId) {
        List<Agent> jumpServers = baseMapper.selectList(Wrappers.<Agent>lambdaQuery().eq(Agent::getServiceId, serviceId)
                .eq(Agent::getIsJumpServer, true));
        if (jumpServers.isEmpty()) {
            throw new BusinessException("未设置跳板机");
        }
        if (jumpServers.size() > 1) {
            throw new BusinessException("设置了多个跳板机");
        }
        return jumpServers.get(0);
    }

    @Override
    public List<Agent> getListByServiceId(Integer serviceId) {
        return baseMapper.getListByServiceId(serviceId);
    }

    /**
     * 重写新增修改方法
     *
     * @param entity
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdate(Agent entity) {
        // 修改跳板机 需要把环境其他的跳板机修正 一个环境只有1个跳板机
        if (Objects.nonNull(entity.getIsJumpServer()) && entity.getIsJumpServer() && Objects.nonNull(entity.getServiceId())) {
            lambdaUpdate().set(Agent::getIsJumpServer, false).eq(Agent::getServiceId, entity.getServiceId()).update();

        }
        return super.saveOrUpdate(entity);
    }

    @Override
    public Agent getByMac(String mac, String hostname) {
        Agent config = this.baseMapper.selectOne(Wrappers.<Agent>lambdaQuery().eq(Agent::getMac, mac));
        if (config == null) {
            config = new Agent();
            config.setMac(mac);
            config.setHostname(hostname);
            this.baseMapper.insert(config);
            return null;
        }
        if (config.getServiceId() == null) {
            return config;
        }
        Agent configCommon = this.baseMapper.selectOne(Wrappers.<Agent>lambdaQuery()
                .eq(Agent::getServiceId, config.getServiceId()).isNull(Agent::getMac));
        if (configCommon == null) {
            return config;
        }
        //null,表示无限制，true表示若父类中属性值为空则忽略，不传给子类
        BeanUtil.copyProperties(config, configCommon, copyOption);
        return configCommon;
    }

    @Override
    public Agent getServiceIdAndHostname(Integer serviceId, String hostname) {
        Agent config = this.baseMapper.selectOne(Wrappers.<Agent>lambdaQuery().eq(Agent::getServiceId, serviceId)
                .eq(Agent::getHostname, hostname));
        Agent configCommon = this.baseMapper.selectOne(Wrappers.<Agent>lambdaQuery()
                .eq(Agent::getServiceId, config.getServiceId())
                .eq(Agent::getIsMonitor, true).isNull(Agent::getMac));
        if (configCommon == null) {
            return config;
        }
        BeanUtil.copyProperties(config, configCommon, copyOption);
        return configCommon;
    }

    @Override
    public Agent getByServiceIdAndHost(Integer serviceId, String hostname) {
        return this.baseMapper.getByServiceIdAndHost(serviceId, hostname);
    }


    @Override
    public void receiveClientMsg(ClientMsgForm clientMsgForm) {
        if (MonitorCmdC.updateClientVersion.equals(clientMsgForm.getCmd())) {
            Agent agentConfig = new Agent();
            agentConfig.setId(clientMsgForm.getAgentId());
            agentConfig.setClientVersion(clientMsgForm.getData());
            this.baseMapper.updateById(agentConfig);
        }
    }

    @Override
    public void handleAgentByServiceId(Integer serviceId, String cmd) {
        List<Agent> agentConfigs = this.baseMapper.getListByServiceId(serviceId);
        for (Agent agentConfig : agentConfigs) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOnce("handle", "agentManagerHandle");
            jsonObject.putOnce("url", upgradeClientUrl);
            jsonObject.putOnce("cmd", cmd);
            iMsgService.sendMsg(agentConfig.getId(), jsonObject.toString(), MonitorMsgType.TASK, null);
        }
    }

    @Override
    public void handleAgent(Long agentId, String cmd) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOnce("handle", "agentManagerHandle");
        jsonObject.putOnce("cmd", cmd);
        jsonObject.putOnce("url", upgradeClientUrl);
        iMsgService.sendMsgAndResponse(agentId, jsonObject.toString(), MonitorMsgType.TASK, 3);
    }

}
