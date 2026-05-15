package com.bszn.monitor.file;

import lombok.Data;

@Data
public class FileForm {
    private Long agentId;
    private String filePath;
    private String fileName;
    private String fileContent;

}
