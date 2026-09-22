package com.cloud.ops.taskAlert;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author Liuyun
 */
public interface TaskAlertService extends IService<TaskAlert> {

    /**
     * 立即执行一次任务并记录执行结果
     *
     * @param id 任务Id
     */
    void execute(Long id);

    /**
     * 手动扫描所有启用的任务，执行并检测失败告警
     */
    void taskAlertCheck();

}
