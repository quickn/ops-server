#!/bin/bash

# ======================================================
# Spring Boot部署脚本
# 支持 Docker 和 Docker Compose 两种方式
# ======================================================

set -e
set -o pipefail

# 基本变量
JAR_URL=""
JAR_NAME="ops-server-.jar"
LOCAL_JAR_PATH="/home/park/docker/ops-server/ops-server.jar"
JAR_PATH="/tmp/ops-server-.jar"
IMAGE_NAME="ops-server"
CONTAINER_NAME="ops-server"
PORT="8989"
BUILD_DIR="/tmp/docker-ops-server"

if [ -d "${BUILD_DIR}" ]; then
  rm -rf "${BUILD_DIR}"
  echo "删除构建目录: ${BUILD_DIR}"
fi

ENV_PATH="/home/park/docker"

# Docker Compose 相关变量
USE_DOCKER_COMPOSE="true"
DOCKER_COMPOSE_FILE="$BUILD_DIR/docker-compose.yml"

# 日志函数
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"; }
log_success() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✓ $1"; }
log_error() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✗ $1" >&2; }

echo "========================================"
if [ "true" = "true" ]; then
    echo "     Spring Boot应用部署 (Docker Compose)"
else
    echo "         Spring Boot应用部署"
fi
echo "========================================"

# 步骤1: 清理环境
log "1. 清理环境"
docker stop "ops-server" 2>/dev/null || true
docker rm -f "ops-server" 2>/dev/null || true

# 强制清理任何同名容器
docker ps -a --filter "name=ops-server" --format "{{.ID}}" | xargs -r docker stop 2>/dev/null || true
docker ps -a --filter "name=ops-server" --format "{{.ID}}" | xargs -r docker rm -f 2>/dev/null || true

# 强制清理所有包含容器名的容器
docker ps -a --format "{{.Names}}" | grep "ops-server" | xargs -r docker stop 2>/dev/null || true
docker ps -a --format "{{.Names}}" | grep "ops-server" | xargs -r docker rm -f 2>/dev/null || true

# 强制清理网络
docker network ls --filter "name=ops-server" --format "{{.Name}}" | xargs -r docker network rm 2>/dev/null || true
sleep 3
log_success "环境清理完成"

# 步骤2: 检查端口占用
log "2. 检查端口占用"
if ss -tln | grep -q :8989; then
    log_error "端口 8989 已被占用"
    ss -tln | grep :8989
    exit 1
else
    log_success "端口 8989 可用"
fi
echo ""

# 步骤3: 下载JAR文件（优先使用本地文件）
log "3. 获取JAR文件"
rm -f "${JAR_PATH}"

# 检查本地文件是否存在
if [ -f "/home/park/docker/ops-server/ops-server.jar" ]; then
    log "找到本地JAR文件: /home/park/docker/ops-server/ops-server.jar"
    cp "/home/park/docker/ops-server/ops-server.jar" "${JAR_PATH}"
    log_success "使用本地JAR文件"
else
    log "本地文件不存在，从服务器下载"
    curl -s -L -o "${JAR_PATH}" ""

    if [ ! -f "${JAR_PATH}" ]; then
        log_error "JAR文件不存在"
        exit 1
    fi
    log_success "下载完成"
fi

JAR_SIZE=$(du -h "${JAR_PATH}" | cut -f1)
log_success "JAR文件就绪，大小: $JAR_SIZE"

# 步骤4: 构建Docker镜像
log "4. 构建Docker镜像"

mkdir -p "${BUILD_DIR}"
cp "${JAR_PATH}" "${BUILD_DIR}/ops-server.jar"
cd "${BUILD_DIR}"

cat > Dockerfile << 'EOF'
#FROM openjdk:17-ea-oracle
FROM docker.1ms.run/azul/zulu-openjdk:21

ARG profileActive
ENV profileActive=${profileActive}

ARG contextPath
ENV contextPath=${contextPath}

ARG uid
ENV uid=${uid}

ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime

RUN adduser park --uid ${uid}
USER park

RUN mkdir -p ${contextPath}
WORKDIR ${contextPath}

EXPOSE 8989

ADD ./ops-server.jar ./

ENTRYPOINT ["sh","-c","java -Djava.security.egd=file:/dev/./urandom -jar ./ops-server.jar --spring.profiles.active=prod"]

EOF

if [ "true" = "true" ]; then
    # Docker Compose 构建方式
    echo "# 生成 Docker Compose 配置文件"
    cat > "${DOCKER_COMPOSE_FILE}" << 'EOF'
services:
  ops-server:
    build:
      context: .
      dockerfile: ./Dockerfile
      args:
        profileActive: ${profileActive}
        contextPath: ${contextPath}
        uid: ${uid}
    image: ops-server
    container_name: ops-server
    restart: always
    ports:
      - 8989:8989
    extra_hosts:
      bs-nacos: ${bsNacos}
      bs-mysql: ${bsMysql}
      bs-redis: ${bsRedis}
      bs-rabbitmq: ${monitorRabbitmq}
      bs-job: 172.16.150.158
    network_mode: "host"
    hostname: ops-server
    volumes:
      - ${logPath}/ops-server:${logPath}
      - ${contextPath}/docker/config:${contextPath}/config
      - /etc/localtime:/etc/localtime
    logging:
      driver: json-file
      options:
        max-size: "1024m"
        max-file: "5"
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 200M
EOF

    log_success "Docker Compose 配置文件已生成"

    # 使用 docker-compose 构建
    echo "# 处理 .env 文件（支持动态变量）"
    ENV_SOURCE="/home/park/docker/.env"
    ENV_TARGET="${BUILD_DIR}/.env"
    ENV_FILE_EXISTS=false

    if [ -f "${ENV_SOURCE}" ]; then
        log "处理动态 .env 文件"

        # 清空目标文件
        > "${ENV_TARGET}"

        # 使用 source 命令加载并重新导出变量
        (
            # 在一个子shell中处理，避免污染当前环境
            set -a  # 自动导出所有变量

            # 加载原始 .env 文件
            source "${ENV_SOURCE}" 2>/dev/null || true

            # 导出所有变量到新文件，但只保留有效的变量名
            env | grep -v "^_" | grep -v "^SHLVL=" | grep -v "^PWD=" | grep -v "^OLDPWD=" \
                 | grep -v "^BASH" | grep -v "^SHELL=" | grep -v "^TERM=" | grep -v "^USER=" \
                 | while IFS= read -r line; do
                    # 只保留变量名格式正确的行（只包含字母、数字和下划线）
                    if [[ "$line" =~ ^[a-zA-Z_][a-zA-Z0-9_]*=.*$ ]]; then
                        echo "$line" >> "${ENV_TARGET}"
                    else
                        log "警告: 跳过无效变量: ${line%%=*}"
                    fi
                   done
        )

        # 清理空行
        sed -i '/^[[:space:]]*$/d' "${ENV_TARGET}"

        ENV_FILE_EXISTS=true
        log_success "环境文件处理完成"
        echo "生成的环境变量数量: $(wc -l < "${ENV_TARGET}")"
        echo "示例变量:"
        grep -E "^(UID|GID|os|contextPath)=" "${ENV_TARGET}" || head -5 "${ENV_TARGET}"
        echo ""
    else
        # 创建动态 .env 文件
        log "源环境文件不存在，创建动态环境文件"
        cat > "${ENV_TARGET}" << 'ENV_EOF'
UID=$(id -u)
GID=$(id -g)
DEPLOY_TIMESTAMP=$(date +%s)
ENV_EOF

        ENV_FILE_EXISTS=true
        log_success "已创建动态环境文件"
    fi

    # 构建命令
    DOCKER_COMPOSE_CMD="docker compose"
    echo "使用命令: $DOCKER_COMPOSE_CMD"

    # 使用 docker-compose 构建镜像
    if $DOCKER_COMPOSE_CMD up -d --build; then
        log_success "镜像构建成功 (通过 Docker Compose)"
    else
        log_error "镜像构建失败"
        exit 1
    fi
else
    # 普通的 Docker 构建
    if docker build -t "ops-server" .; then
        log_success "镜像构建成功"
    else
        log_error "镜像构建失败"
        exit 1
    fi
fi

# 步骤5: 运行容器（Docker Compose 或 Docker）
if [ "true" = "true" ]; then
    # 使用 Docker Compose 启动
    log "5. 使用 Docker Compose 启动服务"
    cd "${BUILD_DIR}"
    echo "当前目录: $(pwd)"
    echo "命令: $DOCKER_COMPOSE_CMD up -d"

    if $DOCKER_COMPOSE_CMD up -d; then
        log_success "Docker Compose 启动成功"

        # 等待一会让容器完全启动
        sleep 2

        # 获取容器ID
        CONTAINER_ID=$(docker ps --filter "name=ops-server" --format "{{.ID}}" | head -1)

        if [ -n "$CONTAINER_ID" ]; then
            log_success "容器ID: ${CONTAINER_ID:0:12}"
        else
            # 尝试查找任何包含容器名的容器
            CONTAINER_ID=$(docker ps --filter "name=^/ops-server$" --format "{{.ID}}" | head -1)
            if [ -n "$CONTAINER_ID" ]; then
                log_success "找到容器ID: ${CONTAINER_ID:0:12}"
            else
                log "警告: 无法获取容器ID，但服务已启动"
                CONTAINER_ID="unknown"
            fi
        fi
    else
        log_error "Docker Compose 启动失败"
        $DOCKER_COMPOSE_CMD logs
        exit 1
    fi

    log "6. 等待Spring Boot启动完成（120秒）"
else
    # 使用 Docker 直接运行
    log "5. 运行容器"

    CONTAINER_ID=$(docker run -d \
      --name "ops-server" \
      --restart=always \
      -p 8989:8989 \
      -e TZ=Asia/Shanghai \
      -e JAVA_OPTS="-Xms256m -Xmx512m -Duser.timezone=Asia/Shanghai" \
      --log-opt max-size=10m \
      --log-opt max-file=3 \
      "ops-server")

    if [ $? -eq 0 ] && [ -n "$CONTAINER_ID" ]; then
        log_success "容器启动成功，ID: ${CONTAINER_ID:0:12}"
    else
        log_error "容器启动失败"
        exit 1
    fi

    log "6. 等待Spring Boot启动完成（120秒）"
fi

# 等待Spring Boot启动完成
SUCCESS=false
for i in {1..120}; do
    sleep 1

    # 检查容器是否还在运行
    if ! docker ps --filter "name=ops-server" | grep -q "ops-server"; then
        log_error "容器已停止运行"
        if [ "true" = "true" ]; then
            # 尝试获取Docker Compose日志
            $DOCKER_COMPOSE_CMD logs 2>/dev/null || true
        fi
        break
    fi

    # 获取容器日志
    CONTAINER_LOGS=$(docker logs "ops-server" 2>&1 || true)

    # 检查Spring Boot启动关键词
    if echo "$CONTAINER_LOGS" | grep -q "Tomcat started on port.*8989\|Started .*Application in"; then
        log_success "检测到Spring Boot启动成功"
        SUCCESS=true
        break
    fi

    if [ "true" = "true" ]; then
        echo "  [$i/120] 等待应用启动 (Docker Compose)..."
    else
        echo "  [$i/120] 等待应用启动..."
    fi
done

# 步骤7: 输出结果
log "7. 部署结果"
if [ "$SUCCESS" = true ]; then
    echo ""
    echo "========================================"
    echo "           🎉 部署成功！🎉"
    echo "========================================"

    if [ "true" = "true" ]; then
        # Docker Compose 信息
        echo "🔹 部署方式: Docker Compose"
        echo "🔹 容器名称: ops-server"
        echo "🔹 容器ID:   ${CONTAINER_ID:0:12}"
        if [ "$ENV_FILE_EXISTS" = true ]; then
            echo "🔹 环境文件: 已使用（包含动态变量）"
            echo "🔹 动态UID:  $(grep '^UID=' "${ENV_TARGET}" 2>/dev/null | cut -d= -f2 || echo '未设置')"
        else
            echo "🔹 环境文件: 未使用"
        fi
        echo ""
        echo "📊 容器状态:"
        docker ps --filter "name=ops-server" --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"
        echo ""
        echo "📊 服务列表:"
        $DOCKER_COMPOSE_CMD ps
    else
        # Docker 信息
        CONTAINER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "ops-server" 2>/dev/null || echo "unknown")
        echo "🔹 部署方式: Docker"
        echo "🔹 容器名称: ops-server"
        echo "🔹 容器ID:   ${CONTAINER_ID:0:12}"
        echo "🔹 容器IP:   $CONTAINER_IP"
        echo "🔹 映射端口: 8989 -> 8989"
        echo ""
        echo "📊 容器状态:"
        docker ps --filter "name=ops-server" --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"
    fi

    echo "🔹 镜像版本: ops-server"
    echo "🔹 JAR文件:  ops-server-.jar"
    echo "🔹 启动时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""

    echo "📝 应用启动日志:"
    docker logs "ops-server" 2>&1 | grep -E "Starting|Tomcat started|Started .*Application" | tail -5
    echo ""
    echo "✅ DEPLOY_SUCCESS"
else
    log_error "❌ 部署失败或超时"
    echo ""
    echo "🔍 错误诊断:"

    echo "容器状态:"
    docker ps --filter "name=ops-server" --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"
    echo ""
    echo "容器日志（最后20行）:"
    docker logs "ops-server" 2>&1 | tail -20

    if [ "true" = "true" ]; then
        echo ""
        echo "Docker Compose 日志:"
        $DOCKER_COMPOSE_CMD logs 2>/dev/null || true
    fi

    # 检查端口是否被其他进程占用（只在 Docker 模式下）
    if [ "true" = "false" ]; then
        echo ""
        echo "端口占用情况:"
        ss -tlnp | grep ":8989" || echo "端口 8989 未被其他进程占用"
    fi

    # 清理失败的容器
    if [ "true" = "true" ]; then
        $DOCKER_COMPOSE_CMD down 2>/dev/null || true
    fi
    docker rm -f "ops-server" 2>/dev/null || true

    exit 1
fi

# 清理临时JAR文件
rm -f /tmp/*.jar 2>/dev/null || true
log_success "所有临时文件已清理"
