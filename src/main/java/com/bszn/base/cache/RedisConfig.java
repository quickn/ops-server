package com.bszn.base.cache;

import io.lettuce.core.resource.ClientResources;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.Duration;

/**
 * Created by Liuyun on 2022-05-23 14:49
 **/
@EnableCaching
@Configuration
@AutoConfigureBefore(RedisAutoConfiguration.class)
@Slf4j
public class RedisConfig {

    @Autowired
    private RedisProperties redisProperties;


    // 1. 连接池基础参数
    private GenericObjectPoolConfig<?> poolConfig() {
        RedisProperties.Pool pool = redisProperties.getLettuce().getPool();
        // 连接池配置
        GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(pool.getMaxActive());//连接池最大连接数
        poolConfig.setMaxIdle(pool.getMaxIdle()); //最大空闲连接
        poolConfig.setMinIdle(pool.getMinIdle()); //最小空闲连接
        poolConfig.setMaxWait(pool.getMaxWait()); //连接池最大阻塞等待时间
        poolConfig.setTimeBetweenEvictionRuns(pool.getTimeBetweenEvictionRuns()); //增加逐出扫描间隔
        poolConfig.setTestOnBorrow(true);   // 借用连接时检查
        poolConfig.setTestOnReturn(false); // 归还连接时不检查
        poolConfig.setTestWhileIdle(true); // 开启空闲检测
        return poolConfig;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(ClientResources resources) {

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        config.setDatabase(redisProperties.getDatabase());
        config.setPassword(redisProperties.getPassword());

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(redisProperties.getTimeout())
                .shutdownTimeout(Duration.ofMillis(200)).clientResources(resources)
                .poolConfig(poolConfig())
                .build();
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(config, clientConfig);
        lettuceConnectionFactory.setShareNativeConnection(false);// 必须关，否则池不生效
        return lettuceConnectionFactory;
    }

    /**
     * Redis 消息监听器容器
     * 用于管理 Redis Pub/Sub 订阅
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(LettuceConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        return container;
    }


}
