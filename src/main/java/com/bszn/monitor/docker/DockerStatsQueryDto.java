package com.bszn.monitor.docker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bszn.base.sql.IQuery;
import com.bszn.base.sql.PageForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @author wzh
 * @date 2026/1/21 15:10
 * @description: 指令查询dto
 */
@Data
public class DockerStatsQueryDto extends PageForm<DockerStats> implements IQuery {

    @Schema(description = "环境id")
    private Long serviceId;

    @Schema(description = "服务id")
    private Long agentId;

    @Schema(description = "名称")
    private String names;


    @Override
    public LambdaQueryWrapper<DockerStats> buildLambda() {
        return super.buildLambda()
                .eq(Objects.nonNull(serviceId), DockerStats::getServiceId, serviceId)
                .eq(Objects.nonNull(agentId), DockerStats::getAgentId, agentId)
                .eq(StringUtils.isNotEmpty(names), DockerStats::getNames, names)
                .orderByDesc(DockerStats::getId);
    }
}
