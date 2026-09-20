package com.cloud.ops.docker;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.cloud.ops.base.MonitorBaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Created by Liuyun on 2023-07-27 15:16
 **/
@Data
public class DockerContainer extends MonitorBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String containerId;
    private String names;
    private String hostname;
    private String command;
    private String image;
    private String created;
    private String status;
    private String ports;

    private Boolean isMonitor;

    private Integer serviceId;

    @Schema(description = "服务器id")
    private Long agentId;


    /**
     * cpu使用率
     */
    private String cpu;

    /***
     * mem使用率
     */
    private String mem;

    private Integer sort;

    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
