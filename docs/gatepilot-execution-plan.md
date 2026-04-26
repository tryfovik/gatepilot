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
- 禁止硬编码业务路径、Header、label、reason、配置 key 和默认值；常量按上下文收敛到明确命名的常量类，不能建全局垃圾常量包。
- 对外 HTTP 入参、出参类统一命名为 `Request` / `Response`；`Command` 只用于明确的写入意图或领域命令，查询类不能误命名为 Command；新 Controller 出参不再使用泛化 `Result` 命名。
- 后端服务间 RPC 统一优先走 getboot-rpc / Dubbo，禁止 GatePilot 新增 OpenFeign；console REST、agent 配置同步、proxy HTTP 转发要按各自通道归类，不能混成普通跨服务 RPC。

目标模块：

```text
gatepilot-domain
gatepilot-apiserver
gatepilot-controller-manager
gatepilot-agent
gatepilot-proxy
gatepilot-console
gatepilot-embedded
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

通信能力新增时也按同一条规则处理：后端服务间 RPC 先查 getboot-rpc / Dubbo；OpenFeign 不作为 GatePilot 的跨服务通信选项；agent 配置同步通道可以 HTTP pull / long polling / Nacos watch，但必须显式归入同步适配器，不允许伪装成业务 RPC。

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
- [x] 写入关系型数据库访问必须基于 getboot-database、MyBatis-Plus 和 mapper.xml 的规则。
- [x] 写入并执行 GatePilot Java 展开式 Javadoc 注释格式。
- [x] 写入方法内中文注释规则：注释要短、核心、像人写，末尾不加句号。
- [x] 写入硬编码禁止规则：路径、Header、label、reason、配置 key、默认值必须进入上下文常量类。
- [x] 写入 HTTP 入参出参命名规则：Request / Response，Command 只表示写入意图。
- [x] 写入服务通信规则：后端服务间 RPC 优先 Dubbo / getboot-rpc，禁止 OpenFeign。
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
- [x] 建立 `gatepilot-embedded` 单 JVM 适配模块骨架。
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
- [x] 基于数据库实现跨副本乐观锁写入保护。
- [x] 将数据库资源存储改为 getboot-database + MyBatis-Plus Mapper 实现，复杂查询收敛到 mapper.xml。
- [x] 按 GatePilot 模型重建配置查看能力到 `gatepilot-apiserver`。
- [x] 按 GatePilot 模型重建配置 dry-run 校验到 `gatepilot-apiserver`。
- [x] 按 GatePilot 模型重建配置 diff 到 `gatepilot-apiserver`。
- [x] 按 GatePilot 模型重建版本快照到 `gatepilot-apiserver`。
- [x] 增加发布请求 API。
- [x] 增加回滚请求 API。
- [x] 增加 agent 注册、心跳、配置拉取和状态上报 API。
- [x] 增加 agent 运行审计上报、apiserver 持久化和查询 API。

### Phase 4：controller-manager

- [x] 实现发布 reconcile 骨架。
- [x] 根据资源生成 `PublishedConfig`。
- [x] 汇总 agent apply result。
- [x] 汇总节点发布状态。
- [x] 记录发布事件。
- [x] 建立 controller-manager 的资源读取、发布产物写入和事件写回 `domain.port`。
- [x] 建立 embedded 到 controller-manager `domain.port` 的进程内资源存储适配器。
- [x] 实现发布意图 / GatewayEvent 到 PublishedConfig 的异步推进服务。
- [x] 支持失败回滚编排。
- [x] controller-manager 调度批次入口接入 getboot-lock 分布式锁。

### Phase 5：agent

- [x] 实现节点注册。
- [x] 实现心跳上报。
- [x] 实现 `PublishedConfig` pull。
- [x] agent 访问 apiserver 使用 getboot-http-client 增强后的 WebClient，不手写 Trace Header。
- [ ] 将 agent 配置同步通道抽象为可替换 adapter，保留 HTTP pull / long polling / Nacos watch 方向，不引入 OpenFeign。
- [x] 预留 watch / long polling / SSE 扩展点。
- [x] 实现 staged config。
- [x] 实现 last-good config。
- [x] 实现调用本机 proxy apply。
- [x] 实现 apply result 上报。
- [x] 实现运行审计批量上报。
- [x] 实现节点健康、上游健康和指标摘要上报。
- [x] 实现 agent 生命周期调度，启动注册、last-good 启动、定时 pull 和定时心跳。

### Phase 6：proxy

- [ ] 将旧数据面模块能力按 GatePilot 运行模型收敛到 `gatepilot-proxy`。
- [x] 移除 proxy 中的配置查看、版本快照、Web 管理、审计查询职责。
- [x] proxy 只从 agent 获取 `PublishedConfig`。
- [x] proxy 支持原子切换运行状态。
- [x] proxy 支持失败保留 last-good。
- [x] proxy 支持 `PublishedConfig` 预编译为运行态快照。
- [x] proxy 热路径运行态使用本地内存索引，不访问控制面。
- [x] 基于旧 `GatewayRouteDefinitionLocator` 的能力样本重建路由编译和命中算法。
- [x] 基于旧 `GatewayTrafficColorResolver` / `GatewayTrafficColorFilter` 的能力样本重建流量染色执行能力。
- [x] 参考旧 `GatewayAuthenticationFilter` 改造路由级认证策略判断能力。
- [x] 接入 getboot-auth 执行路由级认证。
- [x] 参考旧 `GatewayMethodAccessFilter` 改造 HTTP 方法白名单判断能力。
- [x] 接入 proxy WebFlux 过滤链执行 HTTP 方法白名单。
- [x] 接入 proxy WebFlux 过滤链执行路由转发。
- [x] 接入 proxy WebFlux 过滤链执行染色解析、请求头透传和响应头回写。
- [x] 接入 ReleasePolicy `trafficSplits`，按灰度 / 蓝绿命中的颜色切换实际上游。
- [x] 接入上游多端点轮询和加权轮询选择，避免所有流量固定打第一个 endpoint。
- [x] 改造 Query / IP 染色规则。
- [x] 接入 TrafficPolicy `retry` 执行幂等请求重试，重试时重新选择上游端点。
- [x] 参考旧 `GatewayCircuitBreakerFilter` 改造熔断和 fallback 能力。
- [x] 接入 getboot-limiter 执行基础路由级 / 参数级限流。
- [ ] 参考旧 `GatewaySentinelRuleRegistrar` 改造 Sentinel 规则注册能力。
- [x] 参考旧审计过滤器改造访问审计采集能力。
- [x] 改造内部运维入口保护。
- [x] 改造上游健康主动探测。
- [x] 保留并验证路由、转发、限流、熔断、重试、染色、灰度、蓝绿执行能力。
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

- [x] 参考旧 `GatewayDiagnosticsService` 改造路由诊断能力到 apiserver 查询用例和 console 页面。
- [ ] 参考旧 `GatewayManagementService` 改造 dry-run、diff、配置摘要能力到 apiserver / controller-manager。
- [ ] 参考旧 `GatewayConfigSnapshotRepository` 改造快照概念到数据库持久化版本表。
- [x] 参考旧 `GatewayAccessAuditController` 改造审计查询能力到 apiserver 持久化查询 API。
- [x] 参考旧 `GatewayRouteCatalogEndpoint` 改造路由目录展示到 console，不再依赖 Actuator 私有端点。
- [ ] 所有旧能力完成新模型覆盖和测试后，物理删除历史模块源码。

### Phase 8：app 合包

- [x] 将单 JVM 进程内适配器从 app / apiserver 拆到 `gatepilot-embedded`。
- [x] 建立 `gatepilot-app` 启动入口。
- [x] 装配 apiserver。
- [x] 装配 controller-manager。
- [x] 装配 agent。
- [x] 装配 proxy。
- [x] 承载 console 静态资源。
- [x] 确认 app 中没有业务实现代码。
- [x] 单体模式装配 proxy 运行审计到 agent 上报链路。
- [x] 支持单体大包运行。
- [x] 保留分服务部署能力。
- [x] embedded 只跟随 `gatepilot.mode=standalone` 自动装配，不再提供第二开关。

### Phase 9：验证与发布质量

- [x] 全量 `mvn -q test` 通过。
- [x] 前端脚本和构建校验通过。
- [x] `git diff --check` 通过。
- [x] 单体模式启动验证通过。
- [x] apiserver Controller 返回协议使用 getboot `ApiResponse` 的测试覆盖。
- [x] agent apiserver 客户端复用 getboot-http-client 增强后的 `WebClient.Builder`，不手写 Trace Header 的测试覆盖。
- [x] app 单体模式通过 getboot-observability 回写 `X-Trace-Id` 的集成测试覆盖。
- [x] embedded 单体、集群和未配置部署模式的装配切换测试覆盖。
- [x] 发布意图到 `PublishedConfig`、配置快照、agent pull 的最小链路测试通过。
- [x] MyBatis-Plus 资源存储保存、更新、分页测试通过。
- [x] agent / apiserver / app 运行审计上报、持久化、查询和进程内桥接测试通过。
- [x] apiserver 不再依赖 controller-manager，app 不再承载进程内适配实现。
- [ ] 分服务模式最小链路验证通过。
- [ ] 配置发布链路端到端验证通过。
- [x] proxy 控制面不可用时 last-good 启动验证通过。
- [ ] 多 proxy 副本注册、拉取配置、应用发布和状态聚合验证通过。
- [ ] controller-manager 多副本 leader / standby 行为验证通过。
- [ ] 1000 项目资源装载、配置生成和分页查询压测通过。
- [ ] 大路由表预编译和 proxy 原子切换压测通过。
- [ ] configShard / isolationGroup 分片发布验证通过。
- [ ] 高流量项目通过扩 proxy 副本承载，不修改转发代码。
- [ ] Kubernetes 基础部署验证通过：VIP / Nginx 入口不承载 GatePilot 项目治理，proxy Deployment + Service + HPA 可扩副本。
- [x] agent 从网关配置读取 namespace、nodeId、zone、isolationGroup、configShards 并注册为 `GatewayNode`。
- [ ] controller-manager 多副本通过 Kubernetes Lease 或等价机制完成 leader / standby 验证。

### Phase 9.5：架构债与 CR 待办

- [ ] CR controller-manager 分布式锁与发布事件 claim 的双保险语义，确认多副本下不会重复推进、不会长时间饿死发布队列，且锁实现缺失时不能静默退化成无锁生产运行。
- [ ] CR 回滚 PublishedConfig 复制策略，确认快照内容不会被后续发布污染，必要时改成 ObjectMapper 深拷贝或不可变快照。
- [ ] CR embedded 资源读取的 500 条上限扫描，改成按 label / namespace / cursor 精准分页，避免 1000 项目后 reconcile 漏数据。
- [ ] CR 发布版本号生成策略，评估是否接入 getboot 统一 ID 能力或单独版本序列，避免继续依赖本地时间。
- [ ] CR MyBatis-Plus 资源表索引、乐观锁和发布事件 claim 原子性，避免多 controller-manager 抢占时只靠内存判断。
- [x] CR agent last-good 存储当前仍是内存实现的问题，补文件或外部卷持久化，保证 proxy 控制面不可用时可恢复启动。
- [ ] CR proxy 运行态策略解析中的 Map 兼容逻辑，确认大配置下没有反射/转换热点拖慢转发路径。

## 5. 打勾规则

- 完成一个 checklist 项，必须在同一轮改动中把 `[ ]` 改成 `[x]`。
- 如果任务拆细，先在本文档补子项，再开始实现。
- 如果发现模块边界不合理，先更新 [GatePilot 架构边界规范](architecture-boundaries.md)，再改代码。
- 如果 Console 页面需要突破现有风格，先更新 [GatePilot Console 设计规范](console-design-guidelines.md)，再改页面。
