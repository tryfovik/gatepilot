# Platform Gateway

独立发布的平台统一网关工程。

这个仓库放在外层工作区 `gateway/` 目录下，但内部 `platform-gateway/` 是独立 Git 仓库，不并入当前 `getboot` 主仓 reactor，也不和 `game`、`village-care` 这类业务项目混在一起。

## 目录结构

- `platform-gateway-runtime`
  平台网关运行时模块，承载配置模型、路由编译、认证过滤、内部运维入口保护、健康检查和配置校验。
- `platform-gateway-server`
  平台网关服务启动模块，负责装配观测、治理、默认配置和应用启动入口。

## 路由模型

网关不再按“每个项目一个网关”的思路建设，而是统一采用 `project -> route` 两级模型：

```yaml
platform:
  gateway:
    projects:
      game:
        path-segment: game
        routes:
          admin:
            path-segment: admin
          open:
            path-segment: open
      village-care:
        path-segment: village-care
        routes:
          admin:
            path-segment: admin
```

标准入口规则：

- 对外 API：`/api/{project}/{route}/**`
- 内部运维：`/internal/{project}/{route}/**`

为了平滑替换存量项目网关，每条路由还支持 `legacy-path-segments`，用于兼容历史入口。例如：

- 标准入口：`/api/game/admin/system/ping`
- 历史兼容入口：`/api/admin/system/ping`

新增项目时只需要在 `projects` 下追加配置，不需要再单独建设一个新网关仓库。

## 企业级能力

当前这版已经内建以下基础能力：

- 统一路由编译：启动时把配置编译为标准化路由定义，校验路径冲突、上游地址和超时配置。
- 路由级认证策略：每条路由可声明 `auth.required` 与 `auth.public-paths`，不再用硬编码路径散落在过滤器里。
- 路由级超时治理：支持 `connect-timeout-ms` 和 `response-timeout`，直接下沉为 Gateway route metadata。
- 路由级流量治理：每条 API 路由可声明 `governance.flow-control.*`，启动时自动装载为 Sentinel Gateway API 分组和流控规则。
- 平台上下文透传：默认向上游注入 `X-Platform-Project`、`X-Platform-Route`。
- 内部入口隔离：运维入口统一收敛在 `/internal/**`，不对公网业务路径混放。
- 上游健康聚合：`gatewayUpstreams` 会主动检查各路由的 actuator 健康状态。
- 生效路由目录：新增 Actuator 端点 `/actuator/platformGatewayRoutes`，可直接查看当前生效的项目、路径、认证策略、流控策略、超时和上游配置。

## 配置说明

核心配置在 `platform.gateway.*` 下，重点字段如下：

- `api-prefix` / `internal-prefix`
  统一约束外部业务入口与内部运维入口。
- `projects.<project>.path-segment`
  项目在平台网关中的标准路径段。
- `projects.<project>.routes.<route>.legacy-path-segments`
  存量入口兼容别名。
- `projects.<project>.routes.<route>.service-uri`
  API 请求的上游地址。
- `projects.<project>.routes.<route>.service-path-prefix`
  API 转发时改写到上游的路径前缀。
- `projects.<project>.routes.<route>.actuator-uri`
  运维请求和健康检查的上游地址。
- `projects.<project>.routes.<route>.auth.required`
  当前路由是否强制认证。
- `projects.<project>.routes.<route>.auth.public-paths`
  当前路由下允许匿名访问的相对路径。
- `projects.<project>.routes.<route>.governance.flow-control.*`
  当前路由的 Sentinel 流控策略，包括 QPS、突发额度、控制行为和热点参数维度限流。
- `projects.<project>.routes.<route>.connect-timeout-ms`
  路由连接超时。
- `projects.<project>.routes.<route>.response-timeout`
  路由响应超时。

默认示例见：

- `platform-gateway-server/src/main/resources/application.yml`
- `platform-gateway-server/src/main/resources/application-local.example.yml`

## 治理与观测

发布件已经引入：

- `getboot-observability`
- `getboot-governance`

默认配置里已预留 Sentinel Gateway fallback 模板；如果要真正启用治理能力，再打开：

```yaml
getboot:
  governance:
    enabled: true
    sentinel:
      enabled: true
```

路由级流控示例：

```yaml
platform:
  gateway:
    projects:
      game:
        routes:
          admin:
            governance:
              flow-control:
                enabled: true
                count: 200
                interval-sec: 1
                burst: 50
                control-behavior: rate-limiter
                max-queueing-timeout-ms: 300
                param:
                  enabled: true
                  parse-strategy: header
                  field-name: X-Tenant-Id
                  pattern: vip-.*
                  match-strategy: regex
```

## 构建与测试

构建前提：

1. `com.dt:getboot-spring-boot-starter-parent:1.0.0` 可从本地或私服解析。
2. `getboot-auth`、`getboot-observability`、`getboot-governance` 已安装到 Maven 仓库。

常用命令：

```bash
mvn -q -DskipTests package
mvn -q test
```

说明：

- 当前单元测试覆盖路由定义编译、认证过滤、配置校验和路由目录端点。
- 依赖本地 socket 绑定的集成测试在受限环境下默认禁用，需要在可绑定端口的环境里执行。
