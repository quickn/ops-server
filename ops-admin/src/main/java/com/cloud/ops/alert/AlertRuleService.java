package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 预警规则 Service
 *
 * @author Liuyun
 */
public interface AlertRuleService {

    /**
     * 分页查询预警规则
     */
    IPage<AlertRule> page(AlertRuleQueryDto dto);

    /**
     * 获取预警规则详情
     */
    AlertRule getById(Long id);

    /**
     * 保存/更新预警规则
     */
    boolean saveOrUpdate(AlertRule alertRule);

    /**
     * 删除预警规则
     */
    boolean removeById(Long id);

    /**
     * 触发预警（供外部调用，如监控指标超阈值时调用）
     *
     * @param ruleId       预警规则ID
     * @param alertContent 预警内容
     */
    void triggerAlert(Long ruleId, String alertContent);

    /**
     * 手动触发一次预警（跳过连续触发阈值，但受静默期约束）
     *
     * @param ruleId       预警规则ID
     * @param alertContent 预警内容
     */
    void manualTrigger(Long ruleId, String alertContent);
}
