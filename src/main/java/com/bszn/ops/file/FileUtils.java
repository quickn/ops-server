package com.bszn.ops.file;

import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    // 解析ls结果的方法
    public static List<FileInfo> parseLsResult(String lsOutput) {
        List<FileInfo> fileList = new ArrayList<>();
        if (lsOutput == null || lsOutput.trim().isEmpty()) {
            return fileList;
        }
        String[] lines = lsOutput.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("total")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length >= 9) {
                FileInfo fileInfo = new FileInfo();
                fileInfo.setPermissions(parts[0]);
                fileInfo.setLinks(Integer.parseInt(parts[1]));
                fileInfo.setOwner(parts[2]);
                fileInfo.setGroup(parts[3]);
                fileInfo.setSize(parts[4]);
                // 组合日期时间
                StringBuilder dateTime = new StringBuilder();
                for (int i = 5; i <= 7; i++) {
                    dateTime.append(parts[i]).append(" ");
                }
                fileInfo.setModifyTime(dateTime.toString().trim());
                // 文件名（可能包含空格）
                StringBuilder fileName = new StringBuilder();
                for (int i = 8; i < parts.length; i++) {
                    fileName.append(parts[i]).append(" ");
                }
                fileInfo.setName(fileName.toString().trim());

                // 判断类型
                if (parts[0].startsWith("d")) {
                    fileInfo.setType("directory");
                } else if (parts[0].startsWith("l")) {
                    fileInfo.setType("link");
                } else {
                    fileInfo.setType("file");
                }
                fileList.add(fileInfo);
            }
        }
        return fileList;
    }
}
