# GatePilot

GatePilot 是网关控制与运行系统。

目标不是给某个业务项目临时补一层转发壳，而是沉淀一盒能直接对外发布的基础网关：

- 入口模型统一
- 默认治理能力完整
- 接入边界清晰
- 运行期路由命中不靠全量线性扫描

当前代码还保留部分历史 `platform-gateway-*` 模块名，后续按 GatePilot 架构逐步迁移。

## 文档导航

- [规划与进度](docs/roadmap.md)
- [GatePilot 建设规划](docs/gatepilot-plan.md)
- [GatePilot 架构边界规范](docs/architecture-boundaries.md)
- [GatePilot Console 设计规范](docs/console-design-guidelines.md)
- [GatePilot 执行计划](docs/gatepilot-execution-plan.md)
- [接入手册](docs/integration-guide.md)
- `gatepilot-console/README.md`
- `platform-gateway-server/src/main/resources/application.yml`
- `platform-gateway-server/src/main/resources/application-local.example.yml`

## 目录结构

- `gatepilot-api`
  声明式资源、枚举和 DTO。
- `gatepilot-apiserver`
  控制面 API，负责配置存储、查询、发布入口、回滚入口、快照、事件和 agent 协议。
- `gatepilot-controller-manager`
  控制器集合，负责把发布意图 reconcile 成 `PublishedConfig`。
- `gatepilot-agent`
  网关节点侧代理，负责注册、心跳、拉取配置、staged / last-good、调用 proxy apply 和结果上报。
- `gatepilot-proxy`
  数据面网关，只消费 `PublishedConfig` 并执行路由、治理、染色和发布策略。
- `gatepilot-console`
  Vue 管理台，只调用 apiserver。
- `gatepilot-app`
  单体合包装配模块，只负责组合 apiserver、controller-manager、agent、proxy 和 console 静态资源。

## 前后端分离

管理台按前后端分离建设，职责边界固定：

- 后端：`gatepilot-apiserver` 提供 REST/JSON API。
- 前端：`gatepilot-console` 使用 Vue 3 + Vite + TypeScript，只调用 apiserver。
- 单体：`gatepilot-app` 可以承载 console 静态资源，但不写业务逻辑。
- 分服务：console、apiserver、controller-manager、agent、proxy 都可以拆开部署。
- 生产配置必须持久化到数据库，内存存储只允许开发测试使用。
- 业务项目不需要依赖 GatePilot client jar，主路径是在网关后台统一配置和发布。

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
- 路由级重试退避：每条 API 路由可声明 `governance.retry.*`，只对显式允许的方法做瞬时失败重试与退避。
- 路由级熔断 fallback：每条 API 路由可声明 `governance.circuit-breaker.*`，异常可即时 fallback，指定状态码和慢调用会进入熔断统计，熔断打开后返回统一 JSON fallback。
- 路由级认证策略：每条路由可声明 `auth.required` 与 `auth.public-paths`，不再用硬编码路径散落在过滤器里。
- 路由级超时治理：支持 `connect-timeout-ms` 和 `response-timeout`，直接下沉为 Gateway route metadata。
- 路由级流量治理：每条 API 路由可声明 `governance.flow-control.*`，启动时自动装载为 Sentinel Gateway API 分组和流控规则。
- 流量染色透传：支持按 header / cookie / query 规则计算 `X-Traffic-Color`，并向下游与响应头统一透传。
- 路由级发布变体：每条 API 路由可声明 `release.variants.*`，按流量颜色优先命中灰度、蓝绿等发布槽位。
- 权重灰度发布：发布变体可声明 `weight`，没有显式颜色的流量会按用户、租户、TraceId 或客户端地址做稳定哈希分流。
- 平台上下文透传：默认向上游注入 `X-Platform-Project`、`X-Platform-Route`。
- 内部入口隔离：运维入口统一收敛在 `/internal/**`，不对公网业务路径混放。
- 上游健康聚合：`gatewayUpstreams` 会主动检查各路由及其发布变体的 actuator 健康状态。
- 生效路由目录：新增 Actuator 端点 `/actuator/platformGatewayRoutes`，可直接查看当前生效的项目、路径、认证策略、流控策略、重试策略、流量染色和发布变体。
- 路由诊断 API：内部端点 `/internal/_platform-gateway/diagnostics/route` 可模拟 method / path / headers / query / cookies，返回路由命中、认证、染色、发布变体、上游和治理策略。
- 访问审计：默认记录最近请求审计事件，并可通过 `/internal/_platform-gateway/audits` 按 traceId、项目、路由、状态码、颜色和变体查询。
- 管理配置治理 API：内部端点提供当前配置导出、候选配置 dry-run 校验、配置 diff、候选版本快照和发布事件记录，作为后续 Web 管理界面的发布前校验底座。

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
- `projects.<project>.routes.<route>.governance.retry.*`
  当前 API 路由的重试与退避策略，包括次数、方法、状态码、异常和退避参数。
- `projects.<project>.routes.<route>.governance.circuit-breaker.*`
  当前 API 路由的熔断与 fallback 策略，包括熔断器名称、触发状态码、fallback URI 和内置 fallback JSON。
- `traffic-color.*`
  全局流量染色规则，包括颜色头名称、是否信任请求头、默认颜色以及按 header / cookie / query 的染色规则。
- `audit.*`
  访问审计配置，包括是否启用、是否输出结构化日志、最近事件保留条数和 Trace 头名称。
- `projects.<project>.routes.<route>.release.variants.*`
  当前 API 路由的发布变体，按流量颜色命中不同上游，可用于灰度和蓝绿发布。
- `projects.<project>.routes.<route>.release.weight-hash-headers`
  权重灰度的稳定哈希请求头，默认依次使用 `X-User-Id`、`X-Tenant-Id`、`X-Trace-Id`，都没有时回落到客户端地址。
- `projects.<project>.routes.<route>.release.variants.<variant>.weight`
  当前发布变体在无显式颜色流量中的灰度比例，取值 `0-100`，同一路由启用变体的权重总和不能超过 `100`。
- `projects.<project>.routes.<route>.connect-timeout-ms`
  路由连接超时。
- `projects.<project>.routes.<route>.response-timeout`
  路由响应超时。

默认示例见：

- `platform-gateway-server/src/main/resources/application.yml`
- `platform-gateway-server/src/main/resources/application-local.example.yml`

## 快速接入

如果是新项目首次接入，至少需要明确六件事：

- 这个项目在网关里的标准入口名，例如 `game`
- 这个项目下要暴露的路由入口，例如 `admin`、`open`
- 每条路由对应的业务上游地址和上游路径前缀
- 每条路由是否强制认证、哪些相对路径允许匿名访问
- 是否需要在网关入口做流量染色，以及染色规则按 header、cookie 还是 query 生效
- 哪些路由需要灰度 / 蓝绿发布变体，以及这些变体分别对应哪个上游

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

## 灰度 / 蓝绿发布

平台网关把“流量染色”和“发布变体”拆成两层能力：

- 入口先通过 `platform.gateway.traffic-color.*` 计算当前请求的流量颜色。
- API 路由再通过 `platform.gateway.projects.<project>.routes.<route>.release.variants.*` 按颜色命中具体发布槽位。

这样可以直接覆盖三类真实场景：

- 流量染色：网关按 header / cookie / query 给请求打 `X-Traffic-Color`。
- 全链路灰度：同一个 `X-Traffic-Color` 会继续透传给下游，下游再调用其他服务时也能沿用。
- 蓝绿发布：默认上游承接主流量，变体上游承接指定颜色流量；切流时只需要调整默认上游与变体配置。
- 权重灰度：没有命中显式染色规则时，网关按稳定哈希把一部分流量打到变体颜色。

示例配置：

```yaml
platform:
  gateway:
    traffic-color:
      enabled: true
      header-name: X-Traffic-Color
      response-header-enabled: true
      trust-request-header: false
      default-color: stable
      rules:
        - name: canary-header
          source: header
          field-name: X-Canary
          pattern: true
          match-strategy: exact
          color: green
    projects:
      game:
        routes:
          open:
            service-uri: http://127.0.0.1:18080
            service-path-prefix: /open
            actuator-uri: http://127.0.0.1:18080
            release:
              weight-hash-headers:
                - X-User-Id
                - X-Tenant-Id
                - X-Trace-Id
              variants:
                green:
                  service-uri: http://127.0.0.1:28080
                  actuator-uri: http://127.0.0.1:28080
                  weight: 10
                  match-colors:
                    - green
```

这套模型里，`green` 颜色会优先命中绿色变体；没有显式颜色的流量会按稳定哈希让约 10% 命中绿色变体，其余流量仍然回落到默认上游。浏览器联调时，响应头里也能直接看见 `X-Traffic-Color`。

## Trace 联动

平台网关不自造另一套 Trace 体系，直接复用 `getboot-observability` 的入口 Trace 约定。

- 客户端已带 `X-Trace-Id` 时，网关直接复用并透传到上游。
- 客户端没带 `X-Trace-Id` 时，网关生成新的 TraceId，并同时补齐到请求头和响应头。
- 下游项目如果也接了 `getboot-observability`，会自动沿用同一个 TraceId。
- 下游项目如果没接 `getboot-observability`，也应该至少读取并继续透传配置的 Trace 头，默认就是 `X-Trace-Id`。

网关负责把第一跳 Trace 链建好，但不会替下游服务补内部日志、RPC、MQ 的透传逻辑。

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

其中 `/actuator/platformGatewayRoutes` 会返回当前生效的项目、路由、路径根、认证策略、超时、流量染色和发布变体，适合在联调和变更发布后做快速核对。

内部路由诊断 API 示例：

```bash
curl -X POST 'http://127.0.0.1:18083/internal/_platform-gateway/diagnostics/route' \
  -H 'Content-Type: application/json' \
  -d '{
    "method": "GET",
    "path": "/api/game/admin/system/ping",
    "headers": {
      "X-Canary": ["true"],
      "X-User-Id": ["10001"]
    },
    "query": {},
    "cookies": {},
    "remoteAddress": "127.0.0.1"
  }'
```

诊断结果会说明是否命中路由、是否需要认证、方法是否允许、流量颜色、发布变体、最终上游、重试、流控和熔断策略。这个接口是后续 Web 管理界面的排障底座。

最近访问审计查询示例：

```bash
curl -s 'http://127.0.0.1:18083/internal/_platform-gateway/audits?projectKey=game&routeKey=admin&limit=20'
curl -s 'http://127.0.0.1:18083/internal/_platform-gateway/audits?traceId=trace-demo-001'
```

审计事件包含 traceId、clientIp、method、path、routeType、projectKey、routeKey、status、latencyMs、trafficColor、releaseVariant、upstreamUri、methodAllowed、authRequired、publicPath、fallback、outcome 和 error。这个接口只保留最近事件，长期检索应接日志平台。

管理配置治理 API 示例：

```bash
curl -s 'http://127.0.0.1:18083/internal/_platform-gateway/config/effective'
curl -s -X POST 'http://127.0.0.1:18083/internal/_platform-gateway/config/validate' \
  -H 'Content-Type: application/json' \
  --data-binary @candidate-gateway-config.json
curl -s -X POST 'http://127.0.0.1:18083/internal/_platform-gateway/config/diff' \
  -H 'Content-Type: application/json' \
  --data-binary @candidate-gateway-config.json
curl -s -X POST 'http://127.0.0.1:18083/internal/_platform-gateway/config/versions' \
  -H 'Content-Type: application/json' \
  --data-binary @candidate-gateway-snapshot.json
curl -s 'http://127.0.0.1:18083/internal/_platform-gateway/config/versions?limit=20'
curl -s 'http://127.0.0.1:18083/internal/_platform-gateway/config/releases?limit=20'
```

`config/validate` 会复用启动期 fail-fast 校验并编译路由摘要；`config/diff` 会比较当前配置和候选配置的全局入口、上下文头、染色、审计、上游、方法、认证、重试、流控、熔断和发布变体变化。`config/versions` 会保存候选配置快照、校验结果和 diff，`config/releases` 会返回启动生效配置和候选快照创建事件。当前接口只做预检、记录和只读 diff，真正的动态发布、持久化版本库和回滚会在后续管理面能力里继续补。

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

路由级重试示例：

```yaml
platform:
  gateway:
    projects:
      game:
        routes:
          open:
            governance:
              retry:
                enabled: true
                retries: 2
                methods:
                  - GET
                statuses:
                  - 502
                  - 503
                  - 504
                exceptions:
                  - io
                  - timeout
                backoff:
                  first-backoff: 20ms
                  max-backoff: 200ms
                  factor: 2
                  based-on-previous-value: true
```

路由级熔断 fallback 示例：

```yaml
platform:
  gateway:
    projects:
      game:
        routes:
          open:
            governance:
              circuit-breaker:
                enabled: true
                status-codes:
                  - 500
                  - 502
                  - 503
                  - 504
                sliding-window-size: 100
                minimum-number-of-calls: 20
                failure-rate-threshold: 50
                wait-duration-in-open-state: 30s
                permitted-number-of-calls-in-half-open-state: 10
                slow-call-duration-threshold: 5s
                slow-call-rate-threshold: 80
                fallback-status: 503
                fallback-code: 503
                fallback-message: Service temporarily unavailable
```

未显式配置 `fallback-uri` 时，网关会直接返回内置 UTF-8 JSON。需要自定义 fallback 时，`fallback-uri` 目前只支持 `forward:`。

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

- 当前单元测试覆盖路由定义编译、认证过滤、配置校验、路由目录、流量染色、熔断、审计、诊断和管理配置治理。
- 依赖本地 socket 绑定的集成测试在受限环境下默认禁用，需要在可绑定端口的环境里执行。
