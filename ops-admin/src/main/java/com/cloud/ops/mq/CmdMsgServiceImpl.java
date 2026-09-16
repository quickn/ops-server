package com.cloud.ops.mq;

import com.cloud.base.util.MyIdWorker;
import com.cloud.ops.constant.MonitorMsgType;
import com.cloud.ops.agent.Agent;
import com.cloud.ops.agent.AgentService;
import com.cloud.ops.cmdlog.CmdLogInfo;
import com.cloud.ops.cmdlog.ICmdLogInfoService;
import com.cloud.ops.encryption.CryptoService;
import com.cloud.ops.encryption.EncryptRequest;
import com.cloud.ops.msg.CmdCacheMsgService;
import com.cloud.ops.msg.IMsgService;
import com.cloud.system.common.exception.BusinessException;
import com.cloud.system.common.util.SecurityUtils;
import com.cloud.utils.CmdLogInfoSessionUtil;
import com.cloud.utils.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CmdMsgServiceImpl implements IMsgService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CmdCacheMsgService cmdCacheMsgService;

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private ICmdLogInfoService cmdLogInfoService;

    @Autowired
    AgentService agentConfigService;


    @Override
    public String sendMsg(Long agentId, String command, String script, String msgType, Integer timeout) {
        String ip = IpUtil.getIPv4Ip();
        final String messageId = MyIdWorker.getId() + "";
        log.info("发送指令 {} ", script);
        if (StringUtils.isEmpty(script)) {
            log.warn("指令为空 agentId:{}", agentId);
            return null;
        }
        try {
            // 加密
            script = cryptoService.encrypt(EncryptRequest.builder().userId(SecurityUtils.getUserId()).plainText(script).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        rabbitTemplate.convertAndSend("exchange." + MQConstants.MONITOR_CMD, "routing." + MQConstants.MONITOR_CMD + ".key." + agentId, script, message -> {
            message.getMessageProperties().setMessageId(messageId);
            // message.getMessageProperties().setCorrelationId(messageId);
            //message.getMessageProperties().setTimestamp(new Date());
            message.getMessageProperties().setContentType("application/json");
            message.getMessageProperties().setContentEncoding("UTF-8");
            // 添加自定义头部
            message.getMessageProperties().setHeader("serverIp", ip);
            message.getMessageProperties().setHeader("msgType", msgType);
            message.getMessageProperties().setHeader("command", command);
            if (timeout != null) {
                message.getMessageProperties().setHeader("timeout", timeout);
            }
            return message;
        });
        return messageId;
    }

    @Override
    public MsgResult sendMsgAndResponse(Long agentId, String command, String script, String msgType, Integer timeout) {
        return this.sendMsgAndResponse(CmdLogInfo.builder().agentId(agentId).command
                (command).script(script).msgType(msgType).timeout(timeout).build());
    }

    @Override
    public MsgResult sendMsgAndResponse(CmdLogInfo cmdLogInfo) {
        Long agentId = cmdLogInfo.getAgentId();
        String script = cmdLogInfo.getScript();
        String msgType = cmdLogInfo.getMsgType();
        if (msgType == null) {
            msgType = MonitorMsgType.CMD;
        }
        Integer timeout = cmdLogInfo.getTimeout();
        Agent byId = agentConfigService.getById(agentId);
        if (byId == null)
            throw new BusinessException("agent 不存在");
        CmdLogInfo session = CmdLogInfoSessionUtil.get();
        if (session != null) {
            cmdLogInfo.setJobId(session.getJobId());
            cmdLogInfo.setCommand(session.getCommand());
            cmdLogInfo.setMsgType(session.getMsgType());
            cmdLogInfo.setTimeout(session.getTimeout());
        }
        cmdLogInfo.setUserId(SecurityUtils.getUserId());
        cmdLogInfo.setServiceId(byId.getServiceId());
        cmdLogInfo.setServiceName(byId.getServiceName());
        cmdLogInfo.setAgentIp(byId.getHostname());
        cmdLogInfo.setCreateTime(LocalDateTime.now());
        cmdLogInfoService.save(cmdLogInfo);
        Long msgId = cmdLogInfo.getId();
        long startTime = System.currentTimeMillis();
        String messageId = sendMsg(agentId, cmdLogInfo.getCommand(), script, msgType, timeout);
        MsgResult msgResult = null;
        try {
            msgResult = cmdCacheMsgService.getMsgResult(messageId, timeout);
        } catch (Exception exception) {
            log.error("发送消息异常", exception);
            int timeConsuming = (int) ((System.currentTimeMillis() - startTime) / 1000);
            cmdLogInfoService.updateResult(msgId, exception.getMessage(), timeConsuming, false);
            throw new BusinessException(exception.getMessage());
        }
        String result = "返回结果为空";
        if (msgResult != null) {
            result = msgResult.getData();
        }
        int timeConsuming = (int) ((System.currentTimeMillis() - startTime) / 1000);
        cmdLogInfoService.updateResult(msgId, result, timeConsuming, true);
        assert msgResult != null;
        msgResult.setMsgId(msgId);
        // 保存日志
        return msgResult;
    }
}
