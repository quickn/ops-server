package com.bszn.base.cache;

/**
 * 缓存类
 * Created by Liuyun on 2022-03-19 13:59
 **/
public interface IRedisService {

    boolean expire(String key, String val, long time);

    String get(String key);

    void del(String key);

    default String getNotExpireValue(String key) {return null;};
}
