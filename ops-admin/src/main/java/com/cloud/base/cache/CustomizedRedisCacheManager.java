package com.cloud.base.cache;

import com.cloud.utils.RandomUtil;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Created by Liuyun on 2023-03-29 11:12
 **/
public class CustomizedRedisCacheManager extends RedisCacheManager {

    public CustomizedRedisCacheManager(RedisCacheWriter cacheWriter,
                                       RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
    }

    @Override
    protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
        String[] array = StringUtils.delimitedListToStringArray(name, "#");
        name = array[0];
        if (array.length > 1) {
            Integer ttl = Integer.parseInt(array[1]);
            cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(RandomUtil.getTtl(ttl)));
        }
        return super.createRedisCache(name, cacheConfig);
    }
}
