package com.cloud.ops.taskAlert;

import com.alibaba.fastjson2.JSONObject;
import com.cloud.ops.project.IProjectService;
import com.cloud.ops.project.SyncRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 定时同步 Jar 任务处理器
 *
 * <p>参考 {@code ProjectJobHandler#syncJarJobHandler}：从源服务器同步 Jar 包到各项目。
 * 脚本内容约定为 JSON：
 * <pre>
 * {
 *   "projectNames": "projectA,projectB",
 *   "interval": 0
 * }
 * </pre></p>
 *
 * @author Liuyun
 */
@Component
@Slf4j
public class SyncJarTaskHandler implements TaskTypeHandler {

    @Resource
    private IProjectService iProjectService;

    @Override
    public String type() {
        return SYNC_JAR_TYPE;
    }

    @Override
    public void handle(TaskAlert taskAlert, TaskAlertRecord record) {
        if (StringUtils.isBlank(taskAlert.getScript())) {
            record.setState(1);
            record.setResult("同步配置为空");
            return;
        }
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(taskAlert.getScript());
        } catch (Exception e) {
            record.setState(1);
            record.setResult("同步配置解析失败：" + e.getMessage());
            return;
        }

        String projectNamesStr = jsonObject.getString("projectNames");
        if (StringUtils.isBlank(projectNamesStr)) {
            record.setState(1);
            record.setResult("同步配置缺少 projectNames");
            return;
        }
        final Integer interval = jsonObject.getIntValue("interval", 0);

        String[] projectNames = projectNamesStr.split(",");
        List<String> failedProjects = new ArrayList<>();
        List<String> successProjects = new ArrayList<>();
        for (int i = 0; i < projectNames.length; i++) {
            String projectName = projectNames[i].trim();
            if (projectName.isEmpty()) {
                continue;
            }
            try {
                SyncRequest syncRequest = new SyncRequest();
                syncRequest.setServiceId(taskAlert.getServiceId());
                syncRequest.setType(1);
                syncRequest.setSourceAgentId(taskAlert.getAgentId());
                syncRequest.setProjectName(projectName);
                Boolean result = iProjectService.sync(syncRequest);
                if (Boolean.TRUE.equals(result)) {
                    successProjects.add(projectName);
                } else {
                    failedProjects.add(projectName);
                }
            } catch (Exception e) {
                log.error("定时同步 Jar 项目异常 projectName:{}", projectName, e);
                failedProjects.add(projectName);
            }
            // 多个项目之间按 interval 间隔，避免并发冲击
            if (interval > 0 && i < projectNames.length - 1) {
                try {
                    Thread.sleep(interval * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("定时同步 Jar 间隔等待被中断");
                }
            }
        }

        if (!failedProjects.isEmpty()) {
            record.setState(1);
            record.setResult("同步失败项目：" + String.join(",", failedProjects)
                    + "；成功项目：" + successProjects.size() + " 个");
        } else {
            record.setResult("同步完成，共 " + successProjects.size() + " 个项目");
        }
    }

}
