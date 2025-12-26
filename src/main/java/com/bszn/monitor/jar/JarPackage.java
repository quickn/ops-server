package com.bszn.monitor.jar;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bszn.base.ServiceBaseEntity;
import lombok.Data;

import java.util.Date;

@Data
@TableName("jar_package")
public class JarPackage extends ServiceBaseEntity {

    private String fileName;
    private String originalName;
    private String version;
    private String remark;
    private String jarPath;

    private String downloadUrl;
    private String dockerImageName;
    private String dockerContainerName;
    private String dockerfilePath;
    private String dockerfileContent;
    private String agentIds;
    private String agentNames;
    private String targetContainerNames;

    private Integer status; // 0-未部署 1-部署中 2-部署成功 3-部署失败

}