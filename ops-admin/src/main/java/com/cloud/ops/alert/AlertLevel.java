package com.cloud.ops.alert;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预警级别
 *
 * @author Liuyun
 */
@Getter
@AllArgsConstructor
public enum AlertLevel {

    INFO(1, "通知"),
    WARN(2, "警告"),
    CRITICAL(3, "严重");

    private final int code;
    private final String desc;

    public static AlertLevel fromCode(int code) {
        for (AlertLevel level : values()) {
            if (level.getCode() == code) {
                return level;
            }
        }
        return WARN;
    }
}
