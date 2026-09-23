package com.cloud.ops.alert;

import com.cloud.base.sql.IQuery;
import com.cloud.base.sql.PageForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Objects;

/**
 * @author Liuyun
 */
@Data
public class AlertContactGroupQueryDto extends PageForm<AlertContactGroup> implements IQuery {

    @Schema(description = "组名称")
    private String groupName;

    @Override
    public com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AlertContactGroup> buildLambda() {
        return super.buildLambda()
                .like(Objects.nonNull(groupName) && !groupName.isEmpty(), AlertContactGroup::getGroupName, groupName)
                .orderByDesc(AlertContactGroup::getId);
    }
}
