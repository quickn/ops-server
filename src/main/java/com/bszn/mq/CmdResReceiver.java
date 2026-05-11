package com.bszn.mq;

import com.bszn.monitor.msg.CmdCacheMsgService;
import com.bszn.utils.IpUtil;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/***
 * 监控指令接受器
 */

@Configuration
@ConfigurationProperties(prefix = "rabbitmq.cmd")
@Slf4j
@Order(0)
public class CmdResReceiver {

    @Resource
    CmdCacheMsgService monitorCmdMsgHandle;


    /**
     * Fanout Exchange 广播队列可以绑定多个队列；生产者发送的消息，经过exchange到达队列，从而实现一个消息被多个消费者获取的目的
     * Direct Exchange 队列可以绑定多个路由键，消息发送到路由键；这种模式就要去我们生产者在发送消息的时候，为此消息添加一个路由键，这样在消费者获取的时候才能达到“按需索取”
     * Topic Exchange 这种模式和Direct模式的原理是一样的，都是根据路由键进行消息的路由，但是这种支持路由键的模糊匹配，此时队列需要绑定要一个模式上。符号“#”匹配一个或多个词，符号“*”匹配不多不少一个词
     */

    @Bean
    public Queue monitorCmdDirectQueue() {
        String dynamicQueueName = "queue.monitor.cmd.server." + IpUtil.getIPv4Ip();
        return new Queue(dynamicQueueName, true);
    }

    @Bean
    DirectExchange monitorCmdDirectExchange() {
        return new DirectExchange("exchange.monitor.cmd.server");
    }

    @Bean
    Binding bindingSyncDirectQueue(Queue monitorCmdDirectQueue, DirectExchange monitorCmdDirectExchange) {
        String routingKey = "routingKey.monitor.cmd.server." + IpUtil.getIPv4Ip();
        return BindingBuilder.bind(monitorCmdDirectQueue).to(monitorCmdDirectExchange)
                .with(routingKey);
    }

    @RabbitListener(queues = "#{monitorCmdDirectQueue.name}", concurrency = "${rabbitmq.cmd.concurrency}")
    public void onReceiver(Message msg, Channel channel) throws IOException {
        String message = new String(msg.getBody(), StandardCharsets.UTF_8);
        String messageId = msg.getMessageProperties().getMessageId();
        Long tag = msg.getMessageProperties().getDeliveryTag();
        log.info("指令响应 {}", message);
        channel.basicAck(tag, false);
        monitorCmdMsgHandle.handle(messageId, message);
    }

}
