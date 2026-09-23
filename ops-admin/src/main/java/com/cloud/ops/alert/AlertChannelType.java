package com.cloud.ops.alert;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预警通道类型
 *
 * @author Liuyun
 */
@Getter
@AllArgsConstructor
public enum AlertChannelType {

    EMAIL("EMAIL", "邮件"),
    SMS("SMS", "短信");

    private final String code;
    private final String desc;

    public static AlertChannelType fromCode(String code) {
        for (AlertChannelType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
