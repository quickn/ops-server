package com.bszn.base.cache;

import java.util.Map;

public interface IRedisHashServiceCloud {

    void hset(String key, Long field, String value);

    String hget(String key, Long field);

    boolean hdel(String key, Long field);

    Map<Long, String> hvals(String key);

}
