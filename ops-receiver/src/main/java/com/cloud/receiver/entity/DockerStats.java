package com.cloud.receiver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * Created by Liuyun on 2023-07-29 15:01
 **/
@Data
public class DockerStats extends BaseEntity {

    //{{.Name}}|{{.CPUPerc}}|{{.MemUsage}}|{{.MemPerc}}|{{.BlockIO}}|{{.NetIO}}\"";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String names;

    private String hostname;

    private Long agentId;

    /***
     * cpu使用率
     */
    private Double cpu;
    /**
     * 内存使用率
     */
    private Double mem;
    /**
     * 内存使用量
     */
    private String memUsage;

    /**
     * 磁盘IO
     */
    private String blockIo;


    /**
     * IO
     */
    private String netIo;


}
