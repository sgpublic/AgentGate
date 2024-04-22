# AgentGate

这是一个利用 SpringCloud Gateway 开发的，为其他服务添加认证环节的应用。

起因是诸如 frp、nas xunlei 这类应用，一般情况下是局域网私有部署私有访问，因此提供的认证方式十分简陋，一般是 basic 认证方式。

这种方式一是不够优雅，访问的时候浏览器弹一个对话框提示输入账号密码，实在是不美观，其次是这种形式也不方便使用密码管理工具自动填充。

因此本仓库应运而生。

## 配置

配置项如下表格：

| 环境变量                          | 配置文件 yaml 节点          | 命令行参数                   | 描述                                                                                                                                                                                              | 默认值（留空则表示必填）                                                             |
|-------------------------------|-----------------------|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| AGENT_GATE_BASE_PATH          | base.path             | --base-path             | 自定义认证页面即接口的前缀，以避免与原有服务冲突，需以 `/` 开头，不以 `/` 结尾。                                                                                                                                                   | /agent-gate                                                              |
| AGENT_GATE_BASE_PORT          | base.port             | --base-port             | 自定义认证监听端口，以避免与原有服务冲突。                                                                                                                                                                           | 1180                                                                     |
| AGENT_GATE_BASE_DEBUG         | base.debug            | --base-debug            | 开启 debug 模式，将打印更多日志。                                                                                                                                                                            | false                                                                    |
| AGENT_GATE_TARGET_URL         | base.target.url       | --base-target-url       | 目标服务监听地址，需包含协议，且不以 `/` 结尾。<br/>若目标服务使用 basic 认证，则需要将账号密码填写在 url 中。<br/>示例值：`http://127.0.0.1:7890`                                                                                              |                                                                          |
| AGENT_GATE_TARGET_HOME_PATH   | base.target.home-path | --base-target-home-path | 目标服务主页地址，认证成功后将跳转此地址                                                                                                                                                                            | /                                                                        | 
| AGENT_GATE_TARGET_LOGO        | base.target.logo      | --base-target-logo      | 目标服务的 LOGO 的 URL，支持本地文件和网络文件。<br/>本地文件请以 `file://` 开头<br/>网络文件请以 `http://` 或 `https://` 开头。<br/>若想使用目标服务自带的 Logo，请直接填写绝对路径，例如 `/favicon.ico`，则会使用 `$AGENT_GATE_TARGET_URL/favicon.ico` 作为 Logo。 | /favicon.ico                                                             |
| AGENT_GATE_TARGET_NAME        | base.target.name      | --base-target-name      | 被 AgentGate 保护的目标服务的名称。                                                                                                                                                                         | （尝试从 `$AGENT_GATE_TARGET_URL$AGENT_GATE_TARGET_HOME_PATH` 所指向的 HTML 中获取） |
| AGENT_GATE_AUTH_USERNAME      | auth.username         | --auth-username         | 认证用户名。                                                                                                                                                                                          |                                                                          |
| AGENT_GATE_AUTH_PASSWORD      | auth.password         | --auth-password         | 认证密码。                                                                                                                                                                                           |                                                                          |
| AGENT_GATE_TOKEN_SECURITY_KEY | token.secret-key      | --token-secret-key      | token 的签名密钥。                                                                                                                                                                                    | （每次启动随机生成 UUID）                                                          |
| AGENT_GATE_TOKEN_EXPIRE       | token.expire          | --token-expire          | 认证有效期，小于 0 则代表仅当前会话有效。                                                                                                                                                                          | 3600 * 24（即一天）                                                           |
| AGENT_GATE_TOKEN_COOKIE_KEY   | token.cookie-key      | --token-cookie-key      | 自定义 cookie 的 key，以避免与原有服务冲突。                                                                                                                                                                    | X-AgentGate-Auth                                                         |

配置文件仅支持 yaml 格式，配置项优先使用排序：环境变量 > 命令行参数 > 配置文件。

## 部署

使用 docker-compose 部署方式如下：

```yaml
version: "3"
services:
  # 原服务
  frpc:
    image: stilleshan/frpc:latest
    network_mode: host
    restart: unless-stopped
    volumes:
      - ./frpc.yaml:/etc/frp/frpc.yaml
    entrypoint: ["/frp/frpc", "-c", "/etc/frp/frpc.yaml"]
  # 认证服务
  agent-gate:
    image: mhmzx/agent-gate:latest
    restart: unless-stopped
    network_mode: host # 非必须使用 host 模式，根据实际需求选择
    volumes:
      - ./agent-gate.yaml:/etc/agent-gate.yaml # 可使用配置文件或命令行
```

## 路线图

- [x] 初步完成认证拦截页面
- [x] 支持环境变量配置参数
- [ ] 支持命令行传递参数
- [ ] 支持配置文件
- [ ] 支持自定义前端页面
- [ ] 支持内网跳过认证
