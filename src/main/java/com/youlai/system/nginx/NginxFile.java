package com.youlai.system.nginx;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by Liuyun on 2023-09-06 14:43
 **/
@Data
public class NginxFile implements Serializable {

    private Integer fileId;
    private String fileName;
    private String filePath;

}
