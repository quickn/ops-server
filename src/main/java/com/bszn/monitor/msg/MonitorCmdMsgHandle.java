package com.bszn.monitor.msg;

import com.bszn.base.cache.IRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;


@Service
public class MonitorCmdMsgHandle {

    @Autowired
    IRedisService iRedisService;


    ExecutorService executor = Executors.newSingleThreadExecutor();


    public void handle(String messageId, String message) {
        iRedisService.expire("monitor:cmd:" + messageId, message, 60);
    }

    public String getMsg(String messageId) {
        return getMsg(messageId, 3);
    }

    public String getMsg(String messageId, Integer timeout) {
        Future<String> future = null;
        try {
            future = executor.submit(() -> {
                // 模拟耗时操作
                while (true) {
                    String msg = iRedisService.get("monitor:cmd:" + messageId);
                    if (msg != null) {
                        return msg;
                    }
                }
            });
            // 设置超时时间为2秒
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("方法执行超时");
        } catch (Exception e) {
            System.out.println("执行出错: " + e.getMessage());
        } finally {
            if (future != null) {
                future.cancel(true);
            }
        }
        return null;
    }
}
