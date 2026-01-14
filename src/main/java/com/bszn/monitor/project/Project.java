package com.bszn.monitor.project;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bszn.base.ServiceBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author wzh
 * @date 2026/1/14 13:38
 * @description:
 */
@Data
@TableName("project")
public class Project extends ServiceBaseEntity {

    @ApiModelProperty("项目名称")
    private String name;

    @ApiModelProperty("jar包id")
    private Long jarPackageId;

}
