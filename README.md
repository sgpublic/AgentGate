# AgentGate 2.0

AgentGate 是一个运行在 Linux x64 上的 Kotlin/Native 认证反向代理。它在根路径提供登录页面，并在认证成功后透明代理目标服务。前端仍使用 React/TypeScript 构建，生产资源通过 EmbedRaw 直接嵌入原生可执行文件。

## 路由行为

- `GET /`：未认证时返回登录页面；认证后代理到目标服务根路径。
- `POST /api/login`、`GET /api/info`、`POST /api/logout`：未认证时由网关处理登录、登录页元信息及登出；认证后代理至目标服务的同一路径。
- 其他路径：已认证时代理到目标服务；未认证时重定向至 `/`。
- 前端静态资源与目标服务的本地 Logo 路径无需认证，以便登录页面正常显示。

## 配置

环境变量与同名命令行参数等价，命令行参数优先。

| 环境变量 | 命令行参数 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `AGENT_GATE_PORT` | `--port` | `1180` | 监听端口 |
| `AGENT_GATE_TARGET_URL` | `--target-url` | 必填 | 目标服务 URL，必须包含协议且不以 `/` 结尾 |
| `AGENT_GATE_TARGET_LOGO` | `--target-logo` | 自动探测或 `/favicon.ico` | 登录页 Logo；以 `/` 开头时从目标服务代理 |
| `AGENT_GATE_TARGET_NAME` | `--target-name` | 自动探测或 `AgentGate` | 登录页显示的服务名称 |
| `AGENT_GATE_TARGET_WAIT_RETRY_DURATION` | `--target-wait-retry-duration` | `5000` | 目标服务未就绪时的重试间隔，单位毫秒 |
| `AGENT_GATE_TARGET_WAIT_RETRY_TIMES` | `--target-wait-retry-times` | `10` | 目标服务未就绪时的重试次数；负数表示持续重试 |
| `AGENT_GATE_AUTH_USERNAME` | `--auth-username` | 必填 | 登录用户名 |
| `AGENT_GATE_AUTH_PASSWORD` | `--auth-password` | 必填 | 登录密码 |
| `AGENT_GATE_AUTH_ALLOW_BASIC` | `--auth-allow-basic` | `false` | 允许 HTTP Basic Auth 作为 Cookie Session 的替代方式 |
| `AGENT_GATE_SESSION_COOKIE_KEY` | `--session-cookie-key` | `X-AgentGate-Session` | Session Cookie 名称 |
| `AGENT_GATE_SESSION_EXPIRE` | `--session-expire` | `86400` | Session Cookie 有效期，单位秒；负数表示浏览器会话 Cookie |
| `AGENT_GATE_REQUEST_MAX_SIZE` | `--request-max-size` | 不限制 | 基于 `Content-Length` 的最大请求体大小，例如 `10MB` |

## Docker

```yaml
services:
  agent-gate:
    image: mhmzx/agent-gate:2.0.0
    restart: unless-stopped
    ports:
      - "1180:1180"
    environment:
      AGENT_GATE_AUTH_USERNAME: user
      AGENT_GATE_AUTH_PASSWORD: password
      AGENT_GATE_TARGET_URL: http://target-service:8080
```

先运行 `./gradlew :server:dockerBuildImage` 构建镜像，再运行 `docker compose up` 启动。镜像只包含 Linux x64 Native 可执行文件，不包含 JVM。

## 构建

```bash
./gradlew :server:linkReleaseExecutableLinuxX64
```

输出文件为 `server/build/bin/linuxX64/releaseExecutable/server.kexe`。

Docker 镜像可通过 `./gradlew :server:dockerBuildImage` 构建。Dockerfile 仅在 `server/build/docker/` 中由 Gradle 临时生成，不纳入版本控制。
