# GatePilot 执行计划

更新时间：2026-04-25

本文档把本轮已经确认的 GatePilot 架构、Console 设计约束和后续开发计划集中到一个地方。后续我每完成一个阶段或任务，都必须同步更新这里的 checklist。

## 1. 已确认方向

项目名称：`GatePilot`

定位：GatePilot 是网关控制与运行系统，支持单体大包部署，也支持控制面、节点代理、数据面分服务部署。

核心原则：

- 部署形态可以变化，代码职责边界不能变化。
- 不能再使用 `core`、`common`、`shared`、泛化 `runtime` 这类容易变成垃圾包的命名。
- GatePilot 是网关产品本身，内部按 DDD 的 `interfaces / application / domain / infrastructure` 分层。
- getboot 这类公共 starter 才使用 `api / spi / support / infrastructure` 组织对外契约和扩展点，GatePilot 模块内禁止照搬这套包结构。
- 最终大包只能是装配包，不能承载业务实现。
- proxy 只消费已发布配置，不消费草稿配置。
- console 只调用 apiserver，不直接调用 proxy。
- agent 负责节点注册、配置同步、last-good 和状态上报，不做发布决策。
- 公共能力先查 getboot；getboot 有就接入和配置，getboot 没有也不能在 GatePilot 临时补，必须先回 getboot 补能力，再让 GatePilot 依赖。
- Java 注释统一使用展开式 Javadoc，不新增 `/** xxx */` 这种单行 Javadoc。

目标模块：

```text
gatepilot-domain
gatepilot-apiserver
gatepilot-controller-manager
gatepilot-agent
gatepilot-proxy
gatepilot-console
gatepilot-app
```

后端包结构固定规则：

```text
interfaces      入站适配：REST Controller、参数校验、协议适配
application     应用用例：command、dto、查询、发布入口、跨领域编排
domain          领域规则：模型、领域服务、repository/port 等端口
infrastructure  出站实现：数据库、HTTP 客户端、Spring 配置、调度和适配器
```

禁止在 GatePilot 新增内部 `api`、`spi`、`support`、`common`、`core`、`shared` 包。用例入参出参放 `application.command` / `application.dto`；确实只属于对外 HTTP 或 Console 协议的适配对象，放 `interfaces`；确实是外部能力桥接，放 `domain.port`；确实是实现，放 `infrastructure`。

当前不规划 `gatepilot-client` 或业务应用 JVM agent。主路径是网关后台统一配置和发布，避免业务项目侧依赖 SDK 后形成第二个控制面。

## 2. 必守文档

后续开发必须先看这两份文档：

- [GatePilot 架构边界规范](architecture-boundaries.md)
- [GatePilot Console 设计规范](console-design-guidelines.md)

历史网关代码不是当前开发主线，也不是可以直接搬运的实现来源。里面的路由编译、过滤器、染色、熔断、审计、诊断、校验和 Sentinel 注册只能作为能力样本、算法参考、测试素材和工程经验。旧模块不参与 Maven 主构建，改造前必须先看 [旧能力改造清单](architecture-boundaries.md#12-旧能力改造清单)，以 GatePilot 资源模型、`PublishedConfig` 和 DDD 边界为准，禁止按旧模型、旧接口、旧包结构直接搬代码。新模型尚未覆盖的旧能力不能直接删，必须先完成改造和测试。

后端开发还必须参考上层 getboot 规范：

- `../../DEVELOPMENT.md`
- `../../docs/DDD_PACKAGE_RULES.md`
- `../../docs/MODULE_MAP.md`
- 对应 `getboot-*` 模块 README

新增后端能力时，先判断它属于资源、控制、同步、流量还是装配。

新增公共能力时，不直接写进 GatePilot：

```text
判断为公共能力 -> 查 getboot -> 找到则接入配置 -> 找不到也先补 getboot -> GatePilot 再依赖
```

这条没有临时例外。统一响应、异常、Trace、Header 透传、缓存、锁、限流、幂等、HTTP 客户端、指标、数据库访问等公共基础设施，即使 getboot 当前缺失，也要先回 getboot 建能力或扩展能力，GatePilot 只消费这些公共能力。

新增前端页面时，先写页面设计说明，明确参考 Kong Konnect、Kubernetes Dashboard、Argo CD 或 Grafana 的哪类页面结构。

## 3. 发布链路

固定链路采用控制器模型，不采用 apiserver 同步调用整条发布链路的方式：

```text
console
  -> apiserver admission 校验并保存声明式资源
  -> apiserver 创建 ReleaseRequest / GatewayEvent 等发布意图资源
  -> controller-manager watch / list / claim 发布意图
  -> controller-manager 读取 apiserver 中的期望状态
  -> controller-manager reconcile 并生成 PublishedConfig
  -> controller-manager 将 PublishedConfig、Snapshot、Event、Status 写回 apiserver
  -> agent watch / pull apiserver 中的 PublishedConfig
  -> agent 校验 hash / 兼容性并写入 staged config
  -> agent 通知本机 proxy apply
  -> proxy 原子切换本地运行状态
  -> agent 上报 apply result 到 apiserver
  -> controller-manager 汇总节点状态并回写发布状态
  -> console 只查询 apiserver 展示结果
```

关键约束：

- `PublishedConfig` 是数据面唯一配置入口。
- `apiserver` 不做发布推进，不同步调用 agent 或 proxy。
- `proxy` 不存配置版本。
- `agent` 不决定发布策略。
- `controller-manager` 负责发布推进，但不对外提供查询 API。
- `apiserver` 负责存储、权限、审计和查询。
- 分服务部署时，controller-manager 只能通过 apiserver API 消费和回写资源；单体合包时可以共用同一进程内的资源存储适配器，但职责边界不能改变。

## 4. 执行 Checklist

### Phase 0：架构护栏

- [x] 确认项目名为 GatePilot。
- [x] 写入 GatePilot 架构边界规范。
- [x] 写入 GatePilot Console 设计规范。
- [x] 写入 GatePilot 执行计划文档。
- [x] 补充后端开发必须参考 getboot 开发规范。
- [x] 写入公共能力必须先查 getboot、缺失也必须先补 getboot 的硬性规则。
- [x] 对齐 getboot `ApiResponse`、`X-Trace-Id`、MDC `traceId` 和 HTTP 出站透传约定。
- [x] 写入并执行 GatePilot Java 展开式 Javadoc 注释格式。
- [x] 清理旧模块收敛过程中的半成品状态，保证工作区重新回到可编译、可测试状态。
- [x] 将 GatePilot 模块包结构调整为 DDD 分层，移除内部 `api / spi / support` 包口径。
- [x] 从父级 Maven reactor 摘掉历史模块，主构建只保留 GatePilot 新模块。
- [x] 建立旧能力退役对账规则，未被新模型覆盖的旧能力不得物理删除。
- [x] 将资源元模型包从 `resource.common` 收敛为 `resource.meta`，避免 `common` 变成垃圾包入口。

### Phase 1：目标模块骨架

- [x] 建立 `gatepilot-domain` 模块骨架。
- [x] 建立 `gatepilot-apiserver` 模块骨架。
- [x] 建立 `gatepilot-controller-manager` 模块骨架。
- [x] 建立 `gatepilot-agent` 模块骨架。
- [x] 建立 `gatepilot-proxy` 模块骨架。
- [x] 建立 `gatepilot-console` 模块骨架。
- [x] 建立 `gatepilot-app` 装配模块骨架。
- [x] 删除或收敛历史临时 core 命名，避免形成新的垃圾包。

### Phase 2：资源模型

- [x] 定义 `metadata / spec / status` 基础资源结构。
- [x] 定义 `GatewayProject`。
- [x] 定义 `GatewayRoute`。
- [x] 定义 `TrafficPolicy`。
- [x] 定义 `ReleasePolicy`。
- [x] 定义 `AuthPolicy`。
- [x] 定义 `Upstream`。
- [x] 定义 `PublishedConfig`。
- [x] 定义 `GatewayNode`。
- [x] 定义 `GatewayNodeStatus`。
- [x] 定义 `GatewayEvent`。

### Phase 3：apiserver

- [x] 建立 `interfaces / application / domain / infrastructure` 分层骨架。
- [x] 建立声明式资源 CRUD API 骨架。
- [x] 建立资源列表游标分页约束，避免大规模资源一次性返回。
- [x] 按 GatePilot 模型重建配置存储端口到 `gatepilot-apiserver`。
- [x] 实现基于数据库的 `GatePilotResourceStore` 生产存储。
- [x] 基于数据库实现资源 generation 递增、索引分页和审计时间字段。
- [ ] 基于数据库实现跨副本乐观锁写入保护。
- [x] 按 GatePilot 模型重建配置查看能力到 `gatepilot-apiserver`。
- [x] 按 GatePilot 模型重建配置 dry-run 校验到 `gatepilot-apiserver`。
- [x] 按 GatePilot 模型重建配置 diff 到 `gatepilot-apiserver`。
- [x] 按 GatePilot 模型重建版本快照到 `gatepilot-apiserver`。
- [x] 增加发布请求 API。
- [x] 增加回滚请求 API。
- [x] 增加 agent 注册、心跳、配置拉取和状态上报 API。

### Phase 4：controller-manager

- [x] 实现发布 reconcile 骨架。
- [x] 根据资源生成 `PublishedConfig`。
- [x] 汇总 agent apply result。
- [x] 汇总节点发布状态。
- [x] 记录发布事件。
- [x] 建立 controller-manager 的资源读取、发布产物写入和事件写回 `domain.port`。
- [x] 建立 apiserver 到 controller-manager `domain.port` 的资源存储适配器。
- [x] 实现发布意图 / GatewayEvent 到 PublishedConfig 的异步推进服务。
- [ ] 支持失败回滚编排。

### Phase 5：agent

- [x] 实现节点注册。
- [x] 实现心跳上报。
- [x] 实现 `PublishedConfig` pull。
- [x] agent 访问 apiserver 使用 getboot-http-client 增强后的 WebClient，不手写 Trace Header。
- [x] 预留 watch / long polling / SSE 扩展点。
- [x] 实现 staged config。
- [x] 实现 last-good config。
- [x] 实现调用本机 proxy apply。
- [x] 实现 apply result 上报。
- [x] 实现节点健康、上游健康和指标摘要上报。

### Phase 6：proxy

- [ ] 将旧数据面模块能力按 GatePilot 运行模型收敛到 `gatepilot-proxy`。
- [ ] 移除 proxy 中的配置查看、版本快照、Web 管理、审计查询职责。
- [ ] proxy 只从 agent 获取 `PublishedConfig`。
- [x] proxy 支持原子切换运行状态。
- [x] proxy 支持失败保留 last-good。
- [x] proxy 支持 `PublishedConfig` 预编译为运行态快照。
- [x] proxy 热路径运行态使用本地内存索引，不访问控制面。
- [x] 基于旧 `GatewayRouteDefinitionLocator` 的能力样本重建路由编译和命中算法。
- [x] 基于旧 `GatewayTrafficColorResolver` / `GatewayTrafficColorFilter` 的能力样本重建流量染色执行能力。
- [x] 参考旧 `GatewayAuthenticationFilter` 改造路由级认证策略判断能力。
- [ ] 接入 getboot-auth 执行路由级认证。
- [x] 参考旧 `GatewayMethodAccessFilter` 改造 HTTP 方法白名单判断能力。
- [ ] 接入 proxy WebFlux 过滤链执行 HTTP 方法白名单。
- [ ] 接入 proxy WebFlux 过滤链执行路由转发。
- [ ] 接入 proxy WebFlux 过滤链执行染色解析、请求头透传和响应头回写。
- [ ] 改造 Query / IP 染色规则。
- [ ] 参考旧 `GatewayCircuitBreakerFilter` 改造熔断和 fallback 能力。
- [ ] 参考旧 `GatewaySentinelRuleRegistrar` 改造 Sentinel 规则注册能力。
- [ ] 参考旧审计过滤器改造访问审计采集能力。
- [ ] 改造内部运维入口保护。
- [ ] 改造上游健康主动探测。
- [ ] 保留并验证路由、转发、限流、熔断、重试、染色、灰度、蓝绿执行能力。
- [x] 运行审计事件只采集并上报，不在 proxy 内做管理查询。

### Phase 7：console

- [x] 按 Console 设计规范重整现有前端模块。
- [x] Console 默认中文优先，集中管理导航、按钮、状态、错误提示和确认弹窗文案。
- [x] Console 技术栈确定为 Vue 3 + Vite + TypeScript。
- [ ] 建立统一导航、表格、详情页、状态 badge、diff、timeline、confirm dialog。
- [x] 建立统一导航、基础表格和状态 badge。
- [x] 实现 Overview。
- [x] 实现 Projects 基础列表页。
- [x] 实现 Routes 基础列表页。
- [x] 实现 Policies 基础列表页。
- [x] 实现 Releases 基础列表页。
- [x] 实现 Config Snapshots 与版本 Diff 页面。
- [x] 实现 Nodes 基础列表页。
- [x] 实现 Nodes 健康、指标、上游健康专用列表页。
- [x] 实现 Upstreams 基础列表页。
- [x] 实现 Diagnostics 基础列表页。
- [x] 实现 Audits 基础列表页。
- [x] 实现 Settings 基础列表页。

### Phase 7.5：旧管理能力改造

- [ ] 参考旧 `GatewayDiagnosticsService` 改造路由诊断能力到 apiserver 查询用例和 console 页面。
- [ ] 参考旧 `GatewayManagementService` 改造 dry-run、diff、配置摘要能力到 apiserver / controller-manager。
- [ ] 参考旧 `GatewayConfigSnapshotRepository` 改造快照概念到数据库持久化版本表。
- [ ] 参考旧 `GatewayAccessAuditController` 改造审计查询能力到 apiserver 持久化查询 API。
- [ ] 参考旧 `GatewayRouteCatalogEndpoint` 改造路由目录展示到 console，不再依赖 Actuator 私有端点。
- [ ] 所有旧能力完成新模型覆盖和测试后，物理删除历史模块源码。

### Phase 8：app 合包

- [x] 建立 `gatepilot-app` 启动入口。
- [x] 装配 apiserver。
- [x] 装配 controller-manager。
- [x] 装配 agent。
- [x] 装配 proxy。
- [x] 承载 console 静态资源。
- [x] 确认 app 中没有业务实现代码。
- [x] 支持单体大包运行。
- [x] 保留分服务部署能力。

### Phase 9：验证与发布质量

- [x] 全量 `mvn -q test` 通过。
- [x] 前端脚本和构建校验通过。
- [x] `git diff --check` 通过。
- [x] 单体模式启动验证通过。
- [x] apiserver Controller 返回协议使用 getboot `ApiResponse` 的测试覆盖。
- [x] agent apiserver 客户端复用 getboot-http-client 增强后的 `WebClient.Builder`，不手写 Trace Header 的测试覆盖。
- [x] app 单体模式通过 getboot-observability 回写 `X-Trace-Id` 的集成测试覆盖。
- [x] 发布意图到 `PublishedConfig`、配置快照、agent pull 的最小链路测试通过。
- [x] JDBC 资源存储保存、更新、分页测试通过。
- [ ] 分服务模式最小链路验证通过。
- [ ] 配置发布链路端到端验证通过。
- [ ] proxy 控制面不可用时 last-good 启动验证通过。
- [ ] 多 proxy 副本注册、拉取配置、应用发布和状态聚合验证通过。
- [ ] controller-manager 多副本 leader / standby 行为验证通过。
- [ ] 1000 项目资源装载、配置生成和分页查询压测通过。
- [ ] 大路由表预编译和 proxy 原子切换压测通过。
- [ ] configShard / isolationGroup 分片发布验证通过。
- [ ] 高流量项目通过扩 proxy 副本承载，不修改转发代码。
- [ ] Kubernetes 基础部署验证通过：VIP / Nginx 入口不承载 GatePilot 项目治理，proxy Deployment + Service + HPA 可扩副本。
- [ ] agent 从 K8s 环境读取 podName、namespace、zone、nodeName、isolationGroup、configShards 并注册为 `GatewayNode`。
- [ ] controller-manager 多副本通过 Kubernetes Lease 或等价机制完成 leader / standby 验证。

## 5. 打勾规则

- 完成一个 checklist 项，必须在同一轮改动中把 `[ ]` 改成 `[x]`。
- 如果任务拆细，先在本文档补子项，再开始实现。
- 如果发现模块边界不合理，先更新 [GatePilot 架构边界规范](architecture-boundaries.md)，再改代码。
- 如果 Console 页面需要突破现有风格，先更新 [GatePilot Console 设计规范](console-design-guidelines.md)，再改页面。
