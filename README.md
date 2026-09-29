# ops-server

基于 **Spring Boot 3.5 + Java 21** 构建的 Linux 运维监控系统，提供服务器性能监控、告警预警、Docker 容器监控、远程命令执行、文件管理、项目部署、API 健康监测等能力，并内置 MCP Server 支持 AI 助手直接调用运维能力。

## 功能特性

### 服务器监控

- **性能采集**：CPU、内存、磁盘分区、网络 IO、系统负载、进程级资源与网络流量统计
- **主机管理**：Agent 纳管、在线状态检测（超时自动标记离线并告警）、系统信息与磁盘使用查询
- **Docker 监控**：容器清单自动同步、容器 CPU / 内存统计、远程重启 / 停止容器
- **实时日志**：日志文件浏览、在线日志分析、预警日志查询

### 告警预警

- **预警规则**：支持阈值 / 关键字触发、多级别、静默期、连续触发阈值、聚合窗口降噪
- **通知渠道**：邮件（可在线配置邮件服务器并测试发送）、短信
- **告警对象**：CPU / 内存 / 磁盘超阈值、Docker 容器异常、主机离线、应用下线、接口超时、定时任务异常
- **联系人管理**：报警联系人、联系人组、告警发送记录

### 运维操作

- **命令执行**：常用指令库管理、批量下发指令到 Agent / 主机 / 容器 / 服务、指令执行日志审计
- **文件管理**：远程文件浏览 / 创建 / 删除 / 在线编辑、文件上传与 rsync 批量分发同步
- **项目部署**：JAR 包版本管理、多机部署 / 重新部署 / 同步 / 备份恢复、部署记录查询
- **Nginx 管理**：配置语法测试、重载、启动、停止
- **定时任务**：基于 XXL-Job 的指令任务、JAR 同步、定时并行部署

### 平台能力

- **API 健康监测**：HTTP 接口可用性检测与超时预警
- **MCP Server**：内置 Streamable-HTTP 模式 MCP 服务（端点 `/mcp`），提供登录、Agent 列表、服务列表、项目列表 / 部署 / 同步 / 备份等 7 个工具，可接入 AI 助手实现对话式运维
- **系统管理**：用户 / 角色 / 菜单 / 部门 / 字典（RBAC 权限模型、接口与按钮级权限控制、数据权限行级过滤）
- **安全认证**：Spring Security 6 + JWT（Access Token + Refresh Token）、验证码、登录密码 RSA 加密传输

## 系统架构

```
被监控机器（Agent 采集端）──RabbitMQ──> ops-receiver :9999 ──入库──> MySQL 8.x
                                                                │
ops-admin :8989（Web API / MCP Server）<──查询──────┘
```

- Agent 通过 RabbitMQ 上报监控数据（CPU、内存、磁盘、网络、进程、Docker 等）
- `ops-receiver` 消费消息并落库，同时负责超阈值告警判定、主机离线检测、历史数据定期清理
- `ops-admin` 提供查询与管理接口，供前端（独立工程）调用

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.5.13 |
| JDK | Java | 21 |
| 安全框架 | Spring Security 6 + JWT | - |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.x |
| 缓存 | Redis + Redisson | 3.21.0 |
| 消息队列 | RabbitMQ (spring-boot-starter-amqp) | - |
| 定时任务 | XXL-Job | 2.5.0 |
| API 文档 | Knife4j (springdoc-openapi) | 4.3.0 / 2.8.9 |
| 对象映射 | MapStruct | 1.5.3.Final |
| AI 工具协议 | Spring AI MCP (Streamable-HTTP) | - |
| 对象存储 | MinIO / 阿里云 OSS | 8.5.2 / 3.16.3 |
| 序列化 | FastJSON2 | 2.0.53 |
| 工具库 | Hutool | 5.8.15 |

## 模块结构

```
ops-server/
├── ops-common/          # 公共基础模块（实体、Mapper、动态 SQL 引擎、工具、通用配置）
├── ops-admin/           # 主服务模块（系统管理、监控面板、告警、Docker、MCP Server）- 端口 8989
├── ops-receiver/        # 监控数据接收服务（消费 RabbitMQ 上报数据并入库）- 端口 9999
├── db/                  # 数据库初始化脚本
├── docker/              # Docker Compose 部署配置
└── Dockerfile           # 容器构建文件
```

```
ops-admin  ──>  ops-common  <──  ops-receiver
```

两个业务模块相互独立，仅共享 `ops-common`。`ops-receiver` 可独立部署，也可被 `ops-admin` 以 Maven 依赖方式引入后内嵌启动。

## 快速开始

### 1. 环境准备

- JDK 21
- Maven 3.8+
- MySQL 8.x
- Redis 6+
- RabbitMQ 3.x
- IDEA 插件：Lombok、MapStruct Support

### 2. 初始化数据库

在 MySQL 中依次执行 `db/` 目录下的脚本：

| 脚本                      | 说明     |
|-------------------------|--------|
| [schema.sql](db/schema.sql)   | 数据库表结构 |
| [init.sql](db/init.sql) | 初始化数据  |

### 3. 修改配置

修改 `ops-admin/src/main/resources/application-dev.yml` 中的 MySQL、Redis、RabbitMQ 连接配置；如需独立运行 receiver，同步修改 `ops-receiver/src/main/resources/application-dev.yml`。

也可使用根目录 `conf-dev.properties` 作为外部配置覆盖。

### 4. 启动服务

```bash
# 编译打包（默认跳过 ProGuard 混淆）
mvn clean package -DskipTests
```

- 启动 `ops-admin`：运行 `OpsAdminApplication#main()`（端口 8989）
- 启动 `ops-receiver`：运行 `OpsReceiverApplication#main()`（端口 9999，如独立部署）

### 5. 验证

访问接口文档验证服务是否正常：

- Knife4j：<http://localhost:8989/doc.html>
- Swagger：<http://localhost:8989/swagger-ui/index.html>

### 6. 接入 Agent

在「Agent 管理」中添加被监控机器，Agent 客户端通过 RabbitMQ 上报数据（或走 HTTP `/receiver/**` 接口），即可在监控面板查看数据。

## 部署

### Docker Compose

```bash
cd docker && docker-compose up -d
```

详见 [docker/ops-server.yml](docker/ops-server.yml)（包含 `ops-admin` 与 `ops-receiver` 两个服务，通过 `.env` 注入数据库、Redis、RabbitMQ 地址等环境变量）。

### Kubernetes

参见 [deploy/](deploy/) 目录下的 Deployment 与 Ingress 配置。

### 生产构建说明

生产构建默认启用 ProGuard 代码混淆，开发阶段可使用 `-Dproguard.skip=true` 跳过。

## 开发规范

### 实体命名

| 名称 | 用途 | 示例 |
|------|------|------|
| entity | 映射数据库表 | `SysUser`, `Agent`, `CpuState` |
| bo | 多表关联查询的业务实体 | `UserBO`, `ChartBO` |
| query | 查询参数对象（参数≥3时使用） | `UserQuery`, `AgentQuery` |
| form | 表单提交对象 | `UserForm`, `AlertRuleForm` |
| vo | 视图返回对象 | `UserVO`, `AgentVo` |
| dto | RPC/模块间传输 | `UserDTO`, `MonitorMsgDto` |

### 方法命名

| 作用 | Controller | Service | Mapper |
|------|-----------|---------|--------|
| 分页查询 | `getUserPage` | `getUserPage` | `selectUserPage` |
| 列表查询 | `listUsers` | `listUsers` | `selectUserList` |
| 单个查询 | `getUser` | `getUser` | `selectById` |
| 新增 | `saveUser` | `saveUser` | `insert` |
| 修改 | `updateUser` | `updateUser` | `updateById` |
| 删除 | `deleteUser` | `deleteUser` | `deleteById` |

### API 路径规范

RESTful 风格，资源名使用复数名词：

| 操作 | 方法 | 路径 |
|------|------|------|
| 分页查询 | GET | `/api/v1/users` |
| 详情查询 | GET | `/api/v1/users/{id}` |
| 新增 | POST | `/api/v1/users` |
| 修改 | PUT | `/api/v1/users/{id}` |
| 删除 | DELETE | `/api/v1/users/{id}` |
| 部分更新 | PATCH | `/api/v1/users/{id}/status` |

### 动态 SQL 注解

`ops-common` 封装了注解驱动的动态 SQL 查询引擎，查询对象通过注解声明查询条件，Mapper 继承 `BaseQueryMapper` 即可自动生成查询 SQL，无需手写 XML：

```java
public class AgentQuery {
    @SelectFrom("ops_agent")
    @SelectColumn("id, agent_name, ip, os_name")
    @Where("agent_name LIKE '%${agentName}%'")
    private String agentName;

    @OrderBy("create_time DESC")
    private String orderBy;
}
```

## 常见问题

1. **JDK 版本**：项目要求 JDK 21，请勿使用低版本编译运行
2. **snakeyaml 版本**：切勿将 snakeyaml 覆盖到 1.x，Spring Boot 3.5 依赖 snakeyaml 2.x
3. **springdoc 版本**：knife4j 4.3.0 自带的 springdoc-openapi 2.2.0 与 Spring Framework 6.2 不兼容，父 POM 已仲裁到 2.8.9，请勿降级
4. **文件编码**：所有文件使用 UTF-8（不带 BOM）
5. **ops-receiver 部署形态**：可独立部署，也可由 `ops-admin` 引入依赖后内嵌启动，二选一即可

## 参与贡献

欢迎提交 Issue 与 Pull Request。提交信息遵循 [Angular 社区规范](https://github.com/conventional-changelog/conventional-changelog/tree/master/packages/conventional-changelog-angular)：

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 Bug |
| `style` | 代码风格（不影响运行） |
| `perf` | 性能优化 |
| `refactor` | 重构 |
| `revert` | 回滚 |
| `test` | 测试 |
| `docs` | 文档 |
| `chore` | 构建/依赖/配置变更 |

## 开源协议

本项目基于 [Apache License 2.0](LICENSE) 开源。


## 致谢

本项目后端工程系统管理基于开源模板 [youlai-boot](https://gitee.com/youlaiorg/youlai-boot)（有来开源组织）构建，感谢原作者及开源社区的贡献。
