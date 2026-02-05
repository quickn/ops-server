package com.bszn.monitor.heath;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bszn.base.sql.IQuery;
import com.bszn.base.sql.PageForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Objects;

/**
 * Created by Liuyun on 2023-09-09 11:51
 **/
@Data
public class ApiHeathQueryPage extends PageForm<ApiHeathMonitor> implements IQuery {

    @Schema(description = "环境id")
    private Long serviceId;

    @Schema(description = "接口名称")
    private String apiName;

    @Override
    public LambdaQueryWrapper<ApiHeathMonitor> buildLambda() {
        return super.buildLambda()
                .eq(Objects.nonNull(serviceId), ApiHeathMonitor::getServiceId, serviceId)
                .eq(Objects.nonNull(apiName), ApiHeathMonitor::getApiName, apiName);
    }

}
