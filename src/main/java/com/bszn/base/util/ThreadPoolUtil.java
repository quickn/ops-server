package com.bszn.base.util;

import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

/**
 * Created by Liuyun on 2021-12-01 16:44
 **/
@Slf4j
@Service
public class ThreadPoolUtil {

    private volatile static ThreadPoolUtil clientExecutorService;

    public synchronized static ThreadPoolUtil getInstance() {
        if (clientExecutorService == null) {
            clientExecutorService = new ThreadPoolUtil();
        }
        return clientExecutorService;
    }

    /***
     * 自定义线程池 定义线程名称 定义拒绝策略
     * @return
     */
    public ExecutorService getNewCachedThreadPool() {
        return SpringUtil.getBean("newCachedThreadPool", ExecutorService.class);
    }
}
