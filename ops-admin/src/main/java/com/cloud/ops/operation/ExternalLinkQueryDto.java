package com.cloud.ops.operation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.base.sql.IQuery;
import com.cloud.base.sql.PageForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@Data
public class ExternalLinkQueryDto extends PageForm<ExternalLink> implements IQuery {

    @Schema(description = "环境id")
    private Long serviceId;

    @Schema(description = "名称")
    private String title;

    @Override
    public LambdaQueryWrapper<ExternalLink> buildLambda() {
        return super.buildLambda()
                .eq(Objects.nonNull(serviceId), ExternalLink::getServiceId, serviceId)
                .like(StringUtils.isNotEmpty(title), ExternalLink::getTitle, title)
                .orderByDesc(ExternalLink::getId);
    }
}
