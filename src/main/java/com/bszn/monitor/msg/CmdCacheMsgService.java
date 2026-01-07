package com.bszn.monitor.msg;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bszn.base.cache.IRedisService;
import com.bszn.mq.MsgResult;
import com.bszn.system.common.exception.BusinessException;
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
        iRedisService.expire("monitor:cmd:" + messageId, message, 300);
    }

    public String getMsg(String messageId) {
        return getMsg(messageId, 3);
    }

    public MsgResult getMsgResult(String messageId, Integer timeout) {
        String msg = getMsg(messageId, timeout);
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
