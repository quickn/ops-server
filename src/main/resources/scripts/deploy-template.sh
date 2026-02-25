#!/bin/bash

# ======================================================
# Spring Boot部署脚本
# 支持 Docker 和 Docker Compose 两种方式
# ======================================================

set -e
set -o pipefail

# 基本变量
JAR_URL="${JAR_URL}"
JAR_NAME="${JAR_NAME}"
LOCAL_JAR_PATH="${LOCAL_JAR_PATH}"
JAR_PATH="/tmp/${JAR_NAME}"
IMAGE_NAME="${IMAGE_NAME}"
CONTAINER_NAME="${CONTAINER_NAME}"
PORT="${PORT}"
BUILD_DIR="/tmp/build-${CONTAINER_NAME}-$(date +%s)"

# Docker Compose 相关变量
USE_DOCKER_COMPOSE="${USE_DOCKER_COMPOSE}"
DOCKER_COMPOSE_FILE="$BUILD_DIR/docker-compose.yml"

# 日志函数
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"; }
log_success() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✓ $1"; }
log_error() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✗ $1" >&2; }

echo "========================================"
if [ "${USE_DOCKER_COMPOSE}" = "true" ]; then
    echo "     Spring Boot应用部署 (Docker Compose)"
else
    echo "         Spring Boot应用部署"
fi
echo "========================================"

# 步骤1: 清理环境
log "1. 清理环境"
docker stop "${CONTAINER_NAME}" 2>/dev/null || true
docker rm -f "${CONTAINER_NAME}" 2>/dev/null || true

# 强制清理任何同名容器
docker ps -a --filter "name=${CONTAINER_NAME}" --format "{{.ID}}" | xargs -r docker stop 2>/dev/null || true
docker ps -a --filter "name=${CONTAINER_NAME}" --format "{{.ID}}" | xargs -r docker rm -f 2>/dev/null || true

# 强制清理所有包含容器名的容器
docker ps -a --format "{{.Names}}" | grep "${CONTAINER_NAME}" | xargs -r docker stop 2>/dev/null || true
docker ps -a --format "{{.Names}}" | grep "${CONTAINER_NAME}" | xargs -r docker rm -f 2>/dev/null || true

# 强制清理网络
docker network ls --filter "name=${CONTAINER_NAME}" --format "{{.Name}}" | xargs -r docker network rm 2>/dev/null || true
sleep 3
log_success "环境清理完成"

# 步骤2: 检查端口占用
log "2. 检查端口占用"
if ss -tln | grep -q :${PORT}; then
    log_error "端口 ${PORT} 已被占用"
    ss -tln | grep :${PORT}
    exit 1
else
    log_success "端口 ${PORT} 可用"
fi
echo ""

# 步骤3: 下载JAR文件（优先使用本地文件）
log "3. 获取JAR文件"
rm -f "${JAR_PATH}"

# 检查本地文件是否存在
if [ -f "${LOCAL_JAR_PATH}" ]; then
    log "找到本地JAR文件: ${LOCAL_JAR_PATH}"
    cp "${LOCAL_JAR_PATH}" "${JAR_PATH}"
    log_success "使用本地JAR文件"
else
    log "本地文件不存在，从服务器下载"
    curl -s -L -o "${JAR_PATH}" "${JAR_URL}"

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
cp "${JAR_PATH}" "${BUILD_DIR}/${FILE_NAME}.jar"
cd "${BUILD_DIR}"

cat > Dockerfile << 'EOF'
${DOCKERFILE_CONTENT}
EOF

if [ "${USE_DOCKER_COMPOSE}" = "true" ]; then
    # Docker Compose 构建方式
    echo "# 生成 Docker Compose 配置文件"
    cat > "${DOCKER_COMPOSE_FILE}" << 'EOF'
${DOCKER_COMPOSE_CONTENT}
EOF

    log_success "Docker Compose 配置文件已生成"

    # 使用 docker-compose 构建
    echo "# 处理 .env 文件（支持动态变量）"
    ENV_SOURCE="/home/park/docker/.env"
    ENV_TARGET="${BUILD_DIR}/.env"
    ENV_FILE_EXISTS=false

    if [ -f "${ENV_SOURCE}" ]; then
        log "处理动态 .env 文件"

        # 创建临时文件
        TEMP_ENV=$(mktemp)

        # 方法1：使用 grep 直接提取有效变量（不通过 source）
        log "从源文件提取有效变量..."

        # 使用 grep 提取格式正确的变量（变量名只包含字母、数字和下划线）
        grep -E '^[a-zA-Z_][a-zA-Z0-9_]*=' "${ENV_SOURCE}" 2>/dev/null | \
        while IFS= read -r line; do
            # 去除行首行尾空白
            line=$(echo "$line" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')

            # 跳过空行和注释
            if [[ -z "$line" ]] || [[ "$line" =~ ^# ]]; then
                continue
            fi

            # 检查是否包含特殊字符
            if echo "$line" | grep -q '[{}()$`"'\'']'; then
                # 如果包含特殊字符，对值进行转义
                var_name=$(echo "$line" | cut -d'=' -f1)
                var_value=$(echo "$line" | cut -d'=' -f2- | sed 's/"/\\"/g' | sed 's/[{}()$`]//g')
                echo "${var_name}=${var_value}" >> "${TEMP_ENV}"
            else
                echo "$line" >> "${TEMP_ENV}"
            fi
        done

        # 添加系统变量（确保这些变量存在）
        {
            echo ""
            echo "# 系统变量"
            echo "UID=$(id -u)"
            echo "GID=$(id -g)"
            echo "DEPLOY_TIMESTAMP=$(date +%s)"

            # 如果 contextPath 不存在，添加默认值
            if ! grep -q '^contextPath=' "${TEMP_ENV}" 2>/dev/null; then
                echo "contextPath=/home/park"
            fi

            # 如果 os 不存在，添加默认值
            if ! grep -q '^os=' "${TEMP_ENV}" 2>/dev/null; then
                echo "os=centos"
            fi

            # 添加 profileActive（如果不存在）
            if ! grep -q '^profileActive=' "${TEMP_ENV}" 2>/dev/null; then
                echo "profileActive=${profileActive:-dev}"
            fi

            # 添加 logPath（如果不存在）
            if ! grep -q '^logPath=' "${TEMP_ENV}" 2>/dev/null; then
                echo "logPath=${logPath:-/var/log}"
            fi
        } >> "${TEMP_ENV}"

        # 去重并清理空行
        sort -u "${TEMP_ENV}" | sed '/^[[:space:]]*$/d' > "${ENV_TARGET}"
        rm -f "${TEMP_ENV}"

        # 验证生成的 .env 文件
        if [ -s "${ENV_TARGET}" ]; then
            ENV_FILE_EXISTS=true
            log_success "环境文件处理完成"
            echo "生成的环境变量数量: $(wc -l < "${ENV_TARGET}")"
            echo "示例变量:"
            head -5 "${ENV_TARGET}" | sed 's/^/  /'
            echo ""

            # 调试信息：检查是否有异常字符
            if grep -q '[^a-zA-Z0-9_=./@-]' "${ENV_TARGET}"; then
                log "警告: 发现可能的特殊字符，但已处理"
            fi
        else
            log_error "生成的 .env 文件为空"
            exit 1
        fi

    else
        # 创建动态 .env 文件
        log "源环境文件不存在，创建动态环境文件"
        cat > "${ENV_TARGET}" << EOF
# 自动生成的环境文件 - $(date '+%Y-%m-%d %H:%M:%S')
UID=$(id -u)
GID=$(id -g)
DEPLOY_TIMESTAMP=$(date +%s)
contextPath=/home/park
os=centos
profileActive=${profileActive:-dev}
logPath=${logPath:-/var/log}
EOF

        if [ -s "${ENV_TARGET}" ]; then
            ENV_FILE_EXISTS=true
            log_success "已创建动态环境文件"
            echo "生成的环境变量数量: $(wc -l < "${ENV_TARGET}")"
        else
            log_error "创建环境文件失败"
            exit 1
        fi
    fi

    # 显示 .env 文件内容预览
    echo "--- .env 文件预览 (前10行) ---"
    head -10 "${ENV_TARGET}" | sed 's/^/  /'
    echo "------------------------------"

    # 构建命令
    if [ "$ENV_FILE_EXISTS" = true ]; then
        DOCKER_COMPOSE_CMD="docker-compose --env-file .env -f docker-compose.yml -p $CONTAINER_NAME"
    else
        DOCKER_COMPOSE_CMD="docker-compose -f docker-compose.yml -p $CONTAINER_NAME"
    fi

    # 使用 docker-compose 构建镜像
    log "开始构建 Docker 镜像..."
    if $DOCKER_COMPOSE_CMD build; then
        log_success "镜像构建成功 (通过 Docker Compose)"
    else
        log_error "镜像构建失败"
        echo "最后20行构建日志:"
        $DOCKER_COMPOSE_CMD build --no-cache 2>&1 | tail -20 || true
        exit 1
    fi
else
    # 普通的 Docker 构建
    if docker build -t "${IMAGE_NAME}" .; then
        log_success "镜像构建成功"
    else
        log_error "镜像构建失败"
        exit 1
    fi
fi

# 步骤5: 运行容器（Docker Compose 或 Docker）
if [ "${USE_DOCKER_COMPOSE}" = "true" ]; then
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
        CONTAINER_ID=$(docker ps --filter "name=${CONTAINER_NAME}" --format "{{.ID}}" | head -1)

        if [ -n "$CONTAINER_ID" ]; then
            log_success "容器ID: ${CONTAINER_ID:0:12}"
        else
            # 尝试查找任何包含容器名的容器
            CONTAINER_ID=$(docker ps --filter "name=^/${CONTAINER_NAME}$" --format "{{.ID}}" | head -1)
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
      --name "${CONTAINER_NAME}" \
      --restart=always \
      -p ${PORT}:${EXPOSE_PORT} \
      -e TZ=Asia/Shanghai \
      -e JAVA_OPTS="-Xms256m -Xmx512m -Duser.timezone=Asia/Shanghai" \
      --log-opt max-size=10m \
      --log-opt max-file=3 \
      "${IMAGE_NAME}")

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
    if ! docker ps --filter "name=${CONTAINER_NAME}" | grep -q "${CONTAINER_NAME}"; then
        log_error "容器已停止运行"
        if [ "${USE_DOCKER_COMPOSE}" = "true" ]; then
            # 尝试获取Docker Compose日志
            $DOCKER_COMPOSE_CMD logs 2>/dev/null || true
        fi
        break
    fi

    # 获取容器日志
    CONTAINER_LOGS=$(docker logs "${CONTAINER_NAME}" 2>&1 || true)

    # 检查Spring Boot启动关键词
    if echo "$CONTAINER_LOGS" | grep -q "Tomcat started on port.*${EXPOSE_PORT}\|Started .*Application in"; then
        log_success "检测到Spring Boot启动成功"
        SUCCESS=true
        break
    fi

    if [ "${USE_DOCKER_COMPOSE}" = "true" ]; then
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

    if [ "${USE_DOCKER_COMPOSE}" = "true" ]; then
        # Docker Compose 信息
        echo "🔹 部署方式: Docker Compose"
        echo "🔹 容器名称: ${CONTAINER_NAME}"
        echo "🔹 容器ID:   ${CONTAINER_ID:0:12}"
        if [ "$ENV_FILE_EXISTS" = true ]; then
            echo "🔹 环境文件: 已使用（包含动态变量）"
            echo "🔹 动态UID:  $(grep '^UID=' "${ENV_TARGET}" 2>/dev/null | cut -d= -f2 || echo '未设置')"
        else
            echo "🔹 环境文件: 未使用"
        fi
        echo ""
        echo "📊 容器状态:"
        docker ps --filter "name=${CONTAINER_NAME}" --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"
        echo ""
        echo "📊 服务列表:"
        $DOCKER_COMPOSE_CMD ps
    else
        # Docker 信息
        CONTAINER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "${CONTAINER_NAME}" 2>/dev/null || echo "unknown")
        echo "🔹 部署方式: Docker"
        echo "🔹 容器名称: ${CONTAINER_NAME}"
        echo "🔹 容器ID:   ${CONTAINER_ID:0:12}"
        echo "🔹 容器IP:   $CONTAINER_IP"
        echo "🔹 映射端口: ${PORT} -> ${EXPOSE_PORT}"
        echo ""
        echo "📊 容器状态:"
        docker ps --filter "name=${CONTAINER_NAME}" --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"
    fi

    echo "🔹 镜像版本: ${IMAGE_NAME}"
    echo "🔹 JAR文件:  ${JAR_NAME}"
    echo "🔹 启动时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""

    echo "📝 应用启动日志:"
    docker logs "${CONTAINER_NAME}" 2>&1 | grep -E "Starting|Tomcat started|Started .*Application" | tail -5
    echo ""
    echo "✅ DEPLOY_SUCCESS"
else
    log_error "❌ 部署失败或超时"
    echo ""
    echo "🔍 错误诊断:"

    echo "容器状态:"
    docker ps --filter "name=${CONTAINER_NAME}" --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}"
    echo ""
    echo "容器日志（最后20行）:"
    docker logs "${CONTAINER_NAME}" 2>&1 | tail -20

    if [ "${USE_DOCKER_COMPOSE}" = "true" ]; then
        echo ""
        echo "Docker Compose 日志:"
        $DOCKER_COMPOSE_CMD logs 2>/dev/null || true

        echo ""
        echo "Docker Compose 配置:"
        cat "${DOCKER_COMPOSE_FILE}" 2>/dev/null || true

        echo ""
        echo ".env 文件内容:"
        cat "${ENV_TARGET}" 2>/dev/null || true
    fi

    # 检查端口是否被其他进程占用（只在 Docker 模式下）
    if [ "${USE_DOCKER_COMPOSE}" = "false" ]; then
        echo ""
        echo "端口占用情况:"
        ss -tlnp | grep ":${PORT}" || echo "端口 ${PORT} 未被其他进程占用"
    fi

    # 清理失败的容器
    if [ "${USE_DOCKER_COMPOSE}" = "true" ]; then
        $DOCKER_COMPOSE_CMD down 2>/dev/null || true
    fi
    docker rm -f "${CONTAINER_NAME}" 2>/dev/null || true

    exit 1
fi

# 清理临时文件
log "清理临时文件..."
if [ -d "${BUILD_DIR}" ]; then
    echo "删除构建目录: ${BUILD_DIR}"
    rm -rf "${BUILD_DIR}" 2>/dev/null || true
    if [ $? -eq 0 ]; then
        log_success "构建目录已删除"
    else
        log "警告: 构建目录删除失败，但可以忽略"
    fi
else
    log "构建目录不存在，无需清理"
fi

# 清理临时JAR文件
rm -f /tmp/*.jar 2>/dev/null || true
log_success "所有临时文件已清理"