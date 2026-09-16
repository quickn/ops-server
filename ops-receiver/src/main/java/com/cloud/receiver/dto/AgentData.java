package com.cloud.receiver.dto;

import lombok.Data;

@Data
public class AgentData {
    private String dataType;
    private String data;
    private String hostname;
    private Integer serviceId;
    private String serviceName;
    private Long agentId;
}
