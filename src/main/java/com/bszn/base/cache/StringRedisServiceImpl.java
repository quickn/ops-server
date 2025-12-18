package com.bszn.base.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

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

}
