#!/bin/bash

# JAR包重新部署脚本
# 文件: ${JAR_NAME}
# 容器: ${CONTAINER_NAME}

set -e
set -o pipefail

# 定义函数
log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

log_warn() {
    echo "[WARN] $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

# 检查容器是否可以执行命令
is_container_executable() {
    docker exec "$CONTAINER_NAME" echo "ready" >/dev/null 2>&1
    return $?
}

# 确保容器工作目录存在
ensure_workdir_exists() {
    log_info "确保工作目录存在: $CONTAINER_WORKDIR"

    # 如果容器可以执行命令，直接创建目录
    if is_container_executable; then
        docker exec "$CONTAINER_NAME" mkdir -p "$CONTAINER_WORKDIR" 2>/dev/null || {
            log_error "无法创建目录（容器可执行）"
            return 1
        }
        log_info "✓ 目录创建成功"
        return 0
    else
        # 容器不可执行，需要启动容器后创建
        log_info "容器不可执行，尝试启动容器后创建目录..."

        # 保存当前重启策略
        local current_restart
        current_restart=$(docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' "$CONTAINER_NAME" 2>/dev/null || echo "no")

        # 临时禁用重启
        docker update --restart=no "$CONTAINER_NAME" >/dev/null 2>&1 || true

        # 启动容器
        if docker start "$CONTAINER_NAME" >/dev/null 2>&1; then
            sleep 3

            # 等待容器可执行
            local wait_time=10
            for i in $(seq 1 $wait_time); do
                if is_container_executable; then
                    # 创建目录
                    if docker exec "$CONTAINER_NAME" mkdir -p "$CONTAINER_WORKDIR" 2>/dev/null; then
                        log_info "✓ 容器启动后目录创建成功"
                        # 停止容器
                        docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
                        sleep 2
                        # 恢复重启策略
                        docker update --restart="$current_restart" "$CONTAINER_NAME" >/dev/null 2>&1 || true
                        return 0
                    else
                        log_error "容器启动后仍无法创建目录"
                        docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
                        docker update --restart="$current_restart" "$CONTAINER_NAME" >/dev/null 2>&1 || true
                        return 1
                    fi
                fi
                sleep 1
            done

            log_error "容器启动但无法执行命令"
            docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
            docker update --restart="$current_restart" "$CONTAINER_NAME" >/dev/null 2>&1 || true
            return 1
        else
            log_error "无法启动容器"
            docker update --restart="$current_restart" "$CONTAINER_NAME" >/dev/null 2>&1 || true
            return 1
        fi
    fi
}

# 复制JAR文件到容器
copy_jar_to_container() {
    log_info "复制JAR文件到容器..."

    # 方法1：直接使用docker cp
    log_info "方法1: 使用docker cp"
    if docker cp "$JAR_PATH" "${CONTAINER_NAME}:${CONTAINER_JAR_PATH}"; then
        log_info "✓ docker cp 成功"
        return 0
    fi

    log_warn "方法1失败，尝试方法2..."

    # 方法2：启动容器后复制
    log_info "方法2: 启动容器后复制"

    # 保存当前重启策略
    local current_restart
    current_restart=$(docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' "$CONTAINER_NAME" 2>/dev/null || echo "no")

    # 临时禁用重启
    docker update --restart=no "$CONTAINER_NAME" >/dev/null 2>&1 || true

    # 启动容器
    if docker start "$CONTAINER_NAME" >/dev/null 2>&1; then
        sleep 3

        # 等待容器可执行
        local wait_time=10
        for i in $(seq 1 $wait_time); do
            if is_container_executable; then
                log_info "容器已启动且可执行命令"

                # 使用docker cp复制
                if docker cp "$JAR_PATH" "${CONTAINER_NAME}:${CONTAINER_JAR_PATH}"; then
                    log_info "✓ 启动后复制成功"

                    # 验证文件
                    if docker exec "$CONTAINER_NAME" [ -f "$CONTAINER_JAR_PATH" ] 2>/dev/null; then
                        log_info "✓ 文件验证成功"

                        # 设置权限
                        docker exec "$CONTAINER_NAME" chmod +x "$CONTAINER_JAR_PATH" 2>/dev/null || log_warn "设置权限失败"

                        # 停止容器
                        docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
                        sleep 2

                        # 恢复重启策略
                        docker update --restart="$current_restart" "$CONTAINER_NAME" >/dev/null 2>&1 || true
                        return 0
                    else
                        log_error "文件复制但验证失败"
                    fi
                else
                    log_error "启动后复制失败"
                fi

                # 停止容器并恢复
                docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
                docker update --restart="$current_restart" "$CONTAINER_NAME" >/dev/null 2>&1 || true
                return 1
            fi
            sleep 1
        done

        log_error "容器启动但无法执行命令"
        docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
        docker update --restart="$current_restart" "$CONTAINER_NAME" >/dev/null 2>&1 || true
        return 1
    else
        log_error "无法启动容器"
        docker update --restart="$current_restart" "$CONTAINER_NAME" >/dev/null 2>&1 || true
        return 1
    fi
}

# 启动容器并验证
start_and_verify_container() {
    log_info "启动容器并验证..."

    # 启动容器
    if ! docker start "$CONTAINER_NAME"; then
        log_error "启动容器失败"
        return 1
    fi

    log_info "等待容器启动..."

    # 等待容器状态变为running
    local timeout=30
    for i in $(seq 1 $timeout); do
        sleep 1

        local status
        status=$(docker inspect -f '{{.State.Status}}' "$CONTAINER_NAME" 2>/dev/null || echo "unknown")

        case "$status" in
            "running")
                log_info "容器状态: running"

                # 等待容器可执行命令
                if is_container_executable; then
                    log_info "✓ 容器可执行命令"

                    # 检查Java进程
                    local java_process
                    java_process=$(docker exec "$CONTAINER_NAME" ps aux 2>/dev/null | grep -E "java.*jar" | head -1 || echo "")

                    if [ -n "$java_process" ]; then
                        log_info "✓ Java进程运行中:"
                        echo "    $java_process"
                        return 0
                    else
                        # 检查容器日志是否有启动成功信息
                        local log_output
                        log_output=$(docker logs "$CONTAINER_NAME" --tail=20 2>/dev/null || echo "")

                        if echo "$log_output" | grep -q -i "started\|running\|ready\|success"; then
                            log_info "✓ 应用日志显示启动成功"
                            return 0
                        else
                            log_warn "未找到Java进程，检查日志..."
                            docker logs "$CONTAINER_NAME" --tail=30
                            # 容器在运行但没有Java进程，可能应用启动失败
                            return 1
                        fi
                    fi
                else
                    log_warn "容器运行但无法执行命令，继续等待..."
                fi
                ;;

            "restarting")
                log_error "容器进入重启循环"
                docker logs "$CONTAINER_NAME" --tail=20 2>/dev/null || echo "无法获取日志"
                docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
                return 1
                ;;

            "exited")
                log_error "容器已退出"
                local exit_code
                exit_code=$(docker inspect -f '{{.State.ExitCode}}' "$CONTAINER_NAME" 2>/dev/null || echo "unknown")
                log_error "退出码: $exit_code"
                docker logs "$CONTAINER_NAME" --tail=30 2>/dev/null || echo "无法获取日志"
                return 1
                ;;

            *)
                if [ $i -eq $timeout ]; then
                    log_error "容器启动超时，状态: $status"
                    docker logs "$CONTAINER_NAME" --tail=30 2>/dev/null || echo "无法获取日志"
                    return 1
                fi
                ;;
        esac
    done

    log_error "启动验证超时"
    return 1
}

cleanup() {
    log_info "清理临时文件..."
    rm -f /tmp/*.jar 2>/dev/null || true
}

# 设置trap，确保脚本退出时清理
trap cleanup EXIT

# 定义变量
JAR_URL="${JAR_URL}"
JAR_NAME="${JAR_NAME}"
LOCAL_JAR_PATH="${LOCAL_JAR_PATH}"
JAR_PATH="/tmp/$JAR_NAME"
CONTAINER_NAME="${CONTAINER_NAME}"

# 检查容器是否存在
log_info "检查容器是否存在..."
if ! docker ps -a --filter "name=^${CONTAINER_NAME}$" --format '{{.Names}}' | grep -q "${CONTAINER_NAME}"; then
    log_error "容器 ${CONTAINER_NAME} 不存在"
    exit 1
fi

# 获取容器当前配置
CONTAINER_WORKDIR=$(docker inspect -f '{{.Config.WorkingDir}}' "$CONTAINER_NAME" 2>/dev/null || echo "${DEFAULT_WORKDIR}")
CONTAINER_JAR_PATH="${CONTAINER_WORKDIR}/${FILE_NAME}.jar"
if [ -z "$CONTAINER_WORKDIR" ]; then
    CONTAINER_WORKDIR="${DEFAULT_WORKDIR}"  # 默认工作目录
fi
log_info "容器工作目录: $CONTAINER_WORKDIR"
log_info "容器内JAR路径: $CONTAINER_JAR_PATH"

# 获取当前容器状态
CONTAINER_STATUS=$(docker inspect -f '{{.State.Status}}' "$CONTAINER_NAME" 2>/dev/null || echo "unknown")
log_info "容器状态: $CONTAINER_STATUS"

# 确保容器已停止
if [ "$CONTAINER_STATUS" = "running" ] || [ "$CONTAINER_STATUS" = "restarting" ]; then
    log_info "停止容器..."
    docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || docker kill "$CONTAINER_NAME" >/dev/null 2>&1 || true
    sleep 3
fi

# 获取JAR文件
log_info "获取JAR文件: $JAR_NAME"
rm -f "$JAR_PATH" 2>/dev/null || true

if [ -f "$LOCAL_JAR_PATH" ]; then
    log_info "找到本地JAR文件，使用本地文件"
    cp "$LOCAL_JAR_PATH" "$JAR_PATH"
else
    log_info "本地文件不存在，从服务器下载"
    if ! curl -L -o "$JAR_PATH" "$JAR_URL"; then
        log_error "JAR文件下载失败"
        exit 1
    fi
fi

# 验证JAR文件
log_info "验证JAR文件..."
if [ ! -f "$JAR_PATH" ]; then
    log_error "JAR文件不存在"
    exit 1
fi

FILE_SIZE=$(wc -c < "$JAR_PATH" 2>/dev/null | tr -d ' ')
if [ "$FILE_SIZE" -eq 0 ]; then
    log_error "JAR文件为空"
    exit 1
fi
log_info "JAR文件大小: ${FILE_SIZE} bytes"

# 确保工作目录存在
if ! ensure_workdir_exists; then
    log_error "无法确保工作目录存在"
    exit 1
fi

# 复制JAR文件
if ! copy_jar_to_container; then
    log_error "复制JAR文件失败"
    exit 1
fi

# 验证文件已成功复制
log_info "验证文件已成功复制..."
if ! docker exec "$CONTAINER_NAME" [ -f "$CONTAINER_JAR_PATH" ] 2>/dev/null; then
    # 如果容器不可执行，尝试启动容器验证
    log_info "容器不可执行，启动容器验证文件..."

    docker start "$CONTAINER_NAME" >/dev/null 2>&1
    sleep 3

    if docker exec "$CONTAINER_NAME" [ -f "$CONTAINER_JAR_PATH" ] 2>/dev/null; then
        log_info "✓ 文件验证成功"
        docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
        sleep 2
    else
        log_error "✗ 文件验证失败，JAR文件未成功复制"
        docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
        exit 1
    fi
else
    log_info "✓ 文件验证成功"
fi

# 启动容器
log_info "启动容器..."
if ! start_and_verify_container; then
    log_error "容器启动失败"
    exit 1
fi

# 输出成功信息
log_info "重新部署成功！"
echo "=== 部署成功信息 ==="
echo "容器名称: $CONTAINER_NAME"
CONTAINER_ID=$(docker inspect -f '{{.Id}}' "$CONTAINER_NAME" 2>/dev/null || echo "unknown")
echo "容器ID: $CONTAINER_ID"
CONTAINER_STATUS=$(docker inspect -f '{{.State.Status}}' "$CONTAINER_NAME" 2>/dev/null || echo "unknown")
echo "容器状态: $CONTAINER_STATUS"
CONTAINER_IMAGE=$(docker inspect -f '{{.Config.Image}}' "$CONTAINER_NAME" 2>/dev/null || echo "unknown")
echo "容器镜像: $CONTAINER_IMAGE"
if [ "$CONTAINER_STATUS" = "running" ]; then
    if is_container_executable; then
        JAVA_PROCESS=$(docker exec "$CONTAINER_NAME" ps aux 2>/dev/null | grep -E "java.*jar" | head -1 | awk '{print $11, $12}' || echo "未找到")
        echo "Java进程: $JAVA_PROCESS"
        echo "启动时间: $(docker inspect -f '{{.State.StartedAt}}' "$CONTAINER_NAME" 2>/dev/null | cut -d'.' -f1 || echo "未知")"
    else
        echo "Java进程: 容器运行但无法执行命令"
    fi
else
    echo "Java进程: 容器未运行"
fi
echo "JAR版本: ${VERSION}"
echo "部署时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "DEPLOY_SUCCESS"