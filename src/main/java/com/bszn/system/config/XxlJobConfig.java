package com.bszn.system.config;

import com.bszn.utils.IpUtil;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * xxl-job config
 *
 * @author xuxueli 2017-04-28
 */
@Configuration
// system.config.xxl-job-enabled = true 才会自动装配
@ConditionalOnProperty(name = "system.config.xxl-job-enabled")
@Slf4j
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.address}")
    private String address;

    @Value("${xxl.job.executor.ip}")
    private String ip;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;


    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> xxl-job config init.");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);

        // 执行器 IP：未配置时自动探测本机网卡地址。
        // 避免 xxl-job 内部 IpUtil.getIp() 走 InetAddress.getLocalHost()，
        // 在容器/host 网络模式下 hostname（如 ops-server）无法解析时抛 UnknownHostException 导致启动失败。
        String executorIp = StringUtils.hasText(ip) ? ip.trim() : resolveLocalIp();
        xxlJobSpringExecutor.setIp(executorIp);
        // 注册地址：未配置时按 ip:port 生成，与 xxl-job 默认规则保持一致
        String executorAddress = StringUtils.hasText(address) ? address : "http://" + executorIp + ":" + port + "/";
        xxlJobSpringExecutor.setAddress(executorAddress);

        log.info(">>>>>>>>>>> xxl-job executor appname={}, ip={}, port={}, address={}", appname, executorIp, port, executorAddress);
        return xxlJobSpringExecutor;
    }

    /**
     * 探测本机 IP，优先使用项目工具类，兜底遍历网卡取非回环 IPv4，全程不依赖 hostname 解析。
     *
     * @return 本机 IP，探测失败时返回 127.0.0.1
     */
    private String resolveLocalIp() {
        String localIp = IpUtil.getIPv4Ip();
        if (StringUtils.hasText(localIp)) {
            return localIp;
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress() && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            log.error("获取本机 IP 失败", e);
        }
        log.warn(">>>>>>>>>>> 未获取到本机网卡 IP，xxl-job 执行器回退使用 127.0.0.1，调度端将无法回调，请显式配置 xxl.job.executor.ip");
        return "127.0.0.1";
    }

}
