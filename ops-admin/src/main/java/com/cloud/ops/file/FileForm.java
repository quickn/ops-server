package com.cloud.ops.file;

import lombok.Data;

@Data
public class FileForm {
    private Long agentId;
    private String filePath;
    private String fileName;
    private String fileContent;

}
