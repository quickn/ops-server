package com.bszn.base.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisInvalidSubscriptionException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * DESC
 *
 * @Author liuyun
 * @Date 2023-03-15
 */
@Service
@Slf4j
@Primary
public class StringRedisServiceImpl implements IRedisService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedisMessageListenerContainer redisMessageListenerContainer;

    /**
     * 活跃的订阅映射：channel -> MessageListener
     */
    private final ConcurrentHashMap<String, MessageListener> activeSubscriptions = new ConcurrentHashMap<>();

    @Override
    public boolean expire(String key, String value, long expire) {
        if (expire == -1) {
            this.stringRedisTemplate.opsForValue().set(key, value);
        } else {
            //redisTemplate.opsForValue().set(key, value);
            //redisTemplate.expire(key, expire, TimeUnit.SECONDS);
            this.stringRedisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
        }
        return true;
    }

    @Override
    public String get(String key) {
        ValueOperations<String, String> operations = this.stringRedisTemplate.opsForValue();
        String string = operations.get(key);
        if (string != null) {
            string = string.trim();
        }
        return string;
    }

    @Override
    public void del(String name) {
        stringRedisTemplate.delete(name);
    }


    @Override
    public void publish(String channel, String message) {
        try {
            stringRedisTemplate.convertAndSend(channel, message);
            log.debug("Redis发布消息成功: channel={}, message={}", channel, message);
        } catch (Exception e) {
            log.error("Redis发布消息失败: channel={}, message={}", channel, message, e);
        }
    }

    private void doRebuild() {
        // 停止并销毁旧容器
        if (redisMessageListenerContainer != null) {
            try {
                redisMessageListenerContainer.stop();
                // 注意：不能直接 destroy，因为容器是 Spring 管理的 Bean
                // 这里只是等待容器完全停止
                Thread.sleep(500);
            } catch (Exception e) {
                log.warn("停止旧容器异常", e);
            }
        }
        // 重新启动容器
        if (!redisMessageListenerContainer.isRunning()) {
            redisMessageListenerContainer.start();
            // 等待容器启动完成
            try {
                Thread.sleep(500);
            } catch (Exception e) {

            }
            log.info("重新构建 {}", "redisMessageListenerContainer");
        }
    }


    @Override
    public void subscribe(String channel, MessageListener listener) {
        try {
            if (!redisMessageListenerContainer.isRunning()) {
                doRebuild();
            }
            // 如果已存在同频道订阅，先取消8
            unsubscribe(channel);
            // 直接实现 MessageListener（无需 MessageListenerAdapter）
            MessageListener redisListener = (message, pattern) -> {
                try {
                    listener.onMessage(message, pattern);
                } catch (Exception e) {
                    log.error("Redis消息处理异常: channel={}", new String(message.getChannel()), e);
                }
            };

            // 注册监听
            redisMessageListenerContainer.addMessageListener(redisListener, new ChannelTopic(channel));
            activeSubscriptions.put(channel, redisListener);
            log.debug("Redis订阅成功: channel={}", channel);
        } catch (RedisInvalidSubscriptionException redisInvalidSubscriptionException) {
            log.warn("订阅失败 channel={}", channel);
            doRebuild();
        } catch (Exception e) {
            log.error("Redis订阅失败: channel={}", channel, e);
        }
    }

    @Override
    public void unsubscribe(String channel) {
        if (channel == null || channel.isEmpty()) {
            return;
        }
        try {
            MessageListener listener = activeSubscriptions.remove(channel);
            if (listener != null) {
                redisMessageListenerContainer.removeMessageListener(listener);
                log.debug("Redis取消订阅成功: channel={}", channel);
            }
        } catch (Exception e) {
            log.error("Redis取消订阅失败: channel={}", channel, e);
        }
    }

}
