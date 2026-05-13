package com.bszn.nginx;

import lombok.Data;

@Data
public class NginxConf {
    private String conf;
    private String filePath;
    private String fileNamePath;
    private Long agentId;

}