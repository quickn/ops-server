package com.bszn.monitor.project;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bszn.base.sql.IQuery;
import com.bszn.base.sql.PageForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Created by Liuyun on 2023-09-09 11:51
 **/
@Data
public class ProjectDto extends PageForm<Project> implements IQuery {

    @Schema(description = "项目名")
    private String name;

    /**
     * 链式wrapper构建
     */
    public LambdaQueryWrapper<Project> buildLambda() {
        return Wrappers.<Project>lambdaQuery()
                .like(StrUtil.isNotEmpty(name), Project::getName, name).orderByDesc(Project::getUpdateTime);
    }

}
