package com.cloud.receiver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * Created by Liuyun on 2023-07-27 15:16
 **/
@Data
public class DockerContainer extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;

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


    /**
     * cpu使用率
     */
    private String cpu;

    /***
     * mem使用率
     */
    private String mem;

    /**
     * 更新时间
     */
    private Date updateTime;

}
