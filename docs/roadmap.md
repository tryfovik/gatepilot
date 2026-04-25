# GatePilot 规划与进度

本文档记录 GatePilot 的当前建设状态、后续规划和待办事项。它面向继续开发网关的人，不替代接入方看的 [接入手册](integration-guide.md)。

后续模块拆分和新增能力落位必须遵守 [GatePilot 架构边界规范](architecture-boundaries.md)。
Console 页面建设必须遵守 [GatePilot Console 设计规范](console-design-guidelines.md)。

当前基线：

- 最近能力提交：`2488847 feat: add traffic color release routing`
- 最近乱码修复提交：`7cd6c8b fix: force utf-8 gateway error responses`
- 最近验证记录：`mvn -q test` 已通过
- 当前范围：只描述 GatePilot 当前仓库，不覆盖其他业务项目

## 1. 项目定位

GatePilot 是一个可独立发布的平台统一网关控制与运行系统，不是某个业务项目的临时反向代理。

它的核心目标是：

1. 用统一入口承接多个项目：`/api/{project}/{route}/**` 和 `/internal/{project}/{route}/**`。
2. 用配置驱动项目和路由，不再为每个项目单独复制一个网关。
3. 在网关层沉淀认证、方法约束、请求体限制、超时、重试、流控、观测、灰度发布等通用能力。
4. 让运行期路由命中可解释、可观测、可校验，避免靠散落的硬编码路径维护。

当前后端 Maven 工程分为四个模块，另有一个独立前端模块：

- `platform-gateway-runtime`：数据面运行时核心，包含配置模型、路由编译、过滤器、安全策略、审计采集、健康检查、路由目录、配置校验。
- `platform-gateway-management`：管理面后端 API，包含诊断、审计查询、配置治理、版本快照和发布事件能力；编译依赖排除 Spring Cloud Gateway 数据面 starter。
- `platform-gateway-server`：网关数据面启动发布件，只装配运行时、`getboot-observability`、`getboot-governance`、默认配置和应用入口。
- `platform-gateway-admin-server`：管理面后端启动发布件，单独装配管理 API，默认端口 `18083`，不加载 Gateway 转发链路。
- `gatepilot-console`：独立 Vue 前端管理台模块，通过 REST/JSON API 调用 apiserver。

## 2. 当前架构结论

读码后可以把当前网关理解成七层：

1. 配置层：`GatewayProperties` 承接 `platform.gateway.*`，定义 project、route、auth、cors、context headers、traffic color、governance、release variants。
2. 编译层：`GatewayRouteDefinitionLocator` 在启动期把配置编译成标准路由定义，并构建 API / internal 路由索引。
3. 路由层：`GatewayRouteConfiguration` 基于 Spring Cloud Gateway 注册默认路由和发布变体路由，发布变体路由优先级高于默认路由。
4. 过滤层：CORS、流量染色、内部入口保护、方法白名单、认证过滤器按顺序处理请求。
5. 观测层：`platformGatewayRoutes` 暴露生效路由目录，`gatewayUpstreams` 聚合默认上游和发布变体的健康状态。
6. 管理后端层：`platform-gateway-management` 承载管理 API，和 `platform-gateway-runtime` 数据面职责分开。
7. 管理前端层：`gatepilot-console` 独立部署，不嵌入网关数据面 jar，通过可配置 API Base 调用 apiserver。

硬边界：数据面不依赖管理前端；`gatepilot-console` 不依赖任何后端 jar，只调用 apiserver。

这套结构已经适合继续扩展生产治理能力。后续新增能力应优先沿着 `GatewayProperties -> Validator -> RouteDefinition -> RouteConfiguration/Filter -> Actuator View -> Tests -> Docs` 这条链路落地。

## 3. 已完成能力

### 3.1 入口与路由

- [x] 统一入口模型：对外 API 固定为 `/api/{project}/{route}/**`。
- [x] 内部运维入口：固定为 `/internal/{project}/{route}/**`。
- [x] 支持 `project -> route` 两级配置。
- [x] 支持项目和路由启停：`enabled`、`api-enabled`、`actuator-enabled`。
- [x] 启动期路由编译，标准化路径前缀和路径段。
- [x] 启动期路径冲突校验，同一入口根路径不能重复。
- [x] 运行期路由索引，API / internal 请求不再全量线性扫描所有路由。
- [x] API 路由转发时支持 `service-path-prefix` 路径改写。
- [x] internal 路由转发时支持 actuator 上游地址。

### 3.2 安全与访问边界

- [x] 全局 CORS 配置，默认暴露 `X-Trace-Id`、`X-Traffic-Color`、`Authorization`。
- [x] 内部入口隔离：`/internal/**` 当前只允许回环地址访问。
- [x] API / internal 方法白名单：非法方法返回 `405 Method Not Allowed`。
- [x] `405` 响应带 `Allow` 头，方便调用方修正请求。
- [x] API / internal 请求体大小限制：超限返回 `413 Payload Too Large`。
- [x] 路由级认证策略：`auth.required`。
- [x] 路由级匿名放行路径：`auth.public-paths`。
- [x] 认证失败、方法拦截、内部入口拦截等网关直接响应均使用 UTF-8 JSON content type。

### 3.3 治理能力

- [x] 路由级连接超时：`connect-timeout-ms`。
- [x] 路由级响应超时：`response-timeout`。
- [x] 路由级重试：`governance.retry.*`。
- [x] 重试支持方法、状态码、状态码系列、异常类型和退避策略。
- [x] 路由级熔断 fallback：`governance.circuit-breaker.*`。
- [x] 熔断支持异常即时 fallback、指定状态码统计、慢调用统计，并提供内置 UTF-8 JSON fallback。
- [x] Sentinel 网关 API 分组自动注册。
- [x] Sentinel 路由级 QPS 流控规则自动注册。
- [x] Sentinel 热点参数流控，支持 client-ip、host、header、url-param、cookie。
- [x] Sentinel fallback 默认配置已预留，并固定 UTF-8 content type。

### 3.4 观测与上下文

- [x] 复用 `getboot-observability` Trace 体系，默认 Trace 头为 `X-Trace-Id`。
- [x] 支持请求头和响应头 Trace 透传。
- [x] 平台上下文头透传：`X-Platform-Project`、`X-Platform-Route`。
- [x] Actuator 路由目录：`/actuator/platformGatewayRoutes`。
- [x] 上游健康聚合：`gatewayUpstreams`。
- [x] 健康检查覆盖默认上游和发布变体上游。
- [x] 内部路由诊断 API：`/internal/_platform-gateway/diagnostics/route`。
- [x] 诊断 API 可返回路由命中、访问控制、流量染色、发布变体、上游、重试、流控和熔断策略。
- [x] 访问审计基础能力：结构化访问日志、最近事件内存保留和 `/internal/_platform-gateway/audits` 查询 API。
- [x] 审计查询支持按 traceId、project、route、clientIp、status、trafficColor、releaseVariant 过滤。
- [x] 管理配置导出 API：`/internal/_platform-gateway/config/effective`。
- [x] 管理配置 dry-run 校验 API：`/internal/_platform-gateway/config/validate`。
- [x] 管理配置 diff API：`/internal/_platform-gateway/config/diff`。
- [x] 管理配置版本快照 API：`/internal/_platform-gateway/config/versions`。
- [x] 管理配置发布事件 API：`/internal/_platform-gateway/config/releases`。
- [x] 管理面后端模块：`platform-gateway-management`，管理 API 已从 `platform-gateway-runtime` 拆出。
- [x] 管理面后端服务：`platform-gateway-admin-server`，管理 API 可独立于网关数据面启动。
- [x] 独立管理台前端模块：`gatepilot-console`。
- [x] 管理台前端已按前后端分离调用 REST/JSON API，覆盖总览、路由、诊断、审计、配置治理、版本和发布事件骨架。

### 3.5 灰度与发布

- [x] 全局流量染色：`platform.gateway.traffic-color.*`。
- [x] 染色来源支持 header、cookie、query。
- [x] 匹配策略支持 exact、prefix、contains、regex。
- [x] 支持是否信任请求自带颜色头：`trust-request-header`。
- [x] 默认颜色：`stable`。
- [x] 响应头回写流量颜色，默认头为 `X-Traffic-Color`。
- [x] 路由级发布变体：`release.variants.*`。
- [x] 发布变体按流量颜色优先命中，未命中时回落默认上游。
- [x] 发布变体可覆盖 service URI、service path prefix、actuator URI、连接超时、响应超时。
- [x] 发布变体支持 `weight` 权重灰度，无显式颜色时按稳定哈希分流。
- [x] 权重灰度分流键支持 `release.weight-hash-headers`，默认按 `X-User-Id`、`X-Tenant-Id`、`X-Trace-Id` 查找，缺失时回落客户端地址。
- [x] 路由目录端点暴露 trafficColor 和 releaseVariants。

### 3.6 文档与测试

- [x] README 已说明项目定位、路由模型、企业级能力、配置说明、灰度发布和构建测试。
- [x] 接入手册已覆盖接入前准备、最小配置、联调命令、排障、上线检查清单。
- [x] 单元测试覆盖路由编译、配置校验、认证、方法白名单、internal 访问保护、路由目录、流量染色、熔断、审计、管理配置治理、Sentinel 规则注册等关键路径。

## 4. 当前边界

这些不是 bug，而是当前版本尚未建设完成的生产级能力边界：

1. 熔断已具备基础 fallback、失败率、慢调用和半开探测，隔离、实例级参数下发和运行期状态导出还需要继续增强。
2. `/internal/**` 当前基于回环地址保护，还没有完整的可信代理链和真实客户端 IP 解析。
3. 权重灰度的客户端地址兜底仍使用连接地址，后续应接入可信真实 IP 解析。
4. 访问审计已具备最近事件查询和结构化日志，但长期存储、采样、脱敏、Trace 关联和上游耗时还未补齐。
5. 管理面已有内部配置导出、dry-run 校验、diff、内存版本快照和发布事件记录，但还没有动态发布、持久化版本库、发布审批和回滚能力。
6. 配置仍以静态配置为主，没有接 Nacos、Apollo 或 DB 动态刷新。
7. 当前 Web 管理界面已有独立前端模块骨架，但还没有生产级 RBAC、指标大盘、告警中心和动态发布闭环。

## 5. 后续建设路线

### P0：生产兜底能力

P0 的目标是让网关能更稳地承接真实生产流量。

1. 路由级熔断精细化治理
   - 在已有 `governance.circuit-breaker.*` 基础上继续补隔离策略、实例级参数下发和运行期状态导出。
   - 增强半开探测观测，暴露熔断打开、半开、关闭状态变化事件。
   - 继续保持路由级 fallback JSON 和可选 fallback URI。
   - 在 `platformGatewayRoutes` 暴露完整生效策略。

2. 真实客户端 IP 信任链
   - 新增 `real-ip.*` 或 `trusted-proxies.*` 配置。
   - 支持 `X-Forwarded-For`、`X-Real-IP`、Forwarded header。
   - 只信任配置内代理追加的转发头，避免客户端伪造真实 IP。
   - 统一给流控、审计、灰度分流使用同一个 resolved client IP。

3. 访问审计日志
   - 在已有最近事件查询和结构化日志基础上，接入日志平台或持久化存储。
   - 补充认证结果、retry result、upstream status 和 upstream latency。
   - 支持采样、敏感字段脱敏和 Trace 关联检索。
   - 给后续告警、管理台排障和审计报表留出稳定字段。

4. 路由级指标增强
   - 按 project、route、variant、status、exception 维度输出指标。
   - 记录上游耗时、网关处理耗时、重试次数、流控拦截次数、fallback 次数。
   - 明确 Prometheus 指标名和标签基数边界。

### P1：发布与运维效率

P1 的目标是让配置变更、灰度发布和排障更可控。

1. 配置导出、校验和差异 API
   - 增加只读导出接口，返回当前生效配置和编译后的路由定义。
   - 增加配置 dry-run 校验接口，输入候选配置并返回校验结果。
   - 增加配置 diff 能力，展示生效路由、上游、治理策略、发布变体的变化。

2. 动态配置刷新
   - 先抽象配置来源接口，再接 Nacos / Apollo / DB。
   - 刷新流程必须包含校验、编译、冲突检测、路由替换、观测事件。
   - 刷新失败时保持旧配置继续服务。
   - 记录每次配置版本、操作者、变更摘要和回滚点。

3. 发布变体操作模型
   - 支持变体启停、权重调整、默认上游切换。
   - 支持蓝绿切换前健康检查。
   - 支持一键回滚到上一版本配置。
   - 输出发布事件，便于审计。

4. 运维 Runbook
   - 补充生产部署、扩容、回滚、健康检查、常见告警处理。
   - 补充 Sentinel 开启、Dashboard 接入和规则验证步骤。
   - 补充接入 K8s / Nginx / SLB 时的真实 IP 和 header 约定。

5. 管理界面后端 API
   - 总览大盘 API：请求量、成功率、错误率、P95/P99、限流、熔断、fallback、上游健康。
   - 路由诊断 API：输入 method、path、headers、query，返回路由命中、认证、染色、变体、限流 key 和熔断策略。
   - Trace 排障 API：按 traceId 查询网关处理链路和上游响应。
   - 审计查询 API：按 project、route、clientIp、status、trafficColor、releaseVariant 检索访问记录。
   - 配置治理 API：已有导出、dry-run、diff、内存版本快照和发布事件，后续补发布、回滚和持久化版本库。

### P2：平台化扩展

P2 的目标是让网关从单一发布件逐步变成平台能力。

1. 管理面
   - 在独立 `gatepilot-console` 模块建设 Web 管理界面，不能只做静态展示页。
   - 前端不打进 `platform-gateway-server`，后端不做服务端模板渲染，保持前后端分离。
   - 管理界面必须覆盖配置查看、校验、发布、回滚、健康、指标、审计、Trace 排障、请求模拟、灰度蓝绿操作和告警事件。
   - 管理界面所有写操作必须走 RBAC、发布确认和操作审计。

2. 多环境和多集群支持
   - 支持按环境、地域、集群导出和对比配置。
   - 支持跨集群健康汇总和发布状态汇总。

3. 插件化治理
   - 把认证、鉴权、限流、审计、流量染色等能力抽成更明确的扩展点。
   - 允许项目按 route 挂载有限的定制策略，但不破坏统一入口模型。

4. 安全增强
   - 增加安全响应头、IP 黑白名单、基础 WAF 规则或与外部 WAF 联动。
   - 管理接口增加 RBAC 和操作审计。

## 6. 推荐下一批提交

建议按下面顺序推进，每个提交都保持配置、校验、实现、测试、文档同步：

1. `feat: resolve trusted client ip`
   - 为流控、审计、权重灰度提供可信客户端身份。

2. `feat: persist gateway audit events`
   - 把最近事件查询升级为可检索的长期审计存储，方便上线排障和审计回溯。

3. `feat: persist gateway config versions`
   - 把内存版本快照升级为可持久化版本库，为动态刷新、发布审批和回滚准备稳定版本模型。

4. `feat: add gateway management console`
   - 建设能用于日常排障的 Web 管理界面，而不是只读配置页面。

## 7. 交付标准

后续每个生产能力都按同一套标准验收：

1. 配置模型清楚，字段名稳定，默认值保守。
2. 启动期有 fail-fast 校验，错误消息能定位到 project / route。
3. 运行期行为能通过 Actuator 或指标观察。
4. 单元测试覆盖正常路径、关闭路径、非法配置和关键边界。
5. README 或接入手册同步更新示例。
6. 网关直接返回的 JSON 响应都明确 UTF-8。
7. 不引入历史路径别名，不破坏 `/api/{project}/{route}/**` 和 `/internal/{project}/{route}/**` 的统一入口模型。

## 8. 暂不做的事情

1. 暂不为历史路径做兼容别名。
2. 暂不为每个业务项目拆独立网关。
3. 暂不把业务规则写进网关代码，项目差异优先通过 route 配置表达。
4. 暂不让客户端直接决定灰度命中，除非显式开启可信 header 或处在受控内网链路。
