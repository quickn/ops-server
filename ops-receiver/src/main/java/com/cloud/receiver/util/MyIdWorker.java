package com.cloud.receiver.util;

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

    public static Long getUUID() {
        return IdWorker.getId();
    }
}
