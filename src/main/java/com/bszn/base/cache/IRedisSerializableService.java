package com.bszn.base.cache;

/**
 * Created by Liuyun on 2023-04-27 14:59
 **/
public interface IRedisSerializableService<R> {

    boolean expire(String key, R val, long time);

    R get(String key);

    boolean del(String key);
}
