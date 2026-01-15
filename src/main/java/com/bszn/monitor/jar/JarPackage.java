package com.bszn.monitor.jar;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bszn.base.ServiceBaseEntity;
import lombok.Data;

@Data
@TableName("jar_package")
public class JarPackage extends ServiceBaseEntity {

    private String fileName;
    private String originalName;
    private String version;
    private String remark;
    private String jarPath;
    private String downloadUrl;

}