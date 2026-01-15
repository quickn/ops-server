package com.bszn.utils;

import com.bszn.monitor.project.InstructionConstant;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wzh
 * @date 2026/1/15 9:35
 * @description: 脚本工具类
 */
public class ScriptUtil {


    /**
     * 部署脚本
     *
     * @param projectName          项目名
     * @param dockerfileContent    docker文件
     * @param jarPath              本地jar包路径
     * @param dockerComposeContent docker编排文件
     * @return 脚本
     */
    public static String deployScript(String projectName, String dockerfileContent, String dockerComposeContent, String jarPath) {
        String str = "";
        return deployScript(str, projectName, str, projectName, projectName, dockerfileContent, dockerComposeContent, jarPath);
    }

    /**
     * 重新部署脚本（只替换JAR包）
     *
     * @param projectName       项目名
     * @param dockerfileContent docker文件
     * @param jarPath           本地jar包路径
     * @return 脚本
     */
    public static String redeployScript(String projectName, String dockerfileContent, String jarPath) {
        String str = "";
        return redeployScript(str, projectName, str, projectName, dockerfileContent, jarPath);
    }


    /**
     * 部署脚本
     *
     * @param downloadUrl          jar包下载地址
     * @param fileName             文件名
     * @param version              版本号
     * @param imageName            镜像名
     * @param containerName        容器名
     * @param dockerfileContent    docker文件
     * @param jarPath              本地jar包路径
     * @param dockerComposeContent docker编排文件
     * @return 脚本
     */
    public static String deployScript(String downloadUrl, String fileName, String version, String imageName,
                                      String containerName, String dockerfileContent, String dockerComposeContent, String jarPath) {

        Map<String, String> stringStringMap = parseDockerfileInfo(dockerfileContent);
        String port = stringStringMap.get(InstructionConstant.EXPOSE);
        StringBuilder script = new StringBuilder();

        script.append("#!/bin/bash\n\n");
        script.append("# ======================================================\n");
        script.append("# Spring Boot部署脚本\n");
        script.append("# 支持 Docker 和 Docker Compose 两种方式\n");
        script.append("# ======================================================\n\n");

        script.append("set -e\n");
        script.append("set -o pipefail\n\n");

        // 基本变量
        script.append("JAR_URL=\"").append(downloadUrl).append("\"\n");
        script.append("JAR_NAME=\"").append(fileName).append("-").append(version).append(".jar\"\n");
        script.append("LOCAL_JAR_PATH=\"").append(jarPath).append("/").append(containerName).append("/").append(fileName).append(".jar\"\n");
        script.append("JAR_PATH=\"/tmp/$JAR_NAME\"\n");
        script.append("IMAGE_NAME=\"").append(imageName).append("\"\n");
        script.append("CONTAINER_NAME=\"").append(containerName).append("\"\n");
        script.append("PORT=\"").append(port).append("\"\n");
        script.append("BUILD_DIR=\"/tmp/build-${CONTAINER_NAME}-$(date +%s)\"\n");

        // Docker Compose 相关变量
        script.append("USE_DOCKER_COMPOSE=").append(dockerComposeContent != null && !dockerComposeContent.trim().isEmpty() ? "true" : "false").append("\n");
        script.append("DOCKER_COMPOSE_FILE=\"$BUILD_DIR/docker-compose.yml\"\n");

        // 日志函数
        script.append("log() { echo \"[$(date '+%Y-%m-%d %H:%M:%S')] $1\"; }\n");
        script.append("log_success() { echo \"[$(date '+%Y-%m-%d %H:%M:%S')] ✓ $1\"; }\n");
        script.append("log_error() { echo \"[$(date '+%Y-%m-%d %H:%M:%S')] ✗ $1\" >&2; }\n\n");

        script.append("echo \"========================================\"\n");
        if (dockerComposeContent != null && !dockerComposeContent.trim().isEmpty()) {
            script.append("echo \"     Spring Boot应用部署 (Docker Compose)\"\n");
        } else {
            script.append("echo \"         Spring Boot应用部署\"\n");
        }
        script.append("echo \"========================================\"\n\n");

        // 步骤1: 清理环境
        script.append("log \"1. 清理环境\"\n");
        script.append("docker stop \"$CONTAINER_NAME\" 2>/dev/null || true\n");
        script.append("docker rm -f \"$CONTAINER_NAME\" 2>/dev/null || true\n");
        script.append("\n");
        script.append("# 强制清理任何同名容器\n");
        script.append("docker ps -a --filter \"name=$CONTAINER_NAME\" --format \"{{.ID}}\" | xargs -r docker stop 2>/dev/null || true\n");
        script.append("docker ps -a --filter \"name=$CONTAINER_NAME\" --format \"{{.ID}}\" | xargs -r docker rm -f 2>/dev/null || true\n");
        script.append("\n");
        script.append("# 强制清理所有包含容器名的容器\n");
        script.append("docker ps -a --format \"{{.Names}}\" | grep \"$CONTAINER_NAME\" | xargs -r docker stop 2>/dev/null || true\n");
        script.append("docker ps -a --format \"{{.Names}}\" | grep \"$CONTAINER_NAME\" | xargs -r docker rm -f 2>/dev/null || true\n");
        script.append("\n");
        script.append("# 强制清理网络\n");
        script.append("docker network ls --filter \"name=$CONTAINER_NAME\" --format \"{{.Name}}\" | xargs -r docker network rm 2>/dev/null || true\n");
        script.append("sleep 3\n");
        script.append("log_success \"环境清理完成\"\n\n");

        // 步骤2: 检查端口占用
        script.append("log \"2. 检查端口占用\"\n");
        script.append("if ss -tln | grep -q :${PORT}; then\n");
        script.append("    log_error \"端口 ${PORT} 已被占用\"\n");
        script.append("    ss -tln | grep :${PORT}\n");
        script.append("    exit 1\n");
        script.append("else\n");
        script.append("    log_success \"端口 ${PORT} 可用\"\n");
        script.append("fi\n");
        script.append("echo \"\"\n");

        // 步骤3: 下载JAR文件（优先使用本地文件）
        script.append("log \"3. 获取JAR文件\"\n");
        script.append("rm -f \"$JAR_PATH\"\n");

        // 检查本地文件是否存在
        script.append("if [ -f \"$LOCAL_JAR_PATH\" ]; then\n");
        script.append("    log \"找到本地JAR文件: $LOCAL_JAR_PATH\"\n");
        script.append("    cp \"$LOCAL_JAR_PATH\" \"$JAR_PATH\"\n");
        script.append("    log_success \"使用本地JAR文件\"\n");
        script.append("else\n");
        script.append("    log \"本地文件不存在，从服务器下载\"\n");
        script.append("    curl -s -L -o \"$JAR_PATH\" \"$JAR_URL\"\n");
        script.append("    \n");
        script.append("    if [ ! -f \"$JAR_PATH\" ]; then\n");
        script.append("        log_error \"JAR文件不存在\"\n");
        script.append("        exit 1\n");
        script.append("    fi\n");
        script.append("    log_success \"下载完成\"\n");
        script.append("fi\n");

        script.append("JAR_SIZE=$(du -h \"$JAR_PATH\" | cut -f1)\n");
        script.append("log_success \"JAR文件就绪，大小: $JAR_SIZE\"\n\n");

        // 步骤4: 构建Docker镜像
        script.append("log \"4. 构建Docker镜像\"\n");

        script.append("mkdir -p \"$BUILD_DIR\"\n");
        script.append("cp \"$JAR_PATH\" \"$BUILD_DIR/").append(fileName).append(".jar\"\n");
        script.append("cd \"$BUILD_DIR\"\n\n");

        script.append("cat > Dockerfile << 'EOF'\n");
        script.append(dockerfileContent).append("\n");
        script.append("EOF\n\n");

        script.append("if docker build -t \"$IMAGE_NAME\" .; then\n");
        script.append("    log_success \"镜像构建成功\"\n");
        script.append("else\n");
        script.append("    log_error \"镜像构建失败\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        // 步骤5: 运行容器（Docker Compose 或 Docker）
        if (dockerComposeContent != null && !dockerComposeContent.trim().isEmpty()) {
            // 使用 Docker Compose
            script.append("log \"5. 配置 Docker Compose\"\n");
            script.append("cat > \"$DOCKER_COMPOSE_FILE\" << 'EOF'\n");
            script.append(dockerComposeContent).append("\n");
            script.append("EOF\n\n");

            script.append("log_success \"Docker Compose 配置文件已生成\"\n");

            // 处理 .env 文件
            script.append("log \"6. 处理环境变量文件\"\n");
            script.append("ENV_SOURCE=\"/home/park/docker/.env\"\n");
            script.append("ENV_TARGET=\"$BUILD_DIR/.env\"\n");
            script.append("ENV_FILE_EXISTS=false\n");
            script.append("\n");
            script.append("# 拷贝 .env 文件（如果存在）\n");
            script.append("if [ -f \"$ENV_SOURCE\" ]; then\n");
            script.append("    cp \"$ENV_SOURCE\" \"$ENV_TARGET\"\n");
            script.append("    ENV_FILE_EXISTS=true\n");
            script.append("    log_success \"已拷贝环境文件到构建目录\"\n");
            script.append("    echo \"环境文件内容:\"\n");
            script.append("    cat \"$ENV_TARGET\"\n");
            script.append("    echo \"\"\n");
            script.append("else\n");
            script.append("    log \"源环境文件不存在，将不使用环境文件\"\n");
            script.append("fi\n");
            script.append("\n");
            script.append("# 构建命令\n");
            script.append("if [ \"$ENV_FILE_EXISTS\" = true ]; then\n");
            script.append("    DOCKER_COMPOSE_CMD=\"docker-compose --env-file .env -f docker-compose.yml -p \\\"$CONTAINER_NAME\\\"\"\n");
            script.append("else\n");
            script.append("    DOCKER_COMPOSE_CMD=\"docker-compose -f docker-compose.yml -p \\\"$CONTAINER_NAME\\\"\"\n");
            script.append("fi\n");
            script.append("\n");
            script.append("log \"7. 使用 Docker Compose 启动服务\"\n");
            script.append("cd \"$BUILD_DIR\"\n");
            script.append("echo \"当前目录: $(pwd)\"\n");
            script.append("echo \"命令: $DOCKER_COMPOSE_CMD up -d\"\n");
            script.append("\n");
            script.append("if $DOCKER_COMPOSE_CMD up -d; then\n");
            script.append("    log_success \"Docker Compose 启动成功\"\n");
            script.append("    \n");
            script.append("    # 等待一会让容器完全启动\n");
            script.append("    sleep 2\n");
            script.append("    \n");
            script.append("    # 直接使用Docker命令获取容器ID\n");
            script.append("    CONTAINER_ID=$(docker ps --filter \"name=${CONTAINER_NAME}\" --format \"{{.ID}}\" | head -1)\n");
            script.append("    \n");
            script.append("    if [ -n \"$CONTAINER_ID\" ]; then\n");
            script.append("        log_success \"容器ID: ${CONTAINER_ID:0:12}\"\n");
            script.append("    else\n");
            script.append("        # 尝试查找任何包含容器名的容器\n");
            script.append("        CONTAINER_ID=$(docker ps --filter \"name=^/${CONTAINER_NAME}$\" --format \"{{.ID}}\" | head -1)\n");
            script.append("        if [ -n \"$CONTAINER_ID\" ]; then\n");
            script.append("            log_success \"找到容器ID: ${CONTAINER_ID:0:12}\"\n");
            script.append("        else\n");
            script.append("            log \"警告: 无法获取容器ID，但服务已启动\"\n");
            script.append("            CONTAINER_ID=\"unknown\"\n");
            script.append("        fi\n");
            script.append("    fi\n");
            script.append("else\n");
            script.append("    log_error \"Docker Compose 启动失败\"\n");
            script.append("    $DOCKER_COMPOSE_CMD logs\n");
            script.append("    exit 1\n");
            script.append("fi\n\n");

            script.append("log \"8. 等待Spring Boot启动完成（120秒）\"\n");
        } else {
            // 使用 Docker 直接运行
            script.append("log \"5. 运行容器\"\n");

            script.append("CONTAINER_ID=$(docker run -d \\\n");
            script.append("  --name \"$CONTAINER_NAME\" \\\n");
            script.append("  --restart=always \\\n");
            script.append("  -p ${PORT}:").append(port).append(" \\\n");
            script.append("  -e TZ=Asia/Shanghai \\\n");
            script.append("  -e JAVA_OPTS=\"-Xms256m -Xmx512m -Duser.timezone=Asia/Shanghai\" \\\n");
            script.append("  --log-opt max-size=10m \\\n");
            script.append("  --log-opt max-file=3 \\\n");
            script.append("  \"$IMAGE_NAME\")\n\n");

            script.append("if [ $? -eq 0 ] && [ -n \"$CONTAINER_ID\" ]; then\n");
            script.append("    log_success \"容器启动成功，ID: ${CONTAINER_ID:0:12}\"\n");
            script.append("else\n");
            script.append("    log_error \"容器启动失败\"\n");
            script.append("    exit 1\n");
            script.append("fi\n\n");

            script.append("log \"6. 等待Spring Boot启动完成（120秒）\"\n");
        }

        // 等待Spring Boot启动完成
        script.append("SUCCESS=false\n");
        script.append("for i in {1..120}; do\n");
        script.append("    sleep 1\n");
        script.append("    \n");
        script.append("    # 检查容器是否还在运行\n");
        script.append("    if [ \"$USE_DOCKER_COMPOSE\" = \"true\" ]; then\n");
        script.append("        # Docker Compose 检查 - 使用Docker命令\n");
        script.append("        if ! docker ps --filter \"name=^/${CONTAINER_NAME}$\" | grep -q \"${CONTAINER_NAME}\"; then\n");
        script.append("            log_error \"容器已停止运行\"\n");
        script.append("            # 尝试获取Docker Compose日志\n");
        script.append("            $DOCKER_COMPOSE_CMD logs 2>/dev/null || true\n");
        script.append("            break\n");
        script.append("        fi\n");
        script.append("        # 获取容器日志 - 直接使用docker logs\n");
        script.append("        CONTAINER_LOGS=$(docker logs \"$CONTAINER_NAME\" 2>&1 || true)\n");
        script.append("    else\n");
        script.append("        # Docker 检查\n");
        script.append("        if ! docker ps --filter \"name=$CONTAINER_NAME\" | grep -q \"$CONTAINER_NAME\"; then\n");
        script.append("            log_error \"容器已停止运行\"\n");
        script.append("            break\n");
        script.append("        fi\n");
        script.append("        # 获取容器日志\n");
        script.append("        CONTAINER_LOGS=$(docker logs \"$CONTAINER_NAME\" 2>&1 || true)\n");
        script.append("    fi\n");
        script.append("    \n");
        script.append("    # 检查Spring Boot启动关键词\n");
        script.append("    if echo \"$CONTAINER_LOGS\" | grep -q \"Tomcat started on port.*").append(port).append("\\|Started .*Application in\"; then\n");
        script.append("        log_success \"检测到Spring Boot启动成功\"\n");
        script.append("        SUCCESS=true\n");
        script.append("        break\n");
        script.append("    fi\n");
        script.append("    \n");
        if (dockerComposeContent != null && !dockerComposeContent.trim().isEmpty()) {
            script.append("    echo \"  [$i/120] 等待应用启动 (Docker Compose)...\"\n");
        } else {
            script.append("    echo \"  [$i/120] 等待应用启动...\"\n");
        }
        script.append("done\n\n");

        // 步骤: 输出结果
        if (dockerComposeContent != null && !dockerComposeContent.trim().isEmpty()) {
            script.append("log \"9. 部署结果\"\n");
        } else {
            script.append("log \"7. 部署结果\"\n");
        }

        script.append("if [ \"$SUCCESS\" = true ]; then\n");
        script.append("    echo \"\"\n");
        script.append("    echo \"========================================\"\n");
        script.append("    echo \"           🎉 部署成功！🎉\"\n");
        script.append("    echo \"========================================\"\n");
        script.append("    \n");
        script.append("    if [ \"$USE_DOCKER_COMPOSE\" = \"true\" ]; then\n");
        script.append("        # Docker Compose 信息\n");
        script.append("        echo \"🔹 部署方式: Docker Compose\"\n");
        script.append("        echo \"🔹 容器名称: $CONTAINER_NAME\"\n");
        script.append("        echo \"🔹 容器ID:   ${CONTAINER_ID:0:12}\"\n");
        script.append("        if [ \"$ENV_FILE_EXISTS\" = true ]; then\n");
        script.append("            echo \"🔹 环境文件: 已使用\"\n");
        script.append("        else\n");
        script.append("            echo \"🔹 环境文件: 未使用\"\n");
        script.append("        fi\n");
        script.append("        echo \"\"\n");
        script.append("        echo \"📊 容器状态:\"\n");
        script.append("        docker ps --filter \"name=^$CONTAINER_NAME$\" --format \"table {{.Names}}\\t{{.Status}}\\t{{.Ports}}\"\n");
        script.append("    else\n");
        script.append("        # Docker 信息\n");
        script.append("        CONTAINER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("        echo \"🔹 部署方式: Docker\"\n");
        script.append("        echo \"🔹 容器名称: $CONTAINER_NAME\"\n");
        script.append("        echo \"🔹 容器ID:   ${CONTAINER_ID:0:12}\"\n");
        script.append("        echo \"🔹 容器IP:   $CONTAINER_IP\"\n");
        script.append("        echo \"🔹 映射端口: $PORT -> ").append(port).append("\"\n");
        script.append("        echo \"\"\n");
        script.append("        echo \"📊 容器状态:\"\n");
        script.append("        docker ps --filter \"name=^$CONTAINER_NAME$\" --format \"table {{.Names}}\\t{{.Status}}\\t{{.Ports}}\"\n");
        script.append("    fi\n");
        script.append("    \n");
        script.append("    echo \"🔹 镜像版本: $IMAGE_NAME\"\n");
        script.append("    echo \"🔹 JAR文件:  $JAR_NAME\"\n");
        script.append("    echo \"🔹 启动时间: $(date '+%Y-%m-%d %H:%M:%S')\"\n");
        script.append("    echo \"\"\n");
        script.append("    \n");
        script.append("    if [ \"$USE_DOCKER_COMPOSE\" = \"true\" ]; then\n");
        script.append("        echo \"📝 应用启动日志:\"\n");
        script.append("        docker logs \"$CONTAINER_NAME\" 2>&1 | grep -E \"Starting|Tomcat started|Started .*Application\" | tail -5\n");
        script.append("    else\n");
        script.append("        echo \"📝 应用启动日志:\"\n");
        script.append("        docker logs \"$CONTAINER_NAME\" 2>&1 | grep -E \"Starting|Tomcat started|Started .*Application\" | tail -5\n");
        script.append("    fi\n");
        script.append("    echo \"\"\n");
        script.append("    echo \"✅ DEPLOY_SUCCESS\"\n");
        script.append("else\n");
        script.append("    log_error \"❌ 部署失败或超时\"\n");
        script.append("    echo \"\"\n");
        script.append("    echo \"🔍 错误诊断:\"\n");
        script.append("    \n");
        script.append("    if [ \"$USE_DOCKER_COMPOSE\" = \"true\" ]; then\n");
        script.append("        echo \"容器状态:\"\n");
        script.append("        docker ps --filter \"name=$CONTAINER_NAME\" --format \"table {{.Names}}\\t{{.Status}}\\t{{.Ports}}\"\n");
        script.append("        echo \"\"\n");
        script.append("        echo \"容器日志（最后20行）:\"\n");
        script.append("        docker logs \"$CONTAINER_NAME\" 2>&1 | tail -20\n");
        script.append("        # 清理失败的容器\n");
        script.append("        docker rm -f \"$CONTAINER_NAME\" 2>/dev/null || true\n");
        script.append("    else\n");
        script.append("        echo \"容器状态:\"\n");
        script.append("        docker inspect \"$CONTAINER_NAME\" 2>/dev/null | grep -E 'Status|ExitCode|Error|RestartCount' | head -6\n");
        script.append("        echo \"\"\n");
        script.append("        echo \"容器日志（最后20行）:\"\n");
        script.append("        docker logs \"$CONTAINER_NAME\" 2>&1 | tail -20\n");
        script.append("        # 清理失败的容器\n");
        script.append("        docker rm -f \"$CONTAINER_NAME\" 2>/dev/null || true\n");
        script.append("    fi\n");
        script.append("    \n");
        script.append("    # 检查端口是否被其他进程占用\n");
        script.append("    echo \"\"\n");
        script.append("    echo \"端口占用情况:\"\n");
        script.append("    ss -tlnp | grep \":$PORT\" || echo \"端口 $PORT 未被其他进程占用\"\n");
        script.append("    \n");
        script.append("    exit 1\n");
        script.append("fi\n");

        // 清理
        script.append("\n# 清理临时文件\n");
        script.append("log \"清理临时文件...\"\n");
        script.append("if [ -d \"$BUILD_DIR\" ]; then\n");
        script.append("    echo \"删除构建目录: $BUILD_DIR\"\n");
        script.append("    rm -rf \"$BUILD_DIR\" 2>/dev/null || true\n");
        script.append("    if [ $? -eq 0 ]; then\n");
        script.append("        log_success \"构建目录已删除\"\n");
        script.append("    else\n");
        script.append("        log \"警告: 构建目录删除失败，但可以忽略\"\n");
        script.append("    fi\n");
        script.append("else\n");
        script.append("    log \"构建目录不存在，无需清理\"\n");
        script.append("fi\n");
        script.append("\n");
        script.append("# 清理临时JAR文件\n");
        script.append("rm -f /tmp/*.jar 2>/dev/null || true\n");
        script.append("log_success \"所有临时文件已清理\"\n");

        return script.toString();
    }

    /**
     * 重新部署脚本（只替换JAR包）
     *
     * @param downloadUrl       jar包下载地址
     * @param fileName          文件名
     * @param version           版本号
     * @param containerName     容器名
     * @param dockerfileContent docker文件
     * @param jarPath           本地jar包路径
     * @return 脚本
     */
    public static String redeployScript(String downloadUrl, String fileName, String version,
                                        String containerName, String dockerfileContent, String jarPath) {
        Map<String, String> stringStringMap = parseDockerfileInfo(dockerfileContent);
        String workdir = stringStringMap.get(InstructionConstant.WORKDIR);
        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n\n");
        script.append("# JAR包重新部署脚本\n");
        script.append("# 文件: ").append(fileName).append("-").append(version).append(".jar\n");
        script.append("# 容器: ").append(containerName).append("\n\n");

        script.append("set -e  # 遇到错误立即退出\n");
        script.append("set -o pipefail  # 管道命令错误也退出\n\n");

        script.append("# 定义函数\n");
        script.append("log_info() {\n");
        script.append("    echo \"[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1\"\n");
        script.append("}\n\n");

        script.append("log_error() {\n");
        script.append("    echo \"[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1\" >&2\n");
        script.append("}\n\n");

        script.append("log_warn() {\n");
        script.append("    echo \"[WARN] $(date '+%Y-%m-%d %H:%M:%S') - $1\" >&2\n");
        script.append("}\n\n");

        script.append("# 检查容器是否可以执行命令\n");
        script.append("is_container_executable() {\n");
        script.append("    docker exec \"$CONTAINER_NAME\" echo \"ready\" >/dev/null 2>&1\n");
        script.append("    return $?\n");
        script.append("}\n\n");

        script.append("# 确保容器工作目录存在\n");
        script.append("ensure_workdir_exists() {\n");
        script.append("    log_info \"确保工作目录存在: $CONTAINER_WORKDIR\"\n");
        script.append("    \n");
        script.append("    # 如果容器可以执行命令，直接创建目录\n");
        script.append("    if is_container_executable; then\n");
        script.append("        docker exec \"$CONTAINER_NAME\" mkdir -p \"$CONTAINER_WORKDIR\" 2>/dev/null || {\n");
        script.append("            log_error \"无法创建目录（容器可执行）\"\n");
        script.append("            return 1\n");
        script.append("        }\n");
        script.append("        log_info \"✓ 目录创建成功\"\n");
        script.append("        return 0\n");
        script.append("    else\n");
        script.append("        # 容器不可执行，需要启动容器后创建\n");
        script.append("        log_info \"容器不可执行，尝试启动容器后创建目录...\"\n");
        script.append("        \n");
        script.append("        # 保存当前重启策略\n");
        script.append("        local current_restart\n");
        script.append("        current_restart=$(docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"no\")\n");
        script.append("        \n");
        script.append("        # 临时禁用重启\n");
        script.append("        docker update --restart=no \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("        \n");
        script.append("        # 启动容器\n");
        script.append("        if docker start \"$CONTAINER_NAME\" >/dev/null 2>&1; then\n");
        script.append("            sleep 3\n");
        script.append("            \n");
        script.append("            # 等待容器可执行\n");
        script.append("            local wait_time=10\n");
        script.append("            for i in $(seq 1 $wait_time); do\n");
        script.append("                if is_container_executable; then\n");
        script.append("                    # 创建目录\n");
        script.append("                    if docker exec \"$CONTAINER_NAME\" mkdir -p \"$CONTAINER_WORKDIR\" 2>/dev/null; then\n");
        script.append("                        log_info \"✓ 容器启动后目录创建成功\"\n");
        script.append("                        # 停止容器\n");
        script.append("                        docker stop \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("                        sleep 2\n");
        script.append("                        # 恢复重启策略\n");
        script.append("                        docker update --restart=\"$current_restart\" \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("                        return 0\n");
        script.append("                    else\n");
        script.append("                        log_error \"容器启动后仍无法创建目录\"\n");
        script.append("                        docker stop \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("                        docker update --restart=\"$current_restart\" \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("                        return 1\n");
        script.append("                    fi\n");
        script.append("                fi\n");
        script.append("                sleep 1\n");
        script.append("            done\n");
        script.append("            \n");
        script.append("            log_error \"容器启动但无法执行命令\"\n");
        script.append("            docker stop \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("            docker update --restart=\"$current_restart\" \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("            return 1\n");
        script.append("        else\n");
        script.append("            log_error \"无法启动容器\"\n");
        script.append("            docker update --restart=\"$current_restart\" \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("            return 1\n");
        script.append("        fi\n");
        script.append("    fi\n");
        script.append("}\n\n");

        script.append("# 复制JAR文件到容器\n");
        script.append("copy_jar_to_container() {\n");
        script.append("    log_info \"复制JAR文件到容器...\"\n");
        script.append("    \n");
        script.append("    # 方法1：直接使用docker cp\n");
        script.append("    log_info \"方法1: 使用docker cp\"\n");
        script.append("    if docker cp \"$JAR_PATH\" \"${CONTAINER_NAME}:${CONTAINER_JAR_PATH}\"; then\n");
        script.append("        log_info \"✓ docker cp 成功\"\n");
        script.append("        return 0\n");
        script.append("    fi\n");
        script.append("    \n");
        script.append("    log_warn \"方法1失败，尝试方法2...\"\n");
        script.append("    \n");
        script.append("    # 方法2：启动容器后复制\n");
        script.append("    log_info \"方法2: 启动容器后复制\"\n");
        script.append("    \n");
        script.append("    # 保存当前重启策略\n");
        script.append("    local current_restart\n");
        script.append("    current_restart=$(docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"no\")\n");
        script.append("    \n");
        script.append("    # 临时禁用重启\n");
        script.append("    docker update --restart=no \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("    \n");
        script.append("    # 启动容器\n");
        script.append("    if docker start \"$CONTAINER_NAME\" >/dev/null 2>&1; then\n");
        script.append("        sleep 3\n");
        script.append("        \n");
        script.append("        # 等待容器可执行\n");
        script.append("        local wait_time=10\n");
        script.append("        for i in $(seq 1 $wait_time); do\n");
        script.append("            if is_container_executable; then\n");
        script.append("                log_info \"容器已启动且可执行命令\"\n");
        script.append("                \n");
        script.append("                # 使用docker cp复制\n");
        script.append("                if docker cp \"$JAR_PATH\" \"${CONTAINER_NAME}:${CONTAINER_JAR_PATH}\"; then\n");
        script.append("                    log_info \"✓ 启动后复制成功\"\n");
        script.append("                    \n");
        script.append("                    # 验证文件\n");
        script.append("                    if docker exec \"$CONTAINER_NAME\" [ -f \"$CONTAINER_JAR_PATH\" ] 2>/dev/null; then\n");
        script.append("                        log_info \"✓ 文件验证成功\"\n");
        script.append("                        \n");
        script.append("                        # 设置权限\n");
        script.append("                        docker exec \"$CONTAINER_NAME\" chmod +x \"$CONTAINER_JAR_PATH\" 2>/dev/null || log_warn \"设置权限失败\"\n");
        script.append("                        \n");
        script.append("                        # 停止容器\n");
        script.append("                        docker stop \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("                        sleep 2\n");
        script.append("                        \n");
        script.append("                        # 恢复重启策略\n");
        script.append("                        docker update --restart=\"$current_restart\" \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("                        return 0\n");
        script.append("                    else\n");
        script.append("                        log_error \"文件复制但验证失败\"\n");
        script.append("                    fi\n");
        script.append("                else\n");
        script.append("                    log_error \"启动后复制失败\"\n");
        script.append("                fi\n");
        script.append("                \n");
        script.append("                # 停止容器并恢复\n");
        script.append("                docker stop \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("                docker update --restart=\"$current_restart\" \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("                return 1\n");
        script.append("            fi\n");
        script.append("            sleep 1\n");
        script.append("        done\n");
        script.append("        \n");
        script.append("        log_error \"容器启动但无法执行命令\"\n");
        script.append("        docker stop \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("        docker update --restart=\"$current_restart\" \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("        return 1\n");
        script.append("    else\n");
        script.append("        log_error \"无法启动容器\"\n");
        script.append("        docker update --restart=\"$current_restart\" \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("        return 1\n");
        script.append("    fi\n");
        script.append("}\n\n");

        script.append("# 启动容器并验证\n");
        script.append("start_and_verify_container() {\n");
        script.append("    log_info \"启动容器并验证...\"\n");
        script.append("    \n");
        script.append("    # 启动容器\n");
        script.append("    if ! docker start \"$CONTAINER_NAME\"; then\n");
        script.append("        log_error \"启动容器失败\"\n");
        script.append("        return 1\n");
        script.append("    fi\n");
        script.append("    \n");
        script.append("    log_info \"等待容器启动...\"\n");
        script.append("    \n");
        script.append("    # 等待容器状态变为running\n");
        script.append("    local timeout=30\n");
        script.append("    for i in $(seq 1 $timeout); do\n");
        script.append("        sleep 1\n");
        script.append("        \n");
        script.append("        local status\n");
        script.append("        status=$(docker inspect -f '{{.State.Status}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("        \n");
        script.append("        case \"$status\" in\n");
        script.append("            \"running\")\n");
        script.append("                log_info \"容器状态: running\"\n");
        script.append("                \n");
        script.append("                # 等待容器可执行命令\n");
        script.append("                if is_container_executable; then\n");
        script.append("                    log_info \"✓ 容器可执行命令\"\n");
        script.append("                    \n");
        script.append("                    # 检查Java进程\n");
        script.append("                    local java_process\n");
        script.append("                    java_process=$(docker exec \"$CONTAINER_NAME\" ps aux 2>/dev/null | grep -E \"java.*jar\" | head -1 || echo \"\")\n");
        script.append("                    \n");
        script.append("                    if [ -n \"$java_process\" ]; then\n");
        script.append("                        log_info \"✓ Java进程运行中:\"\n");
        script.append("                        echo \"    $java_process\"\n");
        script.append("                        return 0\n");
        script.append("                    else\n");
        script.append("                        # 检查容器日志是否有启动成功信息\n");
        script.append("                        local log_output\n");
        script.append("                        log_output=$(docker logs \"$CONTAINER_NAME\" --tail=20 2>/dev/null || echo \"\")\n");
        script.append("                        \n");
        script.append("                        if echo \"$log_output\" | grep -q -i \"started\\|running\\|ready\\|success\"; then\n");
        script.append("                            log_info \"✓ 应用日志显示启动成功\"\n");
        script.append("                            return 0\n");
        script.append("                        else\n");
        script.append("                            log_warn \"未找到Java进程，检查日志...\"\n");
        script.append("                            docker logs \"$CONTAINER_NAME\" --tail=30\n");
        script.append("                            # 容器在运行但没有Java进程，可能应用启动失败\n");
        script.append("                            return 1\n");
        script.append("                        fi\n");
        script.append("                    fi\n");
        script.append("                else\n");
        script.append("                    log_warn \"容器运行但无法执行命令，继续等待...\"\n");
        script.append("                fi\n");
        script.append("                ;;\n");
        script.append("            \n");
        script.append("            \"restarting\")\n");
        script.append("                log_error \"容器进入重启循环\"\n");
        script.append("                docker logs \"$CONTAINER_NAME\" --tail=20 2>/dev/null || echo \"无法获取日志\"\n");
        script.append("                docker stop \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("                return 1\n");
        script.append("                ;;\n");
        script.append("            \n");
        script.append("            \"exited\")\n");
        script.append("                log_error \"容器已退出\"\n");
        script.append("                local exit_code\n");
        script.append("                exit_code=$(docker inspect -f '{{.State.ExitCode}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("                log_error \"退出码: $exit_code\"\n");
        script.append("                docker logs \"$CONTAINER_NAME\" --tail=30 2>/dev/null || echo \"无法获取日志\"\n");
        script.append("                return 1\n");
        script.append("                ;;\n");
        script.append("            \n");
        script.append("            *)\n");
        script.append("                if [ $i -eq $timeout ]; then\n");
        script.append("                    log_error \"容器启动超时，状态: $status\"\n");
        script.append("                    docker logs \"$CONTAINER_NAME\" --tail=30 2>/dev/null || echo \"无法获取日志\"\n");
        script.append("                    return 1\n");
        script.append("                fi\n");
        script.append("                ;;\n");
        script.append("        esac\n");
        script.append("    done\n");
        script.append("    \n");
        script.append("    log_error \"启动验证超时\"\n");
        script.append("    return 1\n");
        script.append("}\n\n");

        script.append("cleanup() {\n");
        script.append("    log_info \"清理临时文件...\"\n");
        script.append("    rm -f /tmp/*.jar 2>/dev/null || true\n");
        script.append("}\n\n");

        script.append("# 设置trap，确保脚本退出时清理\n");
        script.append("trap cleanup EXIT\n\n");

        script.append("# 定义变量\n");
        script.append("JAR_URL=\"").append(downloadUrl).append("\"\n");
        script.append("JAR_NAME=\"").append(fileName).append("-").append(version).append(".jar\"\n");
        script.append("LOCAL_JAR_PATH=\"").append(jarPath).append("/").append(containerName).append("/").append(fileName).append(".jar\"\n");
        script.append("JAR_PATH=\"/tmp/$JAR_NAME\"\n");
        script.append("CONTAINER_NAME=\"").append(containerName).append("\"\n");
        script.append("CONTAINER_WORKDIR=\"\"\n");
        script.append("CONTAINER_JAR_PATH=\"\"\n\n");

        script.append("# 检查容器是否存在\n");
        script.append("log_info \"检查容器是否存在...\"\n");
        script.append("if ! docker ps -a --filter \"name=^${CONTAINER_NAME}$\" --format '{{.Names}}' | grep -q \"${CONTAINER_NAME}\"; then\n");
        script.append("    log_error \"容器 ${CONTAINER_NAME} 不存在\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        // 获取容器配置
        script.append("# 获取容器当前配置\n");
        script.append("CONTAINER_WORKDIR=$(docker inspect -f '{{.Config.WorkingDir}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"").append(workdir).append("\")\n");
        script.append("CONTAINER_JAR_PATH=\"${CONTAINER_WORKDIR}/").append(fileName).append(".jar\"\n");
        script.append("if [ -z \"$CONTAINER_WORKDIR\" ]; then\n");
        script.append("    CONTAINER_WORKDIR=\"").append(workdir).append("\"  # 默认工作目录\n");
        script.append("fi\n");
        script.append("log_info \"容器工作目录: $CONTAINER_WORKDIR\"\n");
        script.append("log_info \"容器内JAR路径: $CONTAINER_JAR_PATH\"\n\n");

        script.append("# 获取当前容器状态\n");
        script.append("CONTAINER_STATUS=$(docker inspect -f '{{.State.Status}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("log_info \"容器状态: $CONTAINER_STATUS\"\n\n");

        script.append("# 确保容器已停止\n");
        script.append("if [ \"$CONTAINER_STATUS\" = \"running\" ] || [ \"$CONTAINER_STATUS\" = \"restarting\" ]; then\n");
        script.append("    log_info \"停止容器...\"\n");
        script.append("    docker stop \"$CONTAINER_NAME\" >/dev/null 2>&1 || docker kill \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("    sleep 3\n");
        script.append("fi\n\n");

        script.append("# 获取JAR文件\n");
        script.append("log_info \"获取JAR文件: $JAR_NAME\"\n");
        script.append("rm -f \"$JAR_PATH\" 2>/dev/null || true\n");
        script.append("\n");
        script.append("if [ -f \"$LOCAL_JAR_PATH\" ]; then\n");
        script.append("    log_info \"找到本地JAR文件，使用本地文件\"\n");
        script.append("    cp \"$LOCAL_JAR_PATH\" \"$JAR_PATH\"\n");
        script.append("else\n");
        script.append("    log_info \"本地文件不存在，从服务器下载\"\n");
        script.append("    if ! curl -L -o \"$JAR_PATH\" \"$JAR_URL\"; then\n");
        script.append("        log_error \"JAR文件下载失败\"\n");
        script.append("        exit 1\n");
        script.append("    fi\n");
        script.append("fi\n\n");

        script.append("# 验证JAR文件\n");
        script.append("log_info \"验证JAR文件...\"\n");
        script.append("if [ ! -f \"$JAR_PATH\" ]; then\n");
        script.append("    log_error \"JAR文件不存在\"\n");
        script.append("    exit 1\n");
        script.append("fi\n");
        script.append("\n");
        script.append("FILE_SIZE=$(wc -c < \"$JAR_PATH\" 2>/dev/null | tr -d ' ')\n");
        script.append("if [ \"$FILE_SIZE\" -eq 0 ]; then\n");
        script.append("    log_error \"JAR文件为空\"\n");
        script.append("    exit 1\n");
        script.append("fi\n");
        script.append("log_info \"JAR文件大小: ${FILE_SIZE} bytes\"\n\n");

        script.append("# 确保工作目录存在\n");
        script.append("if ! ensure_workdir_exists; then\n");
        script.append("    log_error \"无法确保工作目录存在\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 复制JAR文件\n");
        script.append("if ! copy_jar_to_container; then\n");
        script.append("    log_error \"复制JAR文件失败\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 验证文件已成功复制\n");
        script.append("log_info \"验证文件已成功复制...\"\n");
        script.append("if ! docker exec \"$CONTAINER_NAME\" [ -f \"$CONTAINER_JAR_PATH\" ] 2>/dev/null; then\n");
        script.append("    # 如果容器不可执行，尝试启动容器验证\n");
        script.append("    log_info \"容器不可执行，启动容器验证文件...\"\n");
        script.append("    \n");
        script.append("    docker start \"$CONTAINER_NAME\" >/dev/null 2>&1\n");
        script.append("    sleep 3\n");
        script.append("    \n");
        script.append("    if docker exec \"$CONTAINER_NAME\" [ -f \"$CONTAINER_JAR_PATH\" ] 2>/dev/null; then\n");
        script.append("        log_info \"✓ 文件验证成功\"\n");
        script.append("        docker stop \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("        sleep 2\n");
        script.append("    else\n");
        script.append("        log_error \"✗ 文件验证失败，JAR文件未成功复制\"\n");
        script.append("        docker stop \"$CONTAINER_NAME\" >/dev/null 2>&1 || true\n");
        script.append("        exit 1\n");
        script.append("    fi\n");
        script.append("else\n");
        script.append("    log_info \"✓ 文件验证成功\"\n");
        script.append("fi\n\n");

        script.append("# 启动容器\n");
        script.append("log_info \"启动容器...\"\n");
        script.append("if ! start_and_verify_container; then\n");
        script.append("    log_error \"容器启动失败\"\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("# 输出成功信息\n");
        script.append("log_info \"重新部署成功！\"\n");
        script.append("echo \"=== 部署成功信息 ===\"\n");
        script.append("echo \"容器名称: $CONTAINER_NAME\"\n");
        script.append("CONTAINER_ID=$(docker inspect -f '{{.Id}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("echo \"容器ID: $CONTAINER_ID\"\n");
        script.append("CONTAINER_STATUS=$(docker inspect -f '{{.State.Status}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("echo \"容器状态: $CONTAINER_STATUS\"\n");
        script.append("CONTAINER_IMAGE=$(docker inspect -f '{{.Config.Image}}' \"$CONTAINER_NAME\" 2>/dev/null || echo \"unknown\")\n");
        script.append("echo \"容器镜像: $CONTAINER_IMAGE\"\n");
        script.append("if [ \"$CONTAINER_STATUS\" = \"running\" ]; then\n");
        script.append("    if is_container_executable; then\n");
        script.append("        JAVA_PROCESS=$(docker exec \"$CONTAINER_NAME\" ps aux 2>/dev/null | grep -E \"java.*jar\" | head -1 | awk '{print $11, $12}' || echo \"未找到\")\n");
        script.append("        echo \"Java进程: $JAVA_PROCESS\"\n");
        script.append("        echo \"启动时间: $(docker inspect -f '{{.State.StartedAt}}' \"$CONTAINER_NAME\" 2>/dev/null | cut -d'.' -f1 || echo \"未知\")\"\n");
        script.append("    else\n");
        script.append("        echo \"Java进程: 容器运行但无法执行命令\"\n");
        script.append("    fi\n");
        script.append("else\n");
        script.append("    echo \"Java进程: 容器未运行\"\n");
        script.append("fi\n");
        script.append("echo \"JAR版本: ").append(version).append("\"\n");
        script.append("echo \"部署时间: $(date '+%Y-%m-%d %H:%M:%S')\"\n");
        script.append("echo \"DEPLOY_SUCCESS\"\n");

        return script.toString();
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
        String[] instructions = {InstructionConstant.FROM, InstructionConstant.EXPOSE, InstructionConstant.WORKDIR, InstructionConstant.ENTRYPOINT};
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
