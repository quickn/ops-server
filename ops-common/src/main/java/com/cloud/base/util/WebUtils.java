package com.cloud.base.util;

import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


/**
 * Miscellaneous utilities for web applications.
 *
 * @author L.cm
 */
@Slf4j
public class WebUtils {

    private static String IP = null;

    public static String getServerIp() {
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

    public static long ipToLong(String strIp) {
        String[] ip = strIp.split("\\.");
        return (Long.parseLong(ip[0]) << 24) + (Long.parseLong(ip[1]) << 16) + (Long.parseLong(ip[2]) << 8) + Long.parseLong(ip[3]);
    }
}

