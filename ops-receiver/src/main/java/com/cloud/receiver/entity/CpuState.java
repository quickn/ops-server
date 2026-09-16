package com.cloud.receiver.entity;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;


@Data
public class CpuState extends BaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = -2913111613773445949L;


    /**
     * host名称
     */
    private String hostname;

    /**
     * cpu使用率
     */
    private Double sys;

    /**
     * 当前空闲率
     */
    private Double idle;

    /**
     * cpu当前等待率
     */
    private Double iowait;


    /**
     * 添加时间
     * MM-dd hh:mm:ss
     */
    private String dateStr;


    public String getDateStr() {
        if (!StringUtils.isEmpty(dateStr) && dateStr.length() > 16) {
            return dateStr.substring(11);
        }
        return dateStr;
    }


}
