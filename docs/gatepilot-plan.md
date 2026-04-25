# GatePilot 建设规划

更新时间：2026-04-25

本文档把 GatePilot 应该承担的职责、当前项目已经实现的能力、未实现能力和后续开发顺序放在一个文件里，作为后续开发的主规划。

模块边界和后续迁移必须遵守 [GatePilot 架构边界规范](architecture-boundaries.md)。
Web 管理台建设必须遵守 [GatePilot Console 设计规范](console-design-guidelines.md)。

## 1. 项目定位

GatePilot 应该是平台统一入口的控制与运行系统，不是某个业务系统的临时反向代理。

后期会接入很多项目，并且部分项目会有高并发、大流量、灰度发布、蓝绿切换和严格运维隔离诉求。因此网关必须坚持三个原则：

1. 入口统一：所有项目都走 `/api/{project}/{route}/**` 和 `/internal/{project}/{route}/**`。
2. 治理统一：认证、限流、超时、重试、熔断、染色、发布、审计和观测都优先在网关层形成统一能力。
3. 配置驱动：新增项目、新增路由、灰度切流、蓝绿发布和治理策略调整都应该优先通过配置表达，不把业务项目差异写死进代码。

## 2. 平台级网关必须具备的能力

### 2.1 多项目统一接入

- 支持 `project -> route` 两级模型。
- 支持项目级和路由级启停。
- 支持 API 入口与 internal 运维入口分离。
- 支持启动期校验路径冲突、上游地址缺失、配置非法值。
- 支持运行期快速命中路由，不能在大流量下靠全量线性扫描。

### 2.2 高并发流量治理

- 路由级连接超时和响应超时。
- 路由级请求体大小限制，避免大包拖垮网关和上游。
- 路由级 HTTP 方法白名单。
- 路由级 QPS 限流、突发流量、排队等待。
- 热点参数限流，支持租户、用户、IP、header、cookie、query 等维度。
- 路由级重试，只允许用于幂等或可重放请求。
- 路由级熔断、隔离和 fallback，保护上游故障时网关整体稳定性。

### 2.3 流量染色与发布

- 支持按 header、cookie、query 做流量染色。
- 支持统一透传 `X-Traffic-Color`，保证全链路灰度可以延续到下游。
- 支持显式颜色命中发布变体，用于蓝绿和定向灰度。
- 支持无显式颜色时按稳定哈希做权重灰度，保证同一用户、租户或客户端在一段时间内稳定命中同一发布槽位。
- 支持发布变体健康检查、权重调整、默认上游切换和快速回滚。

### 2.4 安全边界

- API 入口按路由声明认证策略和匿名放行路径。
- internal 入口必须和公网业务入口隔离。
- 管理面需要 RBAC、操作审计、只读查看、配置校验和发布审批。
- 后续需要可信代理链和真实客户端 IP 解析，避免客户端伪造 `X-Forwarded-For` 影响限流、审计和灰度分流。

### 2.5 观测与排障

- 统一 Trace 头透传。
- 按 project、route、variant 输出指标。
- 记录结构化访问审计日志，包含 traceId、clientIp、project、route、method、path、status、latency、trafficColor、releaseVariant、upstreamUri、authResult。
- 暴露生效路由目录、治理规则、发布变体、上游健康状态。
- 支持配置导出、配置 diff、dry-run 校验、变更记录和回滚快照。

### 2.6 Web 管理界面

后续必须建设 Web 管理界面，不能只停留在 Actuator JSON 和日志排查。管理界面不是展示页，而是网关日常运维和故障定位入口。

Web 管理界面必须采用前后端分离：

- 前端单独放在 `gatepilot-console` 模块，不打进数据面发布件。
- 管理后端单独放在 `platform-gateway-management` 模块，不混进 `platform-gateway-runtime`。
- 网关数据面由 `platform-gateway-server` 单独启动，默认端口 `18082`，只负责转发、治理、审计采集和基础观测，不承载管理台页面和配置发布流程。
- 管理后端由 `platform-gateway-admin-server` 单独启动，默认端口 `18083`，作为普通 WebFlux/Actuator 管理服务运行，不加载 Spring Cloud Gateway 数据面自动配置。
- 后端只暴露 REST/JSON 管理 API，不做服务端模板渲染。
- 前端通过可配置 API Base 调用管理后端上的 `/actuator/**` 和 `/internal/_platform-gateway/**`。
- 前端静态资源可以独立部署到 Nginx、对象存储、CDN 或单独前端服务。
- 网关转发运行时不能依赖前端模块启动，避免管理台故障影响业务流量。
- 依赖方向必须保持清晰：数据面服务不依赖管理面模块，管理前端不依赖后端 jar，管理后端不能把 Gateway 转发链路带进自身进程。

管理界面至少要覆盖：

- 总览大盘：请求量、成功率、错误率、P95/P99、限流次数、熔断次数、fallback 次数、上游健康状态。
- 项目与路由目录：按 project / route 查看入口路径、上游地址、方法限制、请求体限制、认证策略、超时、重试、流控、熔断、发布变体。
- 实时健康：默认上游和发布变体健康检查结果、最近失败原因、最近恢复时间。
- 灰度和蓝绿发布：查看颜色规则、权重分流、命中变体、变体健康，支持调权、启停、切主、回滚前校验。
- 配置治理：当前配置导出、候选配置 dry-run、配置 diff、版本快照、发布记录、失败回滚。
- 访问审计：按 traceId、clientIp、project、route、status、trafficColor、releaseVariant 查询请求记录。
- Trace 排障：输入 traceId 后能看到网关命中路由、认证结果、染色结果、变体命中、上游地址、耗时和响应状态。
- 请求模拟：输入 method、path、headers、query 后模拟路由命中、认证是否放行、染色结果、发布变体、限流 key 和熔断策略。
- 告警视图：展示路由错误率、慢调用、上游不可用、熔断打开、限流突增、配置发布失败等事件。
- 权限与审计：管理界面需要 RBAC，所有配置变更、发布、回滚、调权动作必须有操作审计。

## 3. 当前代码已经实现

当前后端 Maven 工程分为四个模块，另有一个独立前端模块：

- `platform-gateway-runtime`：数据面运行时，包含配置模型、路由编译、过滤器、安全策略、审计采集、健康检查、路由目录、配置校验。
- `platform-gateway-management`：管理面后端 API，包含诊断、审计查询、配置治理、版本快照和发布事件 REST API；依赖边界排除 Gateway 数据面 starter。
- `platform-gateway-server`：网关数据面启动入口，只负责装配运行时、观测和治理依赖、默认配置。
- `platform-gateway-admin-server`：管理面后端启动入口，单独装配管理 API，默认端口 `18083`，不加载 Spring Cloud Gateway 转发链路。
- `gatepilot-console`：独立 Vue 前端管理台，前后端分离，通过 REST/JSON 调用 apiserver。

已实现能力：

- 统一入口模型：`/api/{project}/{route}/**`。
- 内部运维入口：`/internal/{project}/{route}/**`。
- `project -> route` 两级配置。
- 项目和路由启停：`enabled`、`api-enabled`、`actuator-enabled`。
- 启动期路由编译与路径冲突校验。
- 运行期 API / internal 路由索引。
- API 路径改写到 `service-path-prefix`。
- internal 路径转发到 `actuator-uri`。
- CORS 配置。
- internal 入口回环地址保护。
- 路由级方法白名单，非法方法返回 `405` 并带 `Allow`。
- 路由级请求体大小限制，超限返回 `413`。
- 路由级认证和匿名放行路径。
- 连接超时和响应超时。
- 路由级重试与退避。
- 路由级熔断 fallback，支持异常即时 fallback、指定状态码统计、慢调用统计和内置 UTF-8 JSON fallback。
- Sentinel 网关 API 分组和 QPS 流控规则注册。
- Sentinel 热点参数流控。
- 流量染色，支持 header、cookie、query。
- 发布变体按流量颜色优先命中。
- 发布变体权重灰度，支持无显式颜色时按稳定哈希分流。
- 平台上下文头透传：`X-Platform-Project`、`X-Platform-Route`。
- 上游健康聚合：`gatewayUpstreams`。
- Actuator 生效路由目录：`platformGatewayRoutes`。
- 内部路由诊断 API：`/internal/_platform-gateway/diagnostics/route`，支持模拟请求并返回路由命中、访问控制、流量染色、发布变体、上游和治理策略。
- 访问审计基础能力：结构化访问日志、最近事件内存保留和 `/internal/_platform-gateway/audits` 查询 API。
- 管理配置治理 API：`/internal/_platform-gateway/config/effective`、`/internal/_platform-gateway/config/validate`、`/internal/_platform-gateway/config/diff`，支持当前配置导出、候选配置 dry-run 校验和发布前 diff。
- 管理配置版本快照：`/internal/_platform-gateway/config/versions` 和 `/internal/_platform-gateway/config/releases`，支持保存候选配置校验结果、diff 和发布事件记录。
- 管理面后端模块：`platform-gateway-management`，管理 REST API 已从 `platform-gateway-runtime` 拆出。
- 管理面后端服务：`platform-gateway-admin-server`，可与网关数据面服务分开启动和扩缩容。
- 独立前端管理台模块：`gatepilot-console`，已具备总览、资源列表、快照 diff、节点健康等页面骨架。

## 4. 当前未实现或不完整

这些是后续必须补齐的生产级能力：

1. 路由级熔断已具备基础 fallback、失败率、慢调用和半开探测，隔离、实例级参数下发和更完整的运行期观测还需要补齐。
2. internal 入口目前只做回环地址保护，还没有可信代理链和真实客户端 IP。
3. 访问审计已具备最近事件查询和结构化日志，但长期存储、采样、脱敏、Trace 关联和日志平台检索还没有完成。
4. 路由级指标维度还不够完整，缺少 variant、retry、fallback、upstream latency 等细粒度指标。
5. 管理面已有内部配置导出、dry-run 校验、diff、内存版本快照和发布事件记录，但还没有动态发布、回滚、持久化版本库和发布审批能力。
6. 配置仍是静态配置，没有动态刷新和失败保留旧配置机制。
7. Web 管理界面已有独立前端模块骨架，但还没有生产级 RBAC、持久化发布流、指标大盘和告警中心。
8. 安全增强还未展开，包括管理面 RBAC、IP 黑白名单、基础 WAF 或外部 WAF 联动。

## 5. 后续开发路线

### P0：生产兜底

目标：让网关能承接真实高并发生产流量，并在上游异常时保护平台整体稳定。

1. 路由级熔断精细化治理
   - 在已有 `governance.circuit-breaker.*` 基础上继续补隔离策略、实例级参数下发和运行期状态导出。
   - 增强半开探测观测，暴露熔断打开、半开、关闭状态变化事件。
   - 保持统一 JSON fallback 和可选 fallback URI。
   - 路由目录暴露完整熔断策略。

2. 真实客户端 IP 与可信代理链
   - 新增 `real-ip.*` 或 `trusted-proxies.*` 配置。
   - 支持 `X-Forwarded-For`、`X-Real-IP`、`Forwarded`。
   - 只信任配置内代理，避免客户端伪造。
   - 统一供限流、审计和权重灰度使用。

3. 访问审计日志
   - 在已有最近事件查询和结构化日志基础上，接入日志平台或持久化存储。
   - 补充认证结果、重试结果、上游状态和上游耗时。
   - 支持采样、敏感字段脱敏和 Trace 关联检索。

4. 指标增强
   - 增加 project、route、variant、status、exception 标签。
   - 记录网关耗时、上游耗时、重试次数、限流次数、fallback 次数。
   - 控制标签基数，避免指标系统被打爆。

### P1：发布与运维效率

目标：让灰度发布、蓝绿切换和排障可控。

1. 配置治理 API 增强，在已有导出、diff、dry-run、内存版本快照和发布记录基础上补持久化版本库、发布审批和候选配置来源。
2. 动态配置刷新，刷新失败时保留旧配置继续服务。
3. 发布变体操作模型，支持启停、调权、健康检查、默认上游切换和回滚。
4. 管理界面后端 API，继续补总览、指标、Trace 查询、发布记录和告警事件。
5. Runbook：部署、扩容、回滚、Sentinel 接入、代理链配置、常见告警处理。

### P2：平台化管理

目标：让网关从单个服务变成平台能力。

1. 在独立 `gatepilot-console` 模块继续建设 Web 管理界面，覆盖配置查看、校验、发布、回滚、健康、指标、审计、Trace 排障和请求模拟。
2. 支持多环境、多地域、多集群配置对比和发布状态汇总。
3. 把认证、鉴权、限流、审计、染色等能力抽成更明确的扩展点。
4. 增加管理面 RBAC、操作审计和安全策略。

## 6. 推荐提交顺序

后续每次提交都按“配置模型 -> 校验 -> 实现 -> Actuator 视图 -> 测试 -> 文档”闭环。

1. `feat: resolve trusted client ip`
2. `feat: persist gateway audit events`
3. `feat: add route metrics by project route variant`
4. `feat: persist gateway config versions`
5. `feat: support dynamic gateway config refresh`
6. `feat: add gateway management console`

## 7. 开发约束

- 不为历史路径增加别名，统一入口必须保持清爽。
- 不把业务规则写进网关代码，项目差异优先走 route 配置。
- 任何生产能力都必须有启动期 fail-fast 校验。
- 网关直接返回的 JSON 必须明确 UTF-8。
- 每个新增能力都要能从 Actuator、指标或日志里观察到。
- 高并发路径不要引入全量扫描、阻塞 I/O 或高基数字段。
