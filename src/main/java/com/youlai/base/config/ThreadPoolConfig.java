package com.youlai.base.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * Created by Liuyun on 2023-04-14 13:58
 **/
@Configuration
@Data
@Slf4j
public class ThreadPoolConfig {

    /***
     *固定线程池数量
     */
    @Value("${treadPool.fixCount:64}")
    private Integer fixTreadPoolCount;

    /**
     * 异步线程池数量
     */
    @Value("${treadPool.asyncCount:1000}")
    private Integer asyncTreadPoolCount;


    @Bean("newCachedThreadPool")
    public ExecutorService getBean() {
        ThreadPoolExecutor newCachedThreadPool = new ThreadPoolExecutor(3, this.getAsyncTreadPoolCount(),
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new ThreadFactory() {
            //使用SynchronousQueue队列，提交的任务不会被保存，总是会马上提交执行
            //自定义线程名称
            @Override
            public Thread newThread(Runnable r) {
                //线程命名
                Thread th = new Thread(r, "async-thread-" + r.hashCode());
                return th;
            }
        }, new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                log.warn("执行了拒绝策略 {}", r.toString());
                if (!executor.isShutdown()) {
                    //直接执行run方法
                    r.run();
                }
            }
        });
        return newCachedThreadPool;
    }

}
