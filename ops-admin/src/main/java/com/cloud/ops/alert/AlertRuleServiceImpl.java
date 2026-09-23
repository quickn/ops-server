package com.cloud.ops.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 预警规则业务实现
 *
 * @author Liuyun
 */
@Service
@Slf4j
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleMapper, AlertRule> implements AlertRuleService {

    @Resource
    private AlertRecordMapper alertRecordMapper;

    @Resource
    private AlertNoiseReducer noiseReducer;

    @Resource
    private AlertSender alertSender;

    @Resource
    private AlertContactGroupMapper contactGroupMapper;

    @Resource
    private AlertContactMapper contactMapper;

    @Override
    public IPage<AlertRule> page(AlertRuleQueryDto dto) {
        return this.page(dto.getPage(), dto.buildLambda());
    }

    @Override
    public AlertRule getById(Long id) {
        return super.getById(id);
    }

    @Override
    public boolean saveOrUpdate(AlertRule alertRule) {
        if (alertRule.getId() == null && alertRule.getIsEnabled() == null) {
            alertRule.setIsEnabled(true);
        }
        // 默认触发类型
        if (alertRule.getTriggerType() == null) {
            alertRule.setTriggerType("KEYWORD");
        }
        // 默认降噪参数
        if (alertRule.getSilencePeriod() == null) {
            alertRule.setSilencePeriod(5);
        }
        if (alertRule.getTriggerThreshold() == null) {
            alertRule.setTriggerThreshold(1);
        }
        boolean result = super.saveOrUpdate(alertRule);
        // 规则变更时重置降噪状态
        noiseReducer.resetState(alertRule.getId());
        return result;
    }

    @Override
    public boolean removeById(Long id) {
        noiseReducer.resetState(id);
        return super.removeById(id);
    }

    @Override
    public void triggerAlert(Long ruleId, String alertContent) {
        AlertRule rule = super.getById(ruleId);
        if (rule == null) {
            log.warn("预警规则不存在 id:{}", ruleId);
            return;
        }
        if (!Boolean.TRUE.equals(rule.getIsEnabled())) {
            log.info("预警规则[id={}] 已禁用，跳过", ruleId);
            return;
        }

        // 记录触发（用于连续触发计数）
        noiseReducer.recordTrigger(ruleId);

        // 降噪判断
        boolean shouldSend = noiseReducer.shouldSend(rule);
        int triggerCount = noiseReducer.getTriggerCount(ruleId);

        if (!shouldSend) {
            // 被降噪静默，仍然记录但不发送
            this.saveAlertRecord(rule, null, null, alertContent, 0, 1, triggerCount, "降噪静默");
            return;
        }

        // 解析预警通道并逐一发送
        this.doSendAlert(rule, alertContent, triggerCount);
    }

    @Override
    public void manualTrigger(Long ruleId, String alertContent) {
        AlertRule rule = super.getById(ruleId);
        if (rule == null) {
            log.warn("预警规则不存在 id:{}", ruleId);
            return;
        }
        // 手动触发重置连续计数，确保能发送
        noiseReducer.resetState(ruleId);
        this.doSendAlert(rule, alertContent, 1);
    }

    /**
     * 按规则配置的预警通道逐一发送
     */
    private void doSendAlert(AlertRule rule, String alertContent, int triggerCount) {
        String channels = rule.getAlertChannels();
        if (StringUtils.isBlank(channels)) {
            log.warn("预警规则[id={}] 未配置预警通道", rule.getId());
            return;
        }

        // 从联系人组解析接收人
        List<AlertContact> contacts = resolveContacts(rule.getContactGroupId());
        if (contacts.isEmpty()) {
            log.warn("预警规则[id={}] 联系人组[id={}] 下无联系人", rule.getId(), rule.getContactGroupId());
            return;
        }

        String title = buildAlertTitle(rule);
        List<String> channelList = Arrays.asList(channels.split(","));

        for (String channelCode : channelList) {
            AlertChannelType channelType = AlertChannelType.fromCode(channelCode.trim());
            if (channelType == null) {
                log.warn("预警规则[id={}] 未知通道类型: {}", rule.getId(), channelCode);
                continue;
            }

            // 根据通道类型筛选对应接收人
            List<String> recipientList = resolveRecipients(channelType, contacts);

            String failReason = alertSender.send(rule.getCreateBy(), channelType, recipientList, title, alertContent);
            int sendStatus = failReason == null ? 0 : 1;

            this.saveAlertRecord(rule, channelType, recipientList, alertContent, sendStatus, 0, triggerCount, failReason);
        }
    }

    /**
     * 根据联系人组ID查询组内联系人
     */
    private List<AlertContact> resolveContacts(Long contactGroupId) {
        if (contactGroupId == null) {
            return Collections.emptyList();
        }
        AlertContactGroup group = contactGroupMapper.selectById(contactGroupId);
        if (group == null || StringUtils.isBlank(group.getContactIds())) {
            return Collections.emptyList();
        }
        List<Long> ids = Arrays.stream(group.getContactIds().split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(Long::parseLong)
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return contactMapper.selectBatchIds(ids);
    }

    /**
     * 根据通道类型从联系人列表中筛选对应接收地址
     *
     * <p>EMAIL 通道取联系人的 email 字段，SMS 通道取联系人的 phone 字段</p>
     */
    private List<String> resolveRecipients(AlertChannelType channelType, List<AlertContact> contacts) {
        List<String> result = new ArrayList<>();
        for (AlertContact contact : contacts) {
            if (channelType == AlertChannelType.EMAIL && StringUtils.isNotBlank(contact.getEmail())) {
                result.add(contact.getEmail().trim());
            } else if (channelType == AlertChannelType.SMS && StringUtils.isNotBlank(contact.getPhone())) {
                result.add(contact.getPhone().trim());
            }
        }
        return result;
    }

    /**
     * 构建预警标题
     */
    private String buildAlertTitle(AlertRule rule) {
        AlertLevel level = AlertLevel.fromCode(rule.getAlertLevel() != null ? rule.getAlertLevel() : 2);
        return "[" + level.getDesc() + "] " + rule.getRuleName();
    }

    /**
     * 保存预警记录
     */
    private void saveAlertRecord(AlertRule rule, AlertChannelType channelType, List<String> recipientList,
                                 String alertContent, int sendStatus, int isSilenced,
                                 int triggerCount, String failReason) {
        AlertRecord record = new AlertRecord();
        record.setRuleId(rule.getId());
        record.setRuleName(rule.getRuleName());
        record.setAlertLevel(rule.getAlertLevel());
        record.setAlertChannel(channelType != null ? channelType.getCode() : null);
        record.setAlertContent(alertContent);
        record.setRecipient(recipientList != null ? String.join(";", recipientList) : null);
        record.setSendStatus(sendStatus);
        record.setIsSilenced(isSilenced);
        record.setTriggerCount(triggerCount);
        record.setFailReason(failReason);
        record.setCreateBy(rule.getCreateBy());
        alertRecordMapper.insert(record);
    }
}
