package com.cloud.ops.alert;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 预警降噪处理器
 *
 * <p>提供三种降噪策略：</p>
 * <ul>
 *   <li>静默期（Silence Period）：同一规则触发后，在指定分钟内不重复发送预警</li>
 *   <li>连续触发阈值（Trigger Threshold）：连续触发 N 次后才真正发送预警</li>
 *   <li>聚合窗口（Aggregate Window）：窗口内多次触发合并为一条预警发送</li>
 * </ul>
 *
 * @author Liuyun
 */
@Component
@Slf4j
public class AlertNoiseReducer {

    /**
     * 规则级降噪状态：ruleId -> NoiseState
     */
    private final Map<Long, NoiseState> noiseStateMap = new ConcurrentHashMap<>();

    /**
     * 判断某条预警规则是否应该被发送（经过降噪处理后）
     *
     * @param rule 预警规则
     * @return true=应该发送，false=被降噪拦截
     */
    public boolean shouldSend(AlertRule rule) {
        Long ruleId = rule.getId();
        NoiseState state = noiseStateMap.computeIfAbsent(ruleId, k -> new NoiseState());

        LocalDateTime now = LocalDateTime.now();
        state.incrementTriggerCount();

        // 策略1：静默期检查 - 上一次发送预警后，在静默期内不重复发送
        Integer silencePeriod = rule.getSilencePeriod();
        if (silencePeriod != null && silencePeriod > 0 && state.getLastAlertTime() != null) {
            LocalDateTime silenceEnd = state.getLastAlertTime().plusMinutes(silencePeriod);
            if (now.isBefore(silenceEnd)) {
                log.info("预警规则[id={}] 处于静默期内（{}分钟），跳过发送", ruleId, silencePeriod);
                return false;
            }
        }

        // 策略2：连续触发阈值检查 - 未达到阈值次数则不发送
        Integer threshold = rule.getTriggerThreshold();
        if (threshold != null && threshold > 1) {
            if (state.getConsecutiveCount() < threshold) {
                log.info("预警规则[id={}] 连续触发次数 {}/{}，未达阈值，跳过发送",
                        ruleId, state.getConsecutiveCount(), threshold);
                return false;
            }
        }

        // 策略3：聚合窗口检查 - 窗口内只发送第一次，后续触发合并
        Integer aggregateWindow = rule.getAggregateWindow();
        if (aggregateWindow != null && aggregateWindow > 0 && state.getLastAlertTime() != null) {
            LocalDateTime windowEnd = state.getLastAlertTime().plusMinutes(aggregateWindow);
            if (now.isBefore(windowEnd)) {
                log.info("预警规则[id={}] 在聚合窗口内（{}分钟），合并为一条预警", ruleId, aggregateWindow);
                return true;
            }
        }

        // 通过降噪检查，允许发送
        state.setLastAlertTime(now);
        state.resetConsecutiveCount();
        return true;
    }

    /**
     * 记录触发（不发送），用于累计连续触发次数
     *
     * @param ruleId 规则ID
     */
    public void recordTrigger(Long ruleId) {
        NoiseState state = noiseStateMap.computeIfAbsent(ruleId, k -> new NoiseState());
        state.incrementConsecutiveCount();
        state.incrementTriggerCount();
    }

    /**
     * 重置规则降噪状态（规则修改或手动恢复时使用）
     */
    public void resetState(Long ruleId) {
        noiseStateMap.remove(ruleId);
        log.info("预警规则[id={}] 降噪状态已重置", ruleId);
    }

    /**
     * 获取规则当前连续触发次数
     */
    public int getConsecutiveCount(Long ruleId) {
        NoiseState state = noiseStateMap.get(ruleId);
        return state == null ? 0 : state.getConsecutiveCount();
    }

    /**
     * 获取规则累计触发次数
     */
    public int getTriggerCount(Long ruleId) {
        NoiseState state = noiseStateMap.get(ruleId);
        return state == null ? 0 : state.getTriggerCount();
    }

    /**
     * 降噪状态
     */
    @Data
    static class NoiseState {
        /** 上一次实际发送预警的时间 */
        private LocalDateTime lastAlertTime;
        /** 连续触发次数（成功发送后重置） */
        private int consecutiveCount;
        /** 累计触发次数 */
        private int triggerCount;

        void incrementConsecutiveCount() {
            this.consecutiveCount++;
        }

        void resetConsecutiveCount() {
            this.consecutiveCount = 0;
        }

        void incrementTriggerCount() {
            this.triggerCount++;
        }
    }
}
