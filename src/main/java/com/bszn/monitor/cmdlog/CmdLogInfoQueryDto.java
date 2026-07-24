package com.bszn.monitor.cmdlog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bszn.base.sql.IQuery;
import com.bszn.base.sql.PageForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Objects;

/**
 * @author wzh
 * @date 2026/1/21 15:10
 * @description: 指令查询dto
 */
@Data
public class CmdLogInfoQueryDto extends PageForm<CmdLogInfo> implements IQuery {

    @Schema(description = "环境id")
    private Long serviceId;

    @Schema(description = "agentIP")
    private String agentIP;

    @Schema(description = "指令")
    private String command;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "接收时间")
    private String endTime;


    @Override
    public LambdaQueryWrapper<CmdLogInfo> buildLambda() {
        return super.buildLambda()
                .eq(Objects.nonNull(serviceId), CmdLogInfo::getServiceId, serviceId)
                .eq(Objects.nonNull(agentIP), CmdLogInfo::getAgentIp, agentIP)
                .eq(Objects.nonNull(command), CmdLogInfo::getCommand, command)
                .eq(Objects.nonNull(userId), CmdLogInfo::getUserId, userId)
                .gt(Objects.nonNull(startTime), CmdLogInfo::getCreateTime, startTime)
                .lt(Objects.nonNull(endTime) && !endTime.equals(startTime), CmdLogInfo::getCreateTime, endTime)
                .orderByDesc(CmdLogInfo::getId);
    }
}
