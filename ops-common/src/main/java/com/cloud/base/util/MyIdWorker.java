package com.cloud.base.util;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;

/**
 * ID生成器
 * Created by Liuyun on 2023-07-12 15:42
 **/
@Slf4j
public class MyIdWorker {

    public static long getId() {
        return IdWorker.getId();
    }

    /**
     * 雪花算法ID（字符串形式，兼容 char(32) 类型主键）
     */
    public static Long getUUID() {
        return IdWorker.getId();
    }
}
