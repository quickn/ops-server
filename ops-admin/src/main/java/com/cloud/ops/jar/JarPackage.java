package com.cloud.ops.jar;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.ops.base.MonitorBaseEntity;
import lombok.Data;

@Data
@TableName("jar_package")
public class JarPackage extends MonitorBaseEntity {

    private String fileName;
    private String originalName;
    private String version;
    private String remark;
    private String jarPath;
    private String downloadUrl;

}