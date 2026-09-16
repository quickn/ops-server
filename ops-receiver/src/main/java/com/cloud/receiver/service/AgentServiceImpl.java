package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cloud.receiver.entity.*;
import com.cloud.receiver.util.ThreadLocalUtil;
import com.cloud.receiver.util.TokenUtils;
import com.cloud.receiver.util.msg.WarnMailUtil;
import com.cloud.receiver.util.staticvar.BatchData;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AgentServiceImpl {

    ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 40, 2, TimeUnit.MINUTES, new LinkedBlockingDeque<>());

    @Resource
    private TokenUtils tokenUtils;
    @Resource
    AgentConfigServiceImpl agentConfigService;
    @Resource
    DockerContainerServiceImpl dockerContainerService;
    @Resource
    DockerStatsServiceImpl iDockerStatsService;
    @Resource
    CpuStateService cpuStateService;
    @Resource
    SystemInfoService iSystemInfoService;
    @Resource
    DiskStateService diskStateService;
    @Resource
    ServiceInfoServiceImpl iServiceInfoService;
    @Resource
    CmdLogInfoServiceImpl iCmdResultService;
    @Resource
    ProcessStatService processNetStatService;
    @Resource
    WarnLogInfoService warnLogInfoService;

    @Value("${spring.profiles.active}")
    private String profiles;

    public JSONObject collect(String paramBean, String agentToken) {
        log.debug(paramBean);
        JSONObject agentJsonObject = JSONObject.parseObject(paramBean);
        JSONObject resultJson = new JSONObject();
        if (StringUtils.isEmpty(agentToken)) {
            agentToken = agentJsonObject.getString("agentToken");
        }
        if (!tokenUtils.checkAgentToken(agentToken) && !"dev".equals(profiles)) {
            log.error("token is invalidate");
            resultJson.put("result", "error：token is invalidate");
            return resultJson;
        }
        Integer serviceId = agentJsonObject.getInteger("serviceId");
        String hostname = agentJsonObject.getString("hostname");
        if (serviceId == null) {
            return null;
        }
        //不接受非监控的服务消息
        ServiceInfo serviceInfo = iServiceInfoService.getById(serviceId);
        if (serviceInfo == null || serviceInfo.getIsMonitor() == null || !serviceInfo.getIsMonitor()) {
            return null;
        }
        Integer agentId = agentJsonObject.getInteger("agentId");
        Agent agentConfig = null;
        if (agentId != null) {
            agentConfig = agentConfigService.getById(agentId);
        } else {
            agentConfig = agentConfigService.getServiceIdAndHostname(serviceId, hostname);
        }
        if (agentConfig == null) {
            log.error("agentConfig is null" + serviceId + " " + hostname + agentId);
            return null;
        }
        if (!Objects.equals(agentConfig.getServiceId(), serviceInfo.getId())) {
            log.error("serviceId not match");
            return null;
        }
        if (!agentConfig.getOnline()) {
            agentConfigService.updateOnline(agentConfig.getId(), true);
        }
        ThreadLocalUtil.setServiceId(serviceId);
        JSONObject memState = agentJsonObject.getJSONObject("memState");
        JSONObject sysLoadState = agentJsonObject.getJSONObject("sysLoadState");
        JSONArray appInfoList = agentJsonObject.getJSONArray("appInfoList");
        JSONArray appStateList = agentJsonObject.getJSONArray("appStateList");
        JSONObject logInfo = agentJsonObject.getJSONObject("logInfo");
        JSONObject netIoState = agentJsonObject.getJSONObject("netIoState");
        try {
            cpuStateService.saveAgentJsonObject(agentJsonObject, agentConfig);
            iSystemInfoService.saveAgentJsonObject(agentJsonObject, agentConfig);
            if (logInfo != null) {
                WarnLogInfo bean = (WarnLogInfo) copy(WarnLogInfo.class, logInfo, agentConfig);
                BatchData.LOG_INFO_LIST.add(bean);
            }
            if (memState != null) {
                MemState bean = (MemState) copy(MemState.class, memState, agentConfig);
                BatchData.MEM_STATE_LIST.add(bean);
                Agent finalAgentConfig = agentConfig;
                Runnable runnable = () -> {
                    WarnMailUtil.sendMemWarnInfo(bean, finalAgentConfig);
                };
                executor.execute(runnable);
            }
            if (sysLoadState != null) {
                SysLoadState bean = (SysLoadState) copy(SysLoadState.class, sysLoadState, agentConfig);
                BatchData.SYSLOAD_STATE_LIST.add(bean);
            }
            if (netIoState != null) {
                NetIoState bean = (NetIoState) copy(NetIoState.class, netIoState, agentConfig);
                BatchData.NETIO_STATE_LIST.add(bean);
            }
            if (appInfoList != null && appStateList != null) {
                List<AppInfo> appInfoResList = appInfoList.toJavaList(AppInfo.class);
                for (AppInfo appInfo : appInfoResList) {
                    appInfo.setServiceId(serviceId);
                    BatchData.APP_INFO_LIST.add(appInfo);
                }
                List<AppState> appStateResList = appStateList.toJavaList(AppState.class);
                for (AppState appState : appStateResList) {
                    appState.setServiceId(serviceId);
                    BatchData.APP_STATE_LIST.add(appState);
                }
            }
            diskStateService.saveAgentJsonObject(agentJsonObject, agentConfig);
            dockerContainerService.saveAgentJsonObject(agentJsonObject, agentConfig);
            iCmdResultService.saveAgentJsonObject(agentJsonObject, agentConfig);
            iDockerStatsService.saveAgentJsonObject(agentJsonObject, agentConfig);
            processNetStatService.saveAgentJsonObject(agentJsonObject, agentConfig);
            warnLogInfoService.saveAgentJsonObject(agentJsonObject, agentConfig);
            resultJson.put("result", "success");
        } catch (Exception e) {
            log.error("接收消息异常", e);
            resultJson.put("result", "error：" + e);
        } finally {
            ThreadLocalUtil.removeServiceId();
            return resultJson;
        }
    }

    private <T> Object copy(Class<T> clazz, JSONObject jsonObject, Agent agentConfig) {
        jsonObject.put("serviceId", agentConfig.getServiceId());
        jsonObject.put("serviceName", agentConfig.getServiceName());
        T bean = jsonObject.toJavaObject(clazz);
        return bean;
    }
}
