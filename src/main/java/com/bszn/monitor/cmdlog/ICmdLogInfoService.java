package com.bszn.monitor.cmdlog;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author wzh
 * @date 2026/1/21 15:08
 * @description: 指定日志接口
 */
public interface ICmdLogInfoService extends IService<CmdLogInfo> {

    boolean updateResult(Long id, String result, Integer timeConsuming, Boolean isSuccess);

}
