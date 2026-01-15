package com.bszn.monitor.project;

import cn.hutool.core.bean.BeanUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author wzh
 * @date 2026/1/14 13:49
 * @description:
 */
@Data
public class ProjectParam {

    @ApiModelProperty("项目名称")
    private String name;

    @ApiModelProperty("描述")
    private String remark;

    @ApiModelProperty("docker编排文件")
    private String dockerComposeContent;

    @ApiModelProperty("docker文件")
    private String dockerfileContent;

    public Project toEntity() {
        Project project = new Project();
        BeanUtil.copyProperties(this, project);
        return project;
    }

}
