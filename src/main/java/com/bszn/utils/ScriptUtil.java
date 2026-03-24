package com.bszn.utils;

import com.bszn.monitor.project.InstructionConstant;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wzh
 * @date 2026/1/15 9:35
 * @description: 脚本工具类
 */
public class ScriptUtil {

    /**
     * 读取模板文件 - 使用ClassLoader
     */
    private static String readTemplate(String templateName) throws IOException {
        try (InputStream inputStream = ScriptUtil.class.getClassLoader()
                .getResourceAsStream("scripts/" + templateName)) {
            if (inputStream == null) {
                throw new RuntimeException("找不到模板文件: scripts/" + templateName);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    /**
     * 替换模板中的变量
     */
    private static String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = StringUtils.defaultString(entry.getValue(), "");
            result = result.replace("${" + entry.getKey() + "}", value);
        }
        return result;
    }

    /**
     * 部署脚本
     *
     * @param projectName          项目名
     * @param dockerfileContent    docker文件
     * @param dockerComposeContent docker编排文件
     * @param jarPath              jar包路径
     * @param type                 类型 1 jar部署脚本 2 docker部署脚本 3 替换jar包部署脚本
     * @return 部署脚本
     */
    public static String deployScript(String projectName, String dockerfileContent,
                                      String dockerComposeContent, String jarPath, Integer type) {
        switch (type) {
            case 1:
                return deployScript(projectName, dockerfileContent, dockerComposeContent, jarPath);
            case 2:
                return deployDockerScript(projectName, dockerfileContent, dockerComposeContent);
            case 3:
                return redeployScript(projectName, dockerfileContent, jarPath);
            default:
                return null;
        }
    }

    /**
     * 部署脚本
     */
    public static String deployDockerScript(String projectName, String dockerfileContent,
                                            String dockerComposeContent) {
        return dockerScript(projectName, dockerfileContent, dockerComposeContent);
    }

    /**
     * 部署脚本
     */
    public static String deployScript(String projectName, String dockerfileContent,
                                      String dockerComposeContent, String jarPath) {
        String str = "";
        return deployScript(str, projectName, str, projectName, projectName,
                dockerfileContent, dockerComposeContent, jarPath);
    }

    /**
     * 重新部署脚本（只替换JAR包）
     */
    public static String redeployScript(String projectName, String dockerfileContent, String jarPath) {
        String str = "";
        return redeployScript(str, projectName, str, projectName, dockerfileContent, jarPath);
    }

    /**
     * 部署脚本
     */
    public static String deployScript(String downloadUrl, String fileName, String version,
                                      String imageName, String containerName,
                                      String dockerfileContent, String dockerComposeContent,
                                      String jarPath) {
        try {
            Map<String, String> dockerInfo = parseDockerfileInfo(dockerfileContent);
            String exposePort = dockerInfo.get(InstructionConstant.EXPOSE);

            // 读取模板
            String template = readTemplate("deploy-template.sh");

            // 构建变量映射
            Map<String, String> variables = new HashMap<>();

            // 基本变量
            variables.put("JAR_URL", downloadUrl);
            variables.put("JAR_NAME", fileName + "-" + version + ".jar");
            variables.put("LOCAL_JAR_PATH", jarPath + "/" + containerName + "/" + fileName + ".jar");
            variables.put("IMAGE_NAME", imageName);
            variables.put("CONTAINER_NAME", containerName);
            variables.put("PORT", exposePort);
            variables.put("EXPOSE_PORT", exposePort);
            variables.put("FILE_NAME", fileName);
            variables.put("USE_DOCKER_COMPOSE",
                    StringUtils.isNotBlank(dockerComposeContent) ? "true" : "false");
            variables.put("DOCKERFILE_CONTENT", dockerfileContent);
            variables.put("DOCKER_COMPOSE_CONTENT",
                    StringUtils.defaultString(dockerComposeContent, ""));

            return replaceVariables(template, variables);
        } catch (IOException e) {
            throw new RuntimeException("读取部署脚本模板失败", e);
        }
    }

    /**
     * docker部署脚本
     */
    public static String dockerScript(String fileName, String dockerfileContent, String dockerComposeContent) {
        try {
            Map<String, String> dockerInfo = parseDockerfileInfo(dockerfileContent);
            String exposePort = dockerInfo.get(InstructionConstant.EXPOSE);

            // 读取模板
            String template = readTemplate("deploy-template.sh");

            // 构建变量映射
            Map<String, String> variables = new HashMap<>();

            // 基本变量
            variables.put("IMAGE_NAME", fileName);
            variables.put("CONTAINER_NAME", fileName);
            variables.put("PORT", exposePort);
            variables.put("EXPOSE_PORT", exposePort);
            variables.put("FILE_NAME", fileName);
            variables.put("USE_DOCKER_COMPOSE",
                    StringUtils.isNotBlank(dockerComposeContent) ? "true" : "false");
            variables.put("DOCKERFILE_CONTENT", dockerfileContent);
            variables.put("DOCKER_COMPOSE_CONTENT",
                    StringUtils.defaultString(dockerComposeContent, ""));

            return replaceVariables(template, variables);
        } catch (IOException e) {
            throw new RuntimeException("读取docker部署脚本模板失败", e);
        }
    }


    /**
     * 重新部署脚本（只替换JAR包）
     */
    public static String redeployScript(String downloadUrl, String fileName, String version,
                                        String containerName, String dockerfileContent,
                                        String jarPath) {
        try {
            Map<String, String> dockerInfo = parseDockerfileInfo(dockerfileContent);
            String workdir = dockerInfo.get(InstructionConstant.WORKDIR);

            // 读取模板
            String template = readTemplate("redeploy-template.sh");

            // 构建变量映射
            Map<String, String> variables = new HashMap<>();

            // 基本变量
            variables.put("JAR_URL", downloadUrl);
            variables.put("JAR_NAME", fileName + "-" + version + ".jar");
            variables.put("LOCAL_JAR_PATH", jarPath + "/" + containerName + "/" + fileName + ".jar");
            variables.put("CONTAINER_NAME", containerName);
            variables.put("FILE_NAME", fileName);
            variables.put("DEFAULT_WORKDIR", StringUtils.defaultString(workdir, "/app"));
            variables.put("VERSION", version);

            return replaceVariables(template, variables);
        } catch (IOException e) {
            throw new RuntimeException("读取重新部署脚本模板失败", e);
        }
    }


    /**
     * 解析Dockerfile获取配置信息
     */
    public static Map<String, String> parseDockerfileInfo(String dockerfileContent) {
        Map<String, String> info = new HashMap<>();
        if (StringUtils.isBlank(dockerfileContent)) {
            return info;
        }
        String[] lines = dockerfileContent.split("\n");
        String[] instructions = {InstructionConstant.FROM, InstructionConstant.EXPOSE,
                InstructionConstant.WORKDIR, InstructionConstant.ENTRYPOINT};
        for (String line : lines) {
            line = line.trim();
            for (String instruction : instructions) {
                // 解析指令
                if (line.startsWith(instruction)) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 1) {
                        info.put(instruction, parts[1]);
                    }
                }
            }
        }
        return info;
    }

    /**
     * 提取部署成功信息
     */
    public static String extractDeploySuccessInfo(String scriptResult) {
        StringBuilder info = new StringBuilder();
        String[] lines = scriptResult.split("\n");
        boolean inSuccessSection = false;

        for (String line : lines) {
            if (line.contains("=== 部署成功信息 ===")) {
                inSuccessSection = true;
                continue;
            }
            if (inSuccessSection && line.contains("DEPLOY_SUCCESS")) {
                break;
            }
            if (inSuccessSection) {
                info.append(line).append("\n");
            }
        }

        return info.toString().trim();
    }

    /**
     * 提取部署错误信息
     */
    public static String extractDeployErrorInfo(String scriptResult) {
        StringBuilder errorInfo = new StringBuilder();
        String[] lines = scriptResult.split("\n");
        for (String line : lines) {
            if (line.contains("[ERROR]")) {
                errorInfo.append(line).append("\n");
            }
        }
        if (errorInfo.isEmpty()) {
            // 如果没有明确的错误信息，返回最后10行
            int start = Math.max(0, lines.length - 10);
            for (int i = start; i < lines.length; i++) {
                errorInfo.append(lines[i]).append("\n");
            }
        }
        return errorInfo.toString().trim();
    }
}