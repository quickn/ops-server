package com.bszn.base.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Map;

@Service
@Slf4j
public class RedisHashServiceCloudImpl implements IRedisHashServiceCloud {

    private HashOperations<String, Long, String> hashOperations;

    public RedisHashServiceCloudImpl(RedisTemplate<String, Serializable> redisTemplate) {
        this.hashOperations = redisTemplate.opsForHash();
    }

    @Override
    public void hset(String key, Long field, String value) {
        hashOperations.put(key, field, value);
    }

    @Override
    public String hget(String key, Long field) {
        String val = hashOperations.get(key, field.toString());
        return val == null ? null : val;
    }

    @Override
    public boolean hdel(String key, Long field) {
        Long lang = hashOperations.delete(key, field);
        return true;
    }

    @Override
    public Map<Long, String> hvals(String key) {
        Map<Long, String> myHash = hashOperations.entries(key);
        return myHash;
    }
}
