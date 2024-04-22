# AgentGate

这是一个利用 SpringCloud Gateway 开发的，为其他服务添加认证环节的应用。

起因是诸如 frp、nas xunlei 这类应用，一般情况下是局域网私有部署私有访问，因此提供的认证方式十分简陋，一般是 basic 认证方式。

这种方式一是不够优雅，访问的时候浏览器弹一个对话框提示输入账号密码，实在是不美观，其次是这种形式也不方便使用密码管理工具自动填充。

因此本仓库应运而生。

## 食用方式

配置文件仅支持 yaml 格式，配置项如下表格：

| 环境变量                          | yaml 节点          | 描述                                                               | 默认值（留空则表示必填）     |
|-------------------------------|------------------|------------------------------------------------------------------|------------------|
| AGENT_GATE_BASE_PATH          | base.path        | 自定义认证页面即接口的前缀，以避免与原有服务冲突，需以 `/` 开头，不以 `/` 结尾                     | /agent-gate      |
| AGENT_GATE_BASE_PORT          | base.port        | 自定义认证监听端口，以避免与原有服务冲突                                             | 1180             |
| AGENT_GATE_BASE_TARGET        | base.target      | 被 AgentGate 保护的目标服务，需包含协议，且不以 `/` 结尾，示例值：`http://127.0.0.1:7890` |                  |
| AGENT_GATE_AUTH_USERNAME      | auth.username    | 认证用户名                                                            |                  |
| AGENT_GATE_AUTH_PASSWORD      | auth.password    | 认证密码                                                             |                  |
| AGENT_GATE_TOKEN_SECURITY_KEY | token.secret-key | token 的签名密钥                                                      | （每次启动随机生成 UUID）  |
| AGENT_GATE_TOKEN_EXPIRE       | token.expire     | 认证有效期，小于 0 则代表仅当前会话有效                                            | 3600 * 24（即一天）   |
| AGENT_GATE_TOKEN_COOKIE_KEY   | token.cookie-key | 自定义 cookie 的 key，以避免与原有服务冲突                                      | X-AgentGate-Auth |


```yaml
version: "3"
services:
  # 原服务
  frpc:
    image: stilleshan/frpc:0.57.0
    network_mode: host
    restart: unless-stopped
    volumes:
      - ./frpc.yaml:/etc/frp/frpc.yaml
    entrypoint: ["/frp/frpc", "-c", "/etc/frp/frpc.yaml"]
  # 认证服务
  agent-gate:
    image: mhmzx/agent-gate:latest
    restart: unless-stopped
    ports:
      - 8080:5000
```

## 路线图

- [ ] 初步完成认证拦截页面
- [ ] 支持配置文件
- [ ] 支持使用反代目标服务自带的 basic 认证
- [ ] 支持自定义前端页面
