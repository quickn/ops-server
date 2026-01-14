package com.bszn.monitor.project;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bszn.base.sql.IQuery;
import com.bszn.base.sql.PageForm;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Objects;

/**
 * Created by Liuyun on 2023-09-09 11:51
 **/
@Data
public class ProjectDto extends PageForm<Project> implements IQuery {

    @ApiModelProperty("项目名")
    private String name;

    @ApiModelProperty("服务id")
    private Integer serviceId;

    /**
     * 链式wrapper构建
     */
    public LambdaQueryWrapper<Project> buildLambda() {
        return Wrappers.<Project>lambdaQuery()
                .like(StrUtil.isNotEmpty(name), Project::getName, name)
                .eq(Objects.nonNull(serviceId), Project::getServiceId, serviceId);
    }

}
