package com.bszn.base.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * DESC
 *
 * @Author liuyun
 * @Date 2023-07-17
 */
@Service
@Slf4j
public class RedisSerializableServiceCloudImpl implements IRedisSerializableService<Serializable> {

    @Resource(name = "newRedisTemplate")
    RedisTemplate<String, Serializable> redisTemplate;

    @Override
    public boolean expire(String key, Serializable value, long expire) {
        if (expire == -1) {
            this.redisTemplate.opsForValue().set(key, value);
        } else {
            this.redisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
        }
        return true;
    }

    @Override
    public Serializable get(String key) {
        ValueOperations<String, Serializable> operations = this.redisTemplate.opsForValue();
        return operations.get(key);
    }

    @Override
    public boolean del(String name) {
        return redisTemplate.delete(name);
    }

}
