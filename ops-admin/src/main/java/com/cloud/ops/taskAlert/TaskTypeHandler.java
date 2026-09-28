package com.cloud.ops.taskAlert;

/**
 * 任务类型处理器
 *
 * <p>不同任务类型（shell 脚本、定时部署等）对应不同的执行策略，
 * 由 {@link TaskTypeHandlerFactory} 统一收集并按 {@link #type()} 分发。</p>
 *
 * @author Liuyun
 */
public interface TaskTypeHandler {

    /**
     * 支持的默认任务类型，未匹配到具体处理器时的兜底实现
     */
    String DEFAULT_TYPE = "default";

    /**
     * 定时部署任务类型
     */
    String DEPLOY_TYPE = "deploy";

    /**
     * 定时同步 Jar 任务类型
     */
    String SYNC_JAR_TYPE = "syncJar";

    /**
     * 接口检测任务类型
     */
    String API_CHECk_TYPE = "apiCheck";

    /**
     * 该处理器支持的任务类型标识
     */
    String type();

    /**
     * 执行任务，执行结果写入 {@code record}
     *
     * @param taskAlert 任务配置
     * @param record    执行记录（用于回填结果/状态）
     */
    void handle(TaskAlert taskAlert, TaskAlertRecord record);

}
