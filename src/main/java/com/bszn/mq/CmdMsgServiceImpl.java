package com.bszn.mq;

import com.bszn.base.util.MyIdWorker;
import com.bszn.monitor.cmdlog.ICmdLogInfoService;
import com.bszn.monitor.encryption.CryptoService;
import com.bszn.monitor.encryption.EncryptRequest;
import com.bszn.monitor.msg.CmdCacheMsgService;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.utils.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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


    @Override
    public String sendMsg(Long userId, Long agentId, String msg, String msgType, Integer timeout) {
        String ip = IpUtil.getIPv4Ip();
        final String messageId = MyIdWorker.getId() + "";
        log.info("发送指令 {} ", msg);
        try {
            // 加密
            msg = cryptoService.encrypt(EncryptRequest.builder().userId(userId).plainText(msg).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        rabbitTemplate.convertAndSend("exchange." + MQConstants.MONITOR_CMD, "routing." + MQConstants.MONITOR_CMD + ".key." + agentId, msg, message -> {
            message.getMessageProperties().setMessageId(messageId);
            // message.getMessageProperties().setCorrelationId(messageId);
            //message.getMessageProperties().setTimestamp(new Date());
            message.getMessageProperties().setContentType("application/json");
            message.getMessageProperties().setContentEncoding("UTF-8");
            // 添加自定义头部
            message.getMessageProperties().setHeader("serverIp", ip);
            message.getMessageProperties().setHeader("msgType", msgType);
            if (timeout != null) {
                message.getMessageProperties().setHeader("timeout", timeout);
            }
            return message;
        });
        return messageId;
    }

    @Override
    public MsgResult sendMsgAndResponse(Long userId, Long agentId, String msg, String msgType, Integer timeout) {
        String messageId = sendMsg(userId, agentId, msg, msgType, timeout);
        MsgResult msgResult = cmdCacheMsgService.getMsgResult(messageId, timeout);
        // 保存日志
        cmdLogInfoService.save(userId, agentId, msg, msgResult.getData());
        return msgResult;
    }
}
