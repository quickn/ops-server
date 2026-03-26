package com.bszn.monitor.project;

import cn.hutool.core.bean.BeanUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author wzh
 * @date 2026/1/14 13:49
 * @description:
 */
@Data
public class ProjectParam {

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "类型 1 jar包部署 2 docker部署")
    private Integer type;

    @Schema(description = "源文件目录")
    private Integer sourceDir;

    @Schema(description = "指向目录")
    private String targetDir;

    @Schema(description = "描述")
    private String remark;

    @Schema(description = "docker编排文件")
    private String dockerComposeContent;

    @Schema(description = "docker文件")
    private String dockerfileContent;

    public Project toEntity() {
        Project project = new Project();
        BeanUtil.copyProperties(this, project);
        return project;
    }

}
