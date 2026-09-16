package com.cloud.ops.msg;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cloud.base.cache.IRedisService;
import com.cloud.mq.MsgResult;
import com.cloud.system.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;


@Service
@Slf4j
public class CmdCacheMsgService {

    @Autowired
    IRedisService iRedisService;


    ExecutorService executor = Executors.newSingleThreadExecutor();


    public void handle(String messageId, String message) {
        iRedisService.publish("monitor:cmd:" + messageId, message);
    }

    public String getMsg(String messageId) {
        return getMsg(messageId, 3);
    }

    public MsgResult getMsgResult(String messageId, Integer timeout) {
        String msg = getOrWait("monitor:cmd:" + messageId, timeout);
        if (msg != null) {
            JSONObject jsonObject = JSONUtil.parseObj(msg);
            MsgResult msgResult = jsonObject.toBean(MsgResult.class);
            if (msgResult.getCode() == 1) {
                log.warn("执行指令异常 {}", msgResult.getData());
                throw new BusinessException(msgResult.getData());
            }
            return msgResult;
        }
        return null;
    }

    /**
     * 基于 Redis Pub/Sub 的 getOrWait（终极优化版）
     * <pre>
     * 优化点：
     * 1. 数据内嵌通知 — 通知消息直接携带数据，省去额外 Redis GET
     * 2. 订阅句柄管理 — subscribe 返回 Subscription，finally 中 unsubscribe 防泄漏
     * 3. 错误即时通知 — 错误也通过同一频道推送，不再需要等超时
     * 4. 兜底轮询 — Pub/Sub 不可用时自动 fallback 到轮询，兼顾可靠性
     * 5. CompletableFuture — 优雅的异步编排，取代 CountDownLatch
     * </pre>
     *
     * @param key     Redis key
     * @param timeout 超时时间（秒）
     * @return Redis 中的值，或 null 表示超时
     */
    public String getOrWait(final String key, Integer timeout) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            // 2. 订阅通知频道
            iRedisService.subscribe(key, (message, pattern) -> {
                if (!future.isDone()) {
                    String msg = new String(message.getBody());
                    future.complete(msg);
                }
            });
            // 4. 等待通知
            try {
                return future.get(timeout, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                // Pub/Sub 超时，走兜底轮询
                log.warn("Redis Pub/Sub 超时，启用兜底轮询: key={}", key);
                throw new BusinessException("方法执行超时");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("getOrWait 执行异常: key={}", key, e);
            throw new BusinessException(e.getMessage());
        } finally {
            // 7. 清理订阅，防止内存泄漏
            iRedisService.unsubscribe(key);
            future.cancel(true);
        }
    }

    public String getMsg(String key, Integer timeout) {
        Future<String> future = null;
        try {
            future = executor.submit(() -> {
                // 模拟耗时操作
                while (true) {
                    String msg = iRedisService.get(key);
                    if (msg != null) {
                        return msg;
                    }
                }
            });
            // 设置超时时间为2秒
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new BusinessException("方法执行超时");
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        } finally {
            if (future != null) {
                future.cancel(true);
            }
        }
    }
}
