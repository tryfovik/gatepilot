# Platform Gateway

独立发布的平台统一网关工程。

目标不是给某个业务项目临时补一层转发壳，而是沉淀一盒能直接对外发布的基础网关：

- 入口模型统一
- 默认治理能力完整
- 接入边界清晰
- 运行期路由命中不靠全量线性扫描

当前代码在工作区里开发，但仓库本身按独立发布件设计，可以直接整理后开源到 GitHub。

## 文档导航

- [接入手册](docs/integration-guide.md)
- `platform-gateway-server/src/main/resources/application.yml`
- `platform-gateway-server/src/main/resources/application-local.example.yml`

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

这套网关只接受标准入口，不再为历史路径保留别名。例如：

- 标准入口：`/api/game/admin/system/ping`
- 标准入口：`/api/game/open/system/ping`

新增项目时只需要在 `projects` 下追加配置，不需要再单独建设一个新网关仓库。

## 企业级能力

当前这版已经内建以下基础能力：

- 统一路由编译：启动时把配置编译为标准化路由定义，校验路径冲突、上游地址和超时配置。
- 运行期路由索引：启动阶段预编译 API / internal 根路径索引，运行时命中不再按全部路由线性扫描。
- 路由级方法约束：每条路由可声明 `api-methods` 与 `internal-methods`，不允许的方法返回 `405 Method Not Allowed` 并带 `Allow` 响应头。
- 路由级请求体限制：每条路由可声明 `api-max-request-size` 与 `internal-max-request-size`，超限请求直接在网关层返回 `413 Payload Too Large`。
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
- `projects.<project>.routes.<route>.service-uri`
  API 请求的上游地址。
- `projects.<project>.routes.<route>.api-methods`
  当前 API 路由允许的 HTTP 方法列表；为空时表示不限制。
- `projects.<project>.routes.<route>.api-max-request-size`
  当前 API 路由允许的最大请求体大小，支持 `10MB`、`512KB` 这类 `DataSize` 写法。
- `projects.<project>.routes.<route>.service-path-prefix`
  API 转发时改写到上游的路径前缀。
- `projects.<project>.routes.<route>.actuator-uri`
  运维请求和健康检查的上游地址。
- `projects.<project>.routes.<route>.internal-methods`
  当前内部运维路由允许的 HTTP 方法列表；为空时表示不限制。
- `projects.<project>.routes.<route>.internal-max-request-size`
  当前内部运维路由允许的最大请求体大小，超限时返回 `413`。
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

## 快速接入

如果是新项目首次接入，至少需要明确四件事：

- 这个项目在网关里的标准入口名，例如 `game`
- 这个项目下要暴露的路由入口，例如 `admin`、`open`
- 每条路由对应的业务上游地址和上游路径前缀
- 每条路由是否强制认证、哪些相对路径允许匿名访问

最小配置示例：

```yaml
platform:
  gateway:
    projects:
      village-care:
        enabled: true
        path-segment: village-care
        display-name: Village Care
        routes:
          admin:
            enabled: true
            api-enabled: true
            actuator-enabled: true
            path-segment: admin
            service-uri: http://127.0.0.1:19080
            api-methods:
              - GET
              - POST
            api-max-request-size: 10MB
            service-path-prefix: /admin
            actuator-uri: http://127.0.0.1:19080
            internal-methods:
              - GET
            internal-max-request-size: 1MB
            connect-timeout-ms: 2000
            response-timeout: 5s
            auth:
              required: true
              public-paths:
                - /system/ping
          open:
            enabled: true
            api-enabled: true
            actuator-enabled: true
            path-segment: open
            service-uri: http://127.0.0.1:19080
            service-path-prefix: /open
            actuator-uri: http://127.0.0.1:19080
            api-max-request-size: 2MB
            auth:
              required: false
```

配置完成后，标准访问地址就是：

- 对外 API：`/api/village-care/admin/**`
- 对外 API：`/api/village-care/open/**`
- 内部运维：`/internal/village-care/admin/**`
- 内部运维：`/internal/village-care/open/**`

接入后的首轮联调建议直接验证：

- `GET /api/village-care/open/system/ping`
- `GET /api/village-care/admin/system/ping`
- 对一个未放行的方法执行请求，确认返回 `405` 且响应头带 `Allow`
- 对一个超过限制的大请求体执行请求，确认返回 `413`
- `GET /internal/village-care/admin/actuator/health`
- `GET /actuator/platformGatewayRoutes`

完整接入步骤、联调命令和排障说明见 [接入手册](docs/integration-guide.md)。

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

运行期可以直接通过以下 Actuator 端点观察网关状态：

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/platformGatewayRoutes`

其中 `/actuator/platformGatewayRoutes` 会返回当前生效的项目、路由、路径根、认证策略、超时和流控策略，适合在联调和变更发布后做快速核对。

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
