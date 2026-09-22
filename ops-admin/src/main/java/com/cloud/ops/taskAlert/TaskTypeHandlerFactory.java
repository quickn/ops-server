package com.cloud.ops.taskAlert;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务类型处理器工厂
 *
 * <p>启动时自动收集所有 {@link TaskTypeHandler} Bean，按 {@link TaskTypeHandler#type()}
 * 建立映射。{@link #getHandler(String)} 在未匹配到具体类型时返回默认处理器（兜底）。</p>
 *
 * @author Liuyun
 */
@Component
@Slf4j
public class TaskTypeHandlerFactory {

    private final List<TaskTypeHandler> handlers;

    private final Map<String, TaskTypeHandler> handlerMap = new HashMap<>();

    private TaskTypeHandler defaultHandler;

    public TaskTypeHandlerFactory(List<TaskTypeHandler> handlers) {
        this.handlers = handlers;
    }

    @PostConstruct
    public void init() {
        for (TaskTypeHandler handler : handlers) {
            handlerMap.put(handler.type(), handler);
            if (TaskTypeHandler.DEFAULT_TYPE.equals(handler.type())) {
                defaultHandler = handler;
            }
            log.info("注册任务类型处理器 type:{} -> {}", handler.type(), handler.getClass().getSimpleName());
        }
    }

    /**
     * 根据任务类型获取处理器，未匹配时返回默认处理器
     *
     * @param type 任务类型
     * @return 处理器，保证非空
     */
    public TaskTypeHandler getHandler(String type) {
        TaskTypeHandler handler = handlerMap.get(type);
        if (handler != null) {
            return handler;
        }
        if (defaultHandler != null) {
            return defaultHandler;
        }
        throw new IllegalStateException("未找到任何任务类型处理器");
    }

}
