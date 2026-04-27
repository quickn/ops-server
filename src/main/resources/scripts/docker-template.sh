#!/bin/bash

# ======================================================
# Spring Boot部署脚本
# 支持 Docker 和 Docker Compose 两种方式
# ======================================================

set -e
set -o pipefail

# 基本变量
IMAGE_NAME="${IMAGE_NAME}"
CONTAINER_NAME="${CONTAINER_NAME}"
PORT="${PORT}"
BUILD_DIR="/tmp/docker-${CONTAINER_NAME}"

if [ -d "${BUILD_DIR}" ]; then
  rm -rf "${BUILD_DIR}"
fi

# Docker Compose 相关变量
USE_DOCKER_COMPOSE="${USE_DOCKER_COMPOSE}"
DOCKER_COMPOSE_FILE="$BUILD_DIR/docker-compose.yml"

# 日志函数
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"; }
log_success() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✓ $1"; }
log_error() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✗ $1" >&2; }


get_container_uptime_seconds() {
    local container_name=$1
    local status=$(docker ps --filter "name=${container_name}" --format "{{.Status}}" 2>/dev/null)
    if [ -z "$status" ]; then
        echo "0"
        return
    fi
    # 解析 status 字符串，格式示例：
    # "Up 2 minutes"    -> 120秒
    # "Up 3 hours"      -> 10800秒
    # "Up 5 days"       -> 432000秒
    # "Up 45 seconds"   -> 45秒
    # "Up About a minute" -> 60秒（近似）
    if [[ "$status" =~ ^Up[[:space:]]+([0-9]+)[[:space:]]+seconds? ]]; then
        # Up 45 seconds
        echo "${BASH_REMATCH[1]}"
    elif [[ "$status" =~ ^Up[[:space:]]+([0-9]+)[[:space:]]+minutes? ]]; then
        # Up 2 minutes
        echo "$((${BASH_REMATCH[1]} * 60))"
    elif [[ "$status" =~ ^Up[[:space:]]+([0-9]+)[[:space:]]+hours? ]]; then
        # Up 3 hours
        echo "$((${BASH_REMATCH[1]} * 3600))"
    elif [[ "$status" =~ ^Up[[:space:]]+([0-9]+)[[:space:]]+days? ]]; then
        # Up 5 days
        echo "$((${BASH_REMATCH[1]} * 86400))"
    elif [[ "$status" =~ ^Up[[:space:]]+([0-9]+)[[:space:]]+weeks? ]]; then
        # Up 2 weeks
        echo "$((${BASH_REMATCH[1]} * 604800))"
    elif echo "$status" | grep -qi "about.*minute"; then
        # Up About a minute
        echo "60"
    else
        # 无法解析，返回0
        echo "0"
    fi
}

# 等待容器启动完成完成
wait_for_container_startup(){
  local max_wait_time=90        # 最大等待时间（秒）
  local check_interval=2        # 检查间隔（秒）
  local elapsed_time=0
  while [ $elapsed_time -lt $max_wait_time ]; do
        sleep $check_interval
        elapsed_time=$((elapsed_time + check_interval))
      # 检查容器是否还在运行
      if ! docker ps --filter "name=${CONTAINER_NAME}" | grep -q "${CONTAINER_NAME}"; then
          log_error "容器已停止运行"
          if [ "${USE_DOCKER_COMPOSE}" = "true" ]; then
              # 尝试获取Docker Compose日志
              $DOCKER_COMPOSE_CMD logs 2>/dev/null || true
          fi
          return 1
      fi
       # 获取启动时长（秒）
      UPTIME_SECONDS=$(get_container_uptime_seconds "$CONTAINER_NAME")
      echo "容器已运行: ${UPTIME_SECONDS} 秒"
      if [ "$UPTIME_SECONDS" -ge 15 ]; then
           return 0
      fi
      echo "✗ 容器运行不足15秒，还需等待"
      if [ "${USE_DOCKER_COMPOSE}" = "true" ]; then
          echo "  [$elapsed_time] 等待服务启动 (Docker Compose)..."
      else
          echo "  [$elapsed_time] 等待服务启动..."
      fi
  done
  return 0
}

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
#docker network ls --filter "name=${CONTAINER_NAME}" --format "{{.Name}}" | xargs -r docker network rm 2>/dev/null || true
docker network ls --filter name="${CONTAINER_NAME}" -q | xargs docker network rm 2>/dev/null || true

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


# 步骤4: 构建Docker镜像
log "4. 构建Docker镜像"

mkdir -p "${BUILD_DIR}"
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

    # 添加调试信息：打印源文件路径和内容
    log "========== .env 文件调试信息 =========="
    log "源文件路径: ${ENV_SOURCE}"

    if [ -f "${ENV_SOURCE}" ]; then
        log "✓ 找到 .env 源文件"
        log "文件权限: $(ls -l ${ENV_SOURCE})"
        log "文件大小: $(wc -c < ${ENV_SOURCE}) bytes"
        log "文件行数: $(wc -l < ${ENV_SOURCE})"
        log ""
        log "源文件内容（过滤注释和空行）:"
        grep -v '^#' "${ENV_SOURCE}" | grep -v '^$' | while IFS= read -r line; do
            log "  $line"
        done
        log ""
        # 处理动态 .env 文件（修复：合并到同一个if块中）
        echo "清空目标文件"
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
    if [ "$ENV_FILE_EXISTS" = true ]; then
        DOCKER_COMPOSE_CMD="docker-compose --env-file .env -f docker-compose.yml -p $CONTAINER_NAME"
    else
        DOCKER_COMPOSE_CMD="docker-compose -f docker-compose.yml -p $CONTAINER_NAME"
    fi

    # 使用 docker-compose 构建镜像
    if $DOCKER_COMPOSE_CMD build; then
        log_success "镜像构建成功 (通过 Docker Compose)"
    else
        log_error "镜像构建失败"
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

    log "6. 等待Docker启动"
else
    # 使用 Docker 直接运行
    log "5. 运行容器"

    CONTAINER_ID=$(docker run -d \
      --name "${CONTAINER_NAME}" \
      --restart=always \
      -p ${PORT}:${EXPOSE_PORT} \
      -e TZ=Asia/Shanghai \
      --log-opt max-size=10m \
      --log-opt max-file=3 \
      "${IMAGE_NAME}")

    if [ $? -eq 0 ] && [ -n "$CONTAINER_ID" ]; then
        log_success "容器启动成功，ID: ${CONTAINER_ID:0:12}"
    else
        log_error "容器启动失败"
        exit 1
    fi

    log "6. 等待Docker启动"
fi

log "7. 部署结果"
if wait_for_container_startup; then
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