package com.bszn.system.common.util;

import cn.hutool.core.util.StrUtil;
import com.bszn.system.common.constant.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 请求工具类
 *
 * @author haoxr
 */
public class RequestUtils {

    private static final String UNKNOWN = "unknown";

    /**
     * 请求头解析获取 Token
     */
    public static String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstants.TOKEN_KEY);
        if (StrUtil.isNotBlank(bearerToken) && bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 获取客户端真实IP(兼容Nginx等反向代理场景)
     *
     * @param request 请求对象
     * @return 客户端IP
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalidIp(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getRemoteAddr();
        }
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            // X-Forwarded-For 可能包含多级代理IP，取第一个
            ip = ip.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    private static boolean isInvalidIp(String ip) {
        return StrUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip);
    }
}
