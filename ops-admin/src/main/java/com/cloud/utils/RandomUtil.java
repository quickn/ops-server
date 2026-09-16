package com.cloud.utils;

import java.util.Random;

/**
 * Created by Liuyun on 2023-04-12 10:24
 **/
public class RandomUtil {
    
    private final static Random random = new Random();

    /**
     * 按照ttl 10% 随机添加
     * @param ttl
     * @return
     */
    public static Integer getTtl(Integer ttl) {
        Integer number = random.nextInt(ttl / 10);
        ttl += number;
        return ttl;
    }
}
