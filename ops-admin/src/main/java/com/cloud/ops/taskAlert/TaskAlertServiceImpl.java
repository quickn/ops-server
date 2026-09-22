package com.cloud.ops.taskAlert;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.receiver.util.msg.WarnMailUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 任务告警业务实现
 *
 * <p> 计划任务：每个任务按自己的 cron 表达式独立调度执行，
 * 记录执行结果，当任务连续失败达到阈值时触发邮件告警。</p>
 *
 * <p>任务执行采用工厂模式：由 {@link TaskTypeHandlerFactory} 根据任务类型
 * 分发到对应的 {@link TaskTypeHandler} 处理器。</p>
 *
 * @author Liuyun
 */
@Service
@Slf4j
public class TaskAlertServiceImpl extends ServiceImpl<TaskAlertMapper, TaskAlert> implements TaskAlertService {

    @Resource
    private TaskAlertRecordMapper recordMapper;

    @Resource
    private TaskScheduler taskScheduler;

    @Resource
    private TaskTypeHandlerFactory taskTypeHandlerFactory;

    /**
     * 各任务连续失败次数缓存（内存）
     */
    private final Map<Long, Integer> failCountMap = new ConcurrentHashMap<>();

    /**
     * 各任务动态调度句柄（taskId -> ScheduledFuture）
     */
    private final Map<Long, ScheduledFuture<?>> scheduleFutureMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("初始化任务告警动态调度");
        List<TaskAlert> list = this.list();
        for (TaskAlert taskAlert : list) {
            this.refreshSchedule(taskAlert);
        }
    }

    @Override
    public void execute(Long id) {
        TaskAlert taskAlert = this.getById(id);
        if (taskAlert == null) {
            log.warn("任务告警配置不存在 id:{}", id);
            return;
        }
        if (!Integer.valueOf(1).equals(taskAlert.getStatus())) {
            log.warn("任务告警已停止 id:{}", id);
            return;
        }
        this.runTask(taskAlert);
    }

    @Override
    public void taskAlertCheck() {
        log.info("taskAlertCheck 开始扫描任务告警");
        List<TaskAlert> list = this.list(Wrappers.<TaskAlert>lambdaQuery().eq(TaskAlert::getStatus, 1));
        if (list.isEmpty()) {
            return;
        }
        for (TaskAlert taskAlert : list) {
            try {
                this.runTask(taskAlert);
            } catch (Exception e) {
                log.error("任务告警执行异常 taskId:{}", taskAlert.getId(), e);
            }
        }
        log.info("taskAlertCheck 结束");
    }

    @Override
    public boolean saveOrUpdate(TaskAlert taskAlert) {
        // 保存前校验 cron 表达式，非法时抛出 IllegalArgumentException 中断保存
        this.checkCron(taskAlert);
        // 新增且未显式指定状态时默认启动，避免 status 为空导致调度不注册
        if (taskAlert.getId() == null && taskAlert.getStatus() == null) {
            taskAlert.setStatus(1);
        }
        boolean result = super.saveOrUpdate(taskAlert);
        // 重新读取库中最新配置（含数据库默认值、未被前端覆盖的字段），再刷新调度
        this.refreshSchedule(this.getById(taskAlert.getId()));
        return result;
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        boolean result = super.removeById(id);
        this.cancelSchedule((Long) id);
        return result;
    }

    /**
     * 刷新单个任务的调度：取消旧调度，按最新配置重新注册
     */
    private synchronized void refreshSchedule(TaskAlert taskAlert) {
        if (taskAlert == null || taskAlert.getId() == null) {
            return;
        }
        this.cancelSchedule(taskAlert.getId());

        // 已停止 或 未配置 cron 则不注册调度
        if (!Integer.valueOf(1).equals(taskAlert.getStatus()) || StringUtils.isBlank(taskAlert.getCron())) {
            return;
        }
        String cron = this.normalizeCron(taskAlert.getCron());
        try {
            ScheduledFuture<?> future = taskScheduler.schedule(() -> this.safeRun(taskAlert.getId()), new CronTrigger(cron));
            scheduleFutureMap.put(taskAlert.getId(), future);
            log.info("任务告警已注册调度 taskId:{} cron:{}", taskAlert.getId(), cron);
        } catch (Exception e) {
            log.error("注册任务告警调度失败 taskId:{} cron:{}（Spring cron 需为 6 位：秒 分 时 日 月 周，如 0 0 2 * * ?），任务将执行一次后自动停止", taskAlert.getId(), cron, e);
            // cron 无效：任务执行一次后自动置为停止，避免一直保持启动状态却永不执行
            Long taskId = taskAlert.getId();
            taskScheduler.schedule(() -> {
                this.safeRun(taskId);
                this.stopTask(taskId);
            }, Instant.now());
        }
    }

    /**
     * 将任务状态置为停止（仅更新 status 字段，不触发调度刷新）
     */
    private void stopTask(Long id) {
        TaskAlert update = new TaskAlert();
        update.setId(id);
        update.setStatus(0);
        if (this.updateById(update)) {
            log.info("任务告警已自动停止（cron 无效，执行一次后停止）taskId:{}", id);
        } else {
            log.warn("任务告警自动停止失败 taskId:{}", id);
        }
    }

    /**
     * cron 规范化：兼容常见 5 位（分 时 日 月 周）与 7 位（末尾带年份）写法，
     * 统一转为 Spring CronTrigger 要求的 6 位（秒 分 时 日 月 周）
     */
    private String normalizeCron(String cron) {
        String trimmed = cron.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 5) {
            return "0 " + trimmed;
        }
        if (parts.length == 7) {
            return String.join(" ", Arrays.copyOfRange(parts, 0, 6));
        }
        return trimmed;
    }

    /**
     * 校验 cron 表达式（与注册调度时的规范化逻辑保持一致），非法时抛出 IllegalArgumentException，
     * 由全局异常处理器转换为 400 响应返回给前端
     */
    private void checkCron(TaskAlert taskAlert) {
        if (StringUtils.isBlank(taskAlert.getCron())) {
            // 停止状态的任务允许不配置 cron（仅手动执行），启动状态必须配置
            if (!Integer.valueOf(0).equals(taskAlert.getStatus())) {
                throw new IllegalArgumentException("启动状态的任务必须配置 cron 表达式");
            }
            return;
        }
        String cron = this.normalizeCron(taskAlert.getCron());
        if (!CronExpression.isValidExpression(cron)) {
            throw new IllegalArgumentException("cron 表达式不合法：" + taskAlert.getCron()
                    + "（需为 6 位：秒 分 时 日 月 周，如 0 0 2 * * ?）");
        }
    }

    /**
     * 取消单个任务的调度
     */
    private void cancelSchedule(Long id) {
        ScheduledFuture<?> future = scheduleFutureMap.remove(id);
        if (future != null) {
            future.cancel(false);
            log.info("任务告警已取消调度 taskId:{}", id);
        }
    }

    /**
     * 调度线程安全执行：重新从库中读取最新配置，避免使用过期快照
     */
    private void safeRun(Long id) {
        try {
            TaskAlert taskAlert = this.getById(id);
            if (taskAlert == null || !Integer.valueOf(1).equals(taskAlert.getStatus())) {
                log.warn("任务告警已不存在或已停止，跳过执行 taskId:{}", id);
                return;
            }
            this.runTask(taskAlert);
        } catch (Exception e) {
            log.error("任务告警调度执行异常 taskId:{}", id, e);
        }
    }

    /**
     * 执行单个任务并记录结果，失败达到阈值时发送告警
     */
    private void runTask(TaskAlert taskAlert) {
        long start = System.currentTimeMillis();
        TaskAlertRecord record = new TaskAlertRecord();
        record.setTaskId(taskAlert.getId());
        record.setServiceId(taskAlert.getServiceId());
        record.setServiceName(taskAlert.getServiceName());
        record.setState(0);
        record.setIsAlert(false);

        try {
            // 工厂模式：根据任务类型分发到对应处理器执行
            TaskTypeHandler handler = taskTypeHandlerFactory.getHandler(taskAlert.getTaskType());
            handler.handle(taskAlert, record);
        } catch (Exception e) {
            record.setState(1);
            record.setResult("任务执行失败：" + e.getMessage());
        }
        record.setTimeConsuming(System.currentTimeMillis() - start);
        recordMapper.insert(record);

        // 成功：重置失败计数
        if (record.getState() == 0) {
            failCountMap.put(taskAlert.getId(), 0);
            return;
        }

        // 失败：累计失败次数，达到阈值发送告警
        int threshold = taskAlert.getFailThreshold() == null || taskAlert.getFailThreshold() <= 0
                ? 1 : taskAlert.getFailThreshold();
        int count = failCountMap.getOrDefault(taskAlert.getId(), 0) + 1;
        failCountMap.put(taskAlert.getId(), count);
        if (count >= threshold && Boolean.TRUE.equals(taskAlert.getIsEmail())) {
            record.setIsAlert(true);
            recordMapper.updateById(record);
            this.sendAlert(taskAlert, record, count);
        }
    }

    /**
     * 发送告警邮件
     */
    private void sendAlert(TaskAlert taskAlert, TaskAlertRecord record, int count) {
        try {
            String title = "任务告警：" + taskAlert.getTaskName();
            String content = "任务【" + taskAlert.getTaskName() + "】执行失败，已连续失败 " + count + " 次。<br/>"
                    + "任务类型：" + taskAlert.getTaskType() + "<br/>"
                    + "执行结果：" + record.getResult();
            WarnMailUtil.sendMail(taskAlert.getServiceId(), null, title, content);
            log.info("任务告警邮件已发送 taskId:{}", taskAlert.getId());
        } catch (Exception e) {
            log.error("发送任务告警邮件失败 taskId:{}", taskAlert.getId(), e);
        }
    }

}
