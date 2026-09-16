package com.cloud.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * DESC
 * ip地址工具类
 *
 * @Author liuyun
 * @Date 2022-04-08
 */
@Slf4j
public class CmdUtil {


    // 判断当前机器是否是windows
    public static Boolean isWindows() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("win");
    }

    public static String exec(boolean isSystem, String... command) throws Exception {
        return exec(isSystem, 10, false, command);
    }

    /**
     * 获取当前网络IPv4地址
     *
     * @return
     */
    public static String exec(boolean isSystem, Integer timeout, boolean isNewSession, String... command) throws Exception {
        if (timeout == null) {
            timeout = 10;
        }
        // 创建线程池管理进程
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            log.debug("执行指令 {}", command[0]);
            String[] cmd = null;
            String charsetName = null;
            if (isWindows()) {
                cmd = new String[]{"cmd.exe", "/c", command[0]};
                charsetName = "GB2312";
            } else {
                if (isNewSession) {
                    cmd = new String[]{"setsid", "/bin/sh", "-c", command[0]};
                } else {
                    cmd = new String[]{"/bin/sh", "-c", command[0]};
                }
                charsetName = "UTF-8";
            }
            Process process = null;
            if (isSystem) {
                process = Runtime.getRuntime().exec(cmd);
            } else {
                ProcessBuilder processBuilder = null;
                if (command.length == 1) {
                    processBuilder = new ProcessBuilder(cmd);
                } else {
                    log.debug("执行指令 {}", command[1]);
                    processBuilder = new ProcessBuilder(command);
                }
                process = processBuilder.start();
            }
            // 读取输出（防止缓冲区阻塞）
            Process finalProcess = process;
            String finalCharsetName = charsetName;
            Future<String> outputFuture = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(finalProcess.getInputStream(), finalCharsetName))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            });
            Future<String> errorFuture = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(finalProcess.getErrorStream(), finalCharsetName))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            });
            // 等待进程完成或超时
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy(); // 尝试正常终止
                if (process.isAlive()) {
                    process.destroyForcibly(); // 强制终止
                }
                throw new TimeoutException("指令执行超时");
            }
            // 获取输出结果
            String output = outputFuture.get(1, TimeUnit.SECONDS);
            String error = errorFuture.get(1, TimeUnit.SECONDS);
            if (StringUtils.isNotEmpty(error)) {
                int exitCode = process.exitValue();
                log.info("指令执行异常 {} {} ", exitCode, error);
                return output + error;
            }
            return output;
        } catch (Exception e) {
            log.error("指令执行异常 {}", command[0]);
            log.error("指令执行异常", e);
            throw e;
        } finally {
            executor.shutdownNow();
        }
    }

    public static String[] resultToArray(String result) {
        if (StringUtils.isEmpty(result)) {
            return null;
        }
        String[] arr = result.split("\n");
        return arr;
    }

}
