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

    @ApiModelProperty("jar包id")
    private Long jarPackageId;

    @ApiModelProperty("服务id")
    private Integer serviceId;

    @ApiModelProperty("服务名")
    private String serviceName;

    public Project toEntity() {
        Project project = new Project();
        BeanUtil.copyProperties(this, project);
        return project;
    }

}
