package com.bszn.monitor.project;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bszn.base.ServiceBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author wzh
 * @date 2026/1/14 13:38
 * @description: 项目实体
 */
@Data
@TableName("project")
public class Project extends ServiceBaseEntity {

    @ApiModelProperty("项目名称")
    private String name;

    @ApiModelProperty("docker编排文件")
    private String dockerComposeContent;

    @ApiModelProperty("docker文件")
    private String dockerfileContent;

    @ApiModelProperty("0-未部署 1-部署中 2-部署成功 3-部署失败")
    private Integer status;

    @ApiModelProperty("描述")
    private String remark;

}
