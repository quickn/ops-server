package com.bszn.ops.file;

import lombok.Data;

/**
 * @author wzh
 * @date 2025/12/23 13:58
 * @description: 文件信息类
 */
@Data
public class FileInfo {
    private String name;
    private String type; // file, directory, link
    private String permissions;
    private Integer links;
    private String owner;
    private String group;
    private String size;
    private String modifyTime;
}
