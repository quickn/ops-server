package com.bszn.monitor.project;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author wzh
 * @date 2026/1/14 13:38
 * @description: 项目实体
 */
@Data
@TableName("project")
public class Project implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Serial
    private static final long serialVersionUID = 8698319936744959815L;

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

    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @ApiModelProperty("修改时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
