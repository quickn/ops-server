package com.cloud.ops.cmdlog;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author wzh
 * @date 2026/1/21 15:08
 * @description: 指令日志业务层
 */
@Service
@RequiredArgsConstructor
public class CmdLogInfoServiceImpl extends ServiceImpl<CmdLogInfoMapper, CmdLogInfo> implements ICmdLogInfoService {


    @Override
    public void updateResult(Long id, String result, Integer timeConsuming, Boolean isSuccess) {
        if (result != null && result.length() >= 2000) {
            result = result.substring(0, 2000);
        }
        if (isSuccess && StringUtils.isNotEmpty(result)) {
            if (result.contains("errors")) {
                isSuccess = false;
            }
        }
        updateById(CmdLogInfo.builder()
                .id(id)
                .result(result)
                .isSuccess(isSuccess)
                .timeConsuming(timeConsuming)
                .build());
    }
}
