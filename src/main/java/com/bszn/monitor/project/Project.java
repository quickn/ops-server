package com.bszn.monitor.project;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "项目实体")
public class Project implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Serial
    private static final long serialVersionUID = 8698319936744959815L;

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "docker编排文件")
    private String dockerComposeContent;

    @Schema(description = "docker文件")
    private String dockerfileContent;

    @Schema(description = "0-未部署 1-部署中 2-部署成功 3-部署失败")
    private Integer status;

    @Schema(description = "描述")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "修改时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
