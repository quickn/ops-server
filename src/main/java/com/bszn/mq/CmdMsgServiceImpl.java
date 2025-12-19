package com.bszn.mq;

import com.bszn.base.util.MyIdWorker;
import com.bszn.monitor.msg.CmdCacheMsgService;
import com.bszn.monitor.msg.IMsgService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CmdMsgServiceImpl implements IMsgService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    CmdCacheMsgService cmdCacheMsgService;

    @Override
    public String sendMsg(Long agentId, String msg) {
        final String messageId = MyIdWorker.getId() + "";
        rabbitTemplate.convertAndSend(
                "exchange." + MQConstants.MONITOR_CMD,
                "routing." + MQConstants.MONITOR_CMD + ".key." + agentId,
                msg, new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        message.getMessageProperties().setMessageId(messageId);
                        message.getMessageProperties().setCorrelationId(messageId);
                        //message.getMessageProperties().setTimestamp(new Date());
                        message.getMessageProperties().setContentType("application/json");
                        message.getMessageProperties().setContentEncoding("UTF-8");
                        // 添加自定义头部
                        // message.getMessageProperties().setHeader("business-id", "ORDER-123");
                        // message.getMessageProperties().setHeader("source-system", "order-service");
                        return message;
                    }
                }
        );
        return messageId;
    }

    @Override
    public String sendMsgAndResponse(Long agentId, String msg, Integer timeout) {
        String messageId = sendMsg(agentId, msg);
        return cmdCacheMsgService.getMsg(messageId, timeout);
    }
}
