package com.bszn.base.cache;

import org.springframework.data.redis.connection.MessageListener;

/**
 * 缓存类
 * Created by Liuyun on 2022-03-19 13:59
 **/
public interface IRedisService {

    boolean expire(String key, String val, long time);

    String get(String key);

    void del(String key);

    default String getNotExpireValue(String key) {
        return null;
    }

    ;


    /**
     * 发布消息到指定的 Redis 频道
     *
     * @param channel 频道名称
     * @param message 消息内容
     */
    default void publish(String channel, String message) {
        // 默认实现，子类可覆盖
    }


    /**
     * 订阅指定的 Redis 频道
     *
     * @param channel  频道名称
     * @param listener 消息监听器
     */
    default void subscribe(String channel, MessageListener listener) {
        // 默认实现，子类可覆盖
    }

    /**
     * 取消 Redis 频道订阅
     *
     * @param channel 频道名称
     */
    default void unsubscribe(String channel) {
        // 默认实现
    }
}
