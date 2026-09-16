package com.cloud.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.*;
import java.util.Enumeration;

/**
 * DESC
 * ip地址工具类
 *
 * @Author liuyun
 * @Date 2022-04-08
 */
@Slf4j
public class IpUtil {

    private static String IP = null;


    /**
     * 获取当前网络IPv4地址 优先取ipv4地址 取不到就用ipv6地址
     *
     * @return
     */
    public static String getIPv4Ip() {
        if (IP != null) {
            return IP;
        }
        Enumeration<NetworkInterface> nis;
        String ip = null;
        try {
            nis = NetworkInterface.getNetworkInterfaces();
            for (; nis.hasMoreElements(); ) {
                NetworkInterface ni = nis.nextElement();
                String name = ni.getName();
                if (!name.startsWith("e")) {
                    continue;
                }
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                for (; ias.hasMoreElements(); ) {
                    InetAddress ia = ias.nextElement();
                    if (ia instanceof Inet4Address) {
                        ip = ia.getHostAddress();
                        log.info("Inet4Address {} {}", name, ip);
                        if (ip != null) {
                            break;
                        }
                    }
                    //优先ipv4地址
                    if (ia instanceof Inet6Address && StringUtils.isEmpty(ip)) {
                        ip = ia.getHostAddress();
                        log.info("Inet6Address {} {}", name, ip);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            log.error("获取ip失败", e);
        }
        if (ip != null) {
            IP = ip.trim();
        }
        return IP;
    }

}
