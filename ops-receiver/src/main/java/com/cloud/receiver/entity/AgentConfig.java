package com.cloud.receiver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 代理服务配置实体类
 */
@Data
@TableName("agent_config")
public class AgentConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @Schema(description = "工作路径")
    private String workPath;


}
