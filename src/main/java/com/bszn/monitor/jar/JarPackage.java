package com.bszn.monitor.jar;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@TableName("jar_package")
public class JarPackage {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String fileName;
    private String originalName;
    private String version;
    private String remark;
    private String jarPath;
    
    // 多选Agent相关字段
    private String agentIds; // 存储多个agentId，用逗号分隔
    private String agentNames; // 存储对应的agent名称
    
    // 部署目标容器（多选，逗号分隔）
    private String targetContainerNames;
    private String targetContainerImages;
    
    private Integer status; // 0-未部署，1-部署中，2-部署成功，3-部署失败
    private Integer serviceId;
    private String serviceName;
    
    private Date createTime;
    private Date updateTime;
    
    // 获取Agent ID列表
    public List<Long> getAgentIdList() {
        if (agentIds == null || agentIds.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(agentIds.split(","))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .map(Long::valueOf)
                     .collect(Collectors.toList());
    }
    
    // 获取容器名称列表
    public List<String> getContainerNameList() {
        if (targetContainerNames == null || targetContainerNames.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(targetContainerNames.split(","))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toList());
    }
    
    // 设置Agent ID列表
    public void setAgentIdList(List<Long> agentIds) {
        this.agentIds = agentIds.stream()
                               .map(String::valueOf)
                               .collect(Collectors.joining(","));
    }
    
    // 设置容器名称列表
    public void setContainerNameList(List<String> containerNames) {
        this.targetContainerNames = String.join(",", containerNames);
    }
}