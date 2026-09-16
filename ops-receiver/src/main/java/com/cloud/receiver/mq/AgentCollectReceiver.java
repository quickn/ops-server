package com.cloud.receiver.mq;

import com.cloud.receiver.service.AgentServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.nio.charset.StandardCharsets;

/***
 * 监控指令接受器
 */

@Configuration
@ConfigurationProperties(prefix = "rabbitmq.cmd")
@Slf4j
@Order(0)
public class AgentCollectReceiver {

    @Resource
    AgentServiceImpl agentServiceImpl;


    /**
     * Fanout Exchange 广播队列可以绑定多个队列；生产者发送的消息，经过exchange到达队列，从而实现一个消息被多个消费者获取的目的
     * Direct Exchange 队列可以绑定多个路由键，消息发送到路由键；这种模式就要去我们生产者在发送消息的时候，为此消息添加一个路由键，这样在消费者获取的时候才能达到“按需索取”
     * Topic Exchange 这种模式和Direct模式的原理是一样的，都是根据路由键进行消息的路由，但是这种支持路由键的模糊匹配，此时队列需要绑定要一个模式上。符号“#”匹配一个或多个词，符号“*”匹配不多不少一个词
     */

    @Bean
    public Queue agentCollectQueue() {
        String dynamicQueueName = "queue.ops.agent.collect";
        return new Queue(dynamicQueueName, false);
    }


    @RabbitListener(queues = "#{agentCollectQueue.name}", concurrency = "8", ackMode = "NONE")
    public void onReceiver(Message msg) {
        String message = new String(msg.getBody(), StandardCharsets.UTF_8);
        log.info("指令响应 {}", message);
        agentServiceImpl.collect(message, null);
    }

}
