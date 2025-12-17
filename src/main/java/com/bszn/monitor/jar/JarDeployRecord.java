package com.bszn.monitor.jar;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("jar_deploy_record")
public class JarDeployRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer jarPackageId;
    private Long agentId;
    private String containerName;
    private Integer status; // 0-待部署 1-部署中 2-成功 3-失败
    private Date deployTime;
    private String deployLog;
    private Date createTime;
}