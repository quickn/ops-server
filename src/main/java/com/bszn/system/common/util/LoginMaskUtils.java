package com.bszn.system.common.util;

import cn.hutool.core.util.StrUtil;

/**
 * 登录掩码(IP白名单)工具类
 * <p>
 * 登录掩码用于限制用户只能在指定的公网IP或IP网段登录，为空表示不限制。
 * 支持以下格式(多个之间使用英文逗号、中文逗号、分号或空白分隔)：
 * <ul>
 *     <li>单个IP：192.168.1.100</li>
 *     <li>通配符：192.168.1.*</li>
 *     <li>网段(CIDR)：192.168.1.0/24</li>
 *     <li>区间：192.168.1.10-192.168.1.200</li>
 * </ul>
 *
 * @author liuyun
 */
public class LoginMaskUtils {

    /**
     * 多个掩码之间的分隔符
     */
    private static final String SEPARATOR_REGEX = "[,;；，\\s]+";

    /**
     * IPv4 掩码全1
     */
    private static final long FULL_MASK = 0xFFFFFFFFL;

    private LoginMaskUtils() {
    }

    /**
     * 判断登录IP是否在登录掩码允许范围内
     *
     * @param clientIp  客户端登录IP
     * @param loginMask 登录掩码，为空表示不限制
     * @return true:允许登录; false:禁止登录
     */
    public static boolean matches(String clientIp, String loginMask) {
        // 未配置登录掩码，不做限制
        if (StrUtil.isBlank(loginMask)) {
            return true;
        }
        if (StrUtil.isBlank(clientIp)) {
            return false;
        }
        String ip = clientIp.trim();
        Long client = ipv4ToLong(ip);
        for (String item : loginMask.split(SEPARATOR_REGEX)) {
            if (StrUtil.isBlank(item)) {
                continue;
            }
            if (matchItem(ip, client, item.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验登录掩码格式是否合法
     *
     * @param loginMask 登录掩码，为空视为合法
     * @return true:合法; false:非法
     */
    public static boolean isValid(String loginMask) {
        if (StrUtil.isBlank(loginMask)) {
            return true;
        }
        for (String item : loginMask.split(SEPARATOR_REGEX)) {
            if (StrUtil.isBlank(item)) {
                continue;
            }
            if (!isValidItem(item.trim())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 匹配单个掩码项
     */
    private static boolean matchItem(String ip, Long client, String pattern) {
        // 精确匹配(兼容IPv6等非IPv4格式)
        if (pattern.equals(ip)) {
            return true;
        }
        if (client == null) {
            return false;
        }
        if (pattern.contains("/")) {
            return matchCidr(client, pattern);
        }
        if (pattern.contains("*")) {
            return matchWildcard(ip, pattern);
        }
        if (pattern.contains("-")) {
            return matchRange(client, pattern);
        }
        Long patternIp = ipv4ToLong(pattern);
        return patternIp != null && patternIp.equals(client);
    }

    /**
     * 匹配 CIDR 网段，如 192.168.1.0/24
     */
    private static boolean matchCidr(long client, String pattern) {
        String[] parts = pattern.split("/");
        if (parts.length != 2) {
            return false;
        }
        Long base = ipv4ToLong(parts[0]);
        Integer prefix = toPrefix(parts[1]);
        if (base == null || prefix == null) {
            return false;
        }
        long mask = maskOf(prefix);
        return (client & mask) == (base & mask);
    }

    /**
     * 匹配通配符，如 192.168.*.*
     */
    private static boolean matchWildcard(String ip, String pattern) {
        String[] patternOctets = pattern.split("\\.");
        String[] ipOctets = ip.split("\\.");
        if (patternOctets.length != 4 || ipOctets.length != 4) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if ("*".equals(patternOctets[i])) {
                continue;
            }
            if (!patternOctets[i].matches("\\d{1,3}") || !ipOctets[i].matches("\\d{1,3}")) {
                return false;
            }
            if (Integer.parseInt(patternOctets[i]) != Integer.parseInt(ipOctets[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 匹配 IP 区间，如 192.168.1.10-192.168.1.200
     */
    private static boolean matchRange(long client, String pattern) {
        String[] parts = pattern.split("-");
        if (parts.length != 2) {
            return false;
        }
        Long start = ipv4ToLong(parts[0].trim());
        Long end = ipv4ToLong(parts[1].trim());
        if (start == null || end == null) {
            return false;
        }
        long min = Math.min(start, end);
        long max = Math.max(start, end);
        return client >= min && client <= max;
    }

    /**
     * 校验单个掩码项格式
     */
    private static boolean isValidItem(String pattern) {
        if (pattern.contains("/")) {
            String[] parts = pattern.split("/");
            return parts.length == 2 && ipv4ToLong(parts[0]) != null && toPrefix(parts[1]) != null;
        }
        if (pattern.contains("*")) {
            String[] octets = pattern.split("\\.");
            if (octets.length != 4) {
                return false;
            }
            boolean wildcard = false;
            for (String octet : octets) {
                if ("*".equals(octet)) {
                    wildcard = true;
                    continue;
                }
                if (!octet.matches("\\d{1,3}") || Integer.parseInt(octet) > 255) {
                    return false;
                }
            }
            return wildcard;
        }
        if (pattern.contains("-")) {
            String[] parts = pattern.split("-");
            return parts.length == 2 && ipv4ToLong(parts[0].trim()) != null && ipv4ToLong(parts[1].trim()) != null;
        }
        return ipv4ToLong(pattern) != null;
    }

    /**
     * IPv4 地址转 long
     */
    private static Long ipv4ToLong(String ip) {
        if (StrUtil.isBlank(ip)) {
            return null;
        }
        String[] octets = ip.trim().split("\\.");
        if (octets.length != 4) {
            return null;
        }
        long value = 0L;
        for (String octet : octets) {
            if (!octet.matches("\\d{1,3}")) {
                return null;
            }
            int num = Integer.parseInt(octet);
            if (num < 0 || num > 255) {
                return null;
            }
            value = (value << 8) | num;
        }
        return value;
    }

    /**
     * 获取子网掩码
     */
    private static long maskOf(int prefix) {
        if (prefix <= 0) {
            return 0L;
        }
        if (prefix >= 32) {
            return FULL_MASK;
        }
        return (FULL_MASK << (32 - prefix)) & FULL_MASK;
    }

    /**
     * 解析网段前缀长度
     */
    private static Integer toPrefix(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            int prefix = Integer.parseInt(value.trim());
            return (prefix >= 0 && prefix <= 32) ? prefix : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
