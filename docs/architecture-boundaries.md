# GatePilot 架构边界规范

更新时间：2026-04-26

GatePilot 是网关控制与运行系统。本文档用于约束后续开发的模块边界。GatePilot 可以支持单体大包部署，也可以支持控制面、节点代理、数据面分服务部署；部署形态可以变化，但代码职责边界不能变化。

## 1. 架构原则

GatePilot 采用接近 Kubernetes 的控制面 / 数据面思想：

1. 声明式资源：管理端保存期望状态，数据面只消费已发布状态。
2. 控制面负责管理：配置存储、查看、校验、版本、发布、回滚、权限和操作审计属于控制面。
3. 数据面负责流量：业务请求只进入数据面，数据面不编辑配置、不存配置版本、不提供管理页面。
4. 节点代理负责同步：agent 跟随 proxy 节点部署，负责配置同步、last-good 缓存、节点注册、心跳和状态上报。
5. 合包只做装配：最终大 jar 可以同时启动 apiserver、controller-manager、agent、proxy 和 console 静态资源，但 app 模块不能写业务实现代码。

Java 后端包结构采用 DDD 分层，不沿用 getboot starter 的 `api / spi / support / infrastructure` 分层。getboot 是公共能力和扩展点集合，`api` 表示对外稳定契约，`spi` 表示对外实现桥接；GatePilot 是网关产品本身，内部按业务上下文组织代码。

GatePilot 模块内固定使用：

- `interfaces`：入站适配层，例如 REST Controller、参数校验、协议适配和只属于 REST 的传输对象。
- `application`：应用层，用例编排、命令、查询、返回结果、发布流程入口和跨领域协调。
- `domain`：领域层，领域模型、领域规则、领域服务、仓储端口和外部能力端口。
- `infrastructure`：基础设施层，数据库实现、HTTP 客户端实现、Spring 配置、调度器、进程内适配器。

禁止在 GatePilot 新增内部 `api`、`spi`、`support`、`common`、`core`、`shared` 包。确实需要扩展点时，优先命名为 `domain.port`；用例入参出参优先放 `application.command` / `application.dto`；确实只属于对外 HTTP 或 Console 协议的适配对象，才放在 `interfaces`。如果一个能力看起来“哪里都能放”，先回到本文档判断它属于哪个业务上下文，而不是新建泛化包。

Java 代码注释格式：

- 类、字段、枚举项和公开方法使用展开式 Javadoc。
- 禁止新增单行 Javadoc，例如 `/** 发布版本。 */`。
- 方法内中文注释只写关键原因和关键转换，短一点，像人写，末尾不加句号。
- 禁止在业务代码里散落硬编码字符串、路径、Header、label、reason、配置 key 和默认值。
- 常量必须按上下文收敛到明确命名的常量类，例如资源路径放 apiserver 资源上下文，PublishedConfig 配置 key 放发布配置上下文，proxy HTTP Header 放 proxy HTTP 上下文。
- 禁止为了收常量新建 `common`、`shared`、`core` 这类垃圾包，也禁止把所有模块常量塞进一个全局大杂烩。
- 正确格式如下：

```java
/**
 * 发布版本。
 */
private String version;
```

## 2. 目标模块

目标模块按职责命名，避免出现 `core`、`common`、`runtime` 这类容易变成垃圾包的名字。

### gatepilot-domain

声明式资源模型模块。

只能放：

- 资源定义。
- 枚举。
- 资源 metadata / spec / status 等跨模块资源数据结构。

典型资源：

- `GatewayProject`
- `GatewayRoute`
- `TrafficPolicy`
- `ReleasePolicy`
- `AuthPolicy`
- `Upstream`
- `PublishedConfig`
- `GatewayNode`
- `GatewayNodeStatus`
- `GatewayEvent`

禁止放：

- Spring Controller。
- Service。
- Repository。
- 数据库访问。
- 网关转发逻辑。
- 配置发布流程。
- 页面接口。
- REST 请求响应 DTO。
- 应用层命令 DTO。

`gatepilot-domain` 不是公共工具包，也不是“什么都能放”的核心包。它只表达 GatePilot 的声明式资源长什么样；具体用例、端口、实现和传输协议必须放回各自上下文。

### gatepilot-apiserver

控制面 API 服务。

职责：

- 配置资源 CRUD。
- 配置查看。
- 配置合法性校验。
- 权限校验。
- 操作审计。
- 配置版本存储。
- 发布入口。
- 回滚入口。
- 给 console 提供 REST/JSON API。
- 给 agent 提供注册、心跳、配置 watch / pull、状态上报 API。
- 配置模板、部署向导和 values 渲染属于控制面能力，只能生成声明式资源或部署清单，不能把模板逻辑下沉到 agent / proxy。

存储边界：

- 生产环境配置、发布产物、快照、事件、审计和节点状态必须持久化到数据库。
- `GatePilotResourceStore` 是 apiserver 的资源存储端口。
- 内存版资源存储只能用于本地开发、单元测试和演示，不能作为生产实现。
- `gatepilot.apiserver.store.type=memory` 只允许开发测试使用。
- `gatepilot.apiserver.store.type=database` 使用 getboot-database 提供的数据源、MyBatis-Plus 和数据库增强能力，资源表结构参考 `gatepilot-apiserver/src/main/resources/db/gatepilot/schema-mysql.sql`。
- 后续数据库访问、事务、分页、乐观锁和审计字段优先复用 getboot 数据访问规范和能力；如果 getboot 缺能力，先回 getboot 补，再让 GatePilot 接入。
- GatePilot 关系型数据库访问必须基于 getboot-database 接入 MyBatis-Plus；单表 CRUD 优先用 Mapper / BaseMapper，复杂 SQL 必须放在 mapper.xml，禁止在业务代码里用 JdbcTemplate 或字符串拼接 SQL。
- controller-manager、agent、proxy 都不能直接访问 GatePilot 配置数据库，只能通过 apiserver API 或嵌入式适配器访问资源。

发布边界：

- apiserver 只接收、校验、保存发布意图资源。
- apiserver 可以做 admission 校验，例如必填字段、资源是否存在、基础引用关系是否成立。
- apiserver 不负责发布推进、灰度步骤推进、蓝绿切换决策和自动回滚决策。
- apiserver 不直接调用 agent 或 proxy。
- apiserver 对 console 提供查询 API，但查询结果来自资源存储中的 `PublishedConfig`、快照、事件和状态。

公共能力约束：

- HTTP 统一响应使用 `getboot-web` 的 `ApiResponse`。
- Trace 入口、MDC、响应头回写和 Reactor 上下文传播使用 `getboot-observability`。
- agent / apiserver 之间的 HTTP 出站透传使用 `getboot-http-client`，不手写 Trace Header 注入。
- 通用异常、响应、Trace、Web 规范优先复用 getboot 已有能力。
- 不在 apiserver 内自造通用响应封装、通用异常体系或通用 Web 基础设施。
- GatePilot 只定义自身领域 DTO，例如资源游标分页 `CursorPageResponse`。

公共能力新增流程：

1. 先判断能力是不是网关领域能力。路由、发布、配置同步、proxy apply、流量执行属于 GatePilot；统一响应、异常、Trace、Header 透传、缓存、锁、限流、幂等、HTTP 客户端、指标、数据库访问这类属于公共能力。
2. 如果是公共能力，必须先去 getboot 现有模块查找，优先看 `../../docs/MODULE_MAP.md` 和对应模块 README。
3. getboot 已经有的，GatePilot 只能接入和配置，不能复制一份实现。
4. getboot 没有的，不能在 GatePilot 临时补一份；必须先回 getboot 新增或扩展公共能力，再由 GatePilot 依赖它。
5. 如果 getboot 里找不到对应模块，也仍然先去 getboot 建模块或扩展模块，不允许绕回 GatePilot 自造公共基础设施。
6. 只有确认能力是 GatePilot 专属领域能力，才允许写在 `gatepilot-*` 模块内。
7. 任何新增 `common`、`core`、`shared` 或自造公共基础设施的改动，都必须先改本文档说明原因，否则不允许落代码。

公共能力缺失处理红线：

```text
判断为公共能力
  -> 查 getboot
  -> getboot 有：GatePilot 接入和配置
  -> getboot 没有：先补 getboot，再回 GatePilot 接入
  -> 禁止：因为 getboot 暂时没有，就在 GatePilot 内自造一份
```

这条规则没有“临时例外”。如果为了赶进度必须先落地，也应该先在 getboot 形成最小公共能力，再让 GatePilot 依赖；否则后续会把 GatePilot 拖成基础设施垃圾包。

成熟组件优先级：

| 能力类型 | 首选来源 | GatePilot 允许做的事 | 禁止做的事 |
| --- | --- | --- | --- |
| HTTP 数据面转发 | Spring Cloud Gateway | 路由命中、请求改写、策略适配 | 自己维护 WebClient 转发器 |
| 上游负载均衡 | Spring Cloud LoadBalancer | `PublishedConfig` 到 `ServiceInstance` 的适配 | 自己维护轮询、随机、加权等通用算法 |
| 重试 | Spring Cloud Gateway Retry | 把 `TrafficPolicy.retry` 映射成 SCG 配置 | 自己写通用重试执行器 |
| 限流 | getboot-limiter | 把路由策略映射成限流规则 | 自己写本地或 Redis 限流算法 |
| 认证 | getboot-auth | 做路由级策略判断和结果适配 | 自己实现认证框架 |
| Trace / HTTP 客户端 | getboot-observability / getboot-http-client | 使用增强后的 Spring Bean | 手写 Trace Header 透传 |
| 数据访问 | getboot-datasource / MyBatis-Plus | 写实体、Mapper、Service 和必要 mapper.xml | 手写 JDBC SQL 作为默认实现 |
| 分布式互斥 | getboot-lock | 在调度入口声明锁语义 | 自己用数据库字段或内存锁冒充分布式锁 |

GatePilot 默认 Trace 约定：

```yaml
getboot:
  observability:
    trace:
      enabled: true
      header-name: X-Trace-Id
      request-header-propagation-enabled: true
      response-header-enabled: true
      mdc-key: traceId
```

`tid` 的传递链路固定为：入口请求头 `X-Trace-Id` -> `getboot-observability` 解析或生成 TraceId -> 写入 `TraceContextHolder` 和 MDC `traceId` -> 通过 `getboot-http-client` 写入 agent / apiserver 出站请求头 -> 下游服务继续沿用同一个 `X-Trace-Id`。

agent 访问 apiserver 时必须使用 Spring 管理的 `WebClient.Builder` 或 getboot 已增强过的 HTTP 客户端。禁止在 agent 客户端里手工拼接 `X-Trace-Id`，避免和 getboot Trace 规则分叉。

GatePilot 服务通信边界：

- console 到 apiserver 是浏览器访问控制面，固定使用 HTTP / JSON REST API。
- 后端服务之间的 RPC 优先使用 getboot-rpc / Dubbo，包括 apiserver 与 controller-manager 未来分服务部署时的内部接口。
- GatePilot 后端模块禁止新增 OpenFeign 依赖，禁止用 OpenFeign 作为默认跨服务通信方式。
- agent 拉取配置、上报心跳和 apply result 属于节点配置同步通道，不属于普通业务 RPC；当前可通过 apiserver HTTP pull 实现，后续可以演进为 Nacos / long polling / watch 适配，但不能改成 OpenFeign。
- proxy 转发业务 HTTP 流量时复用 Spring Cloud Gateway 的 RouteLocator、GlobalFilter 和 NettyRoutingFilter，这不是 GatePilot 服务间 RPC，不能为了统一 RPC 把业务转发改成 Dubbo，也不能回到手写 WebClient 转发。
- 如果新增跨服务调用能力，先查 getboot-rpc；getboot-rpc 缺能力时先补 getboot-rpc，再回 GatePilot 接入。

禁止放：

- 业务流量转发。
- Spring Cloud Gateway 路由注册。
- Sentinel 运行规则注册。
- proxy filter。
- 前端页面业务逻辑。

### gatepilot-controller-manager

控制器集合，负责把期望状态推进为已发布状态。

职责：

- watch / list apiserver 中的资源变化。
- claim 发布意图，避免多副本 controller-manager 重复推进同一次发布。
- reconcile `GatewayProject`、`GatewayRoute`、`TrafficPolicy`、`ReleasePolicy` 等资源。
- 生成 `PublishedConfig`。
- 将 `PublishedConfig`、快照、事件和状态写回 apiserver。
- 管理发布计划、灰度计划、蓝绿切换和回滚计划。
- 汇总 agent / proxy 节点应用结果。
- 汇总节点健康和发布状态。
- 生成发布事件。

禁止放：

- 配置 CRUD 页面接口。
- 面向 console 的查询接口。
- 资源存储实现。
- 业务流量转发。
- 具体 proxy filter 实现。
- 前端逻辑。

controller-manager 与 apiserver 的关系：

- 分服务部署时，controller-manager 通过 apiserver API 读写资源。
- 单体合包部署时，controller-manager 可以使用进程内适配器读写同一份资源存储。
- 无论哪种部署方式，controller-manager 应用层只能依赖自身 `domain.port`，不反向依赖 apiserver 的 Controller 或 Web 层。
- 多副本 controller-manager 必须在调度批次入口接入 getboot-lock 分布式锁；发布事件 claim 是第二道防线，不能替代批次锁。
- GatePilot 模块只默认依赖 getboot-lock 契约，不默认强制带入 getboot-coordination 运行实现；Redis / ZooKeeper 锁实现由具体部署包显式引入并配置。
- 回滚推进只能读取已保存的 `GatewayConfigSnapshot` 中的 `PublishedConfig`，不能重新读取当前草稿资源拼出“伪回滚”。
- apiserver 不能通过一个同步 Service 调用把整条发布链路跑完，否则发布推进职责会回流到 apiserver。

### gatepilot-agent

节点侧代理，跟 proxy 部署在一起。

职责：

- 节点注册。
- 心跳上报。
- 拉取或 watch `PublishedConfig`。
- 校验配置版本、hash、签名和兼容性。
- 写入本地 last-good 配置。
- last-good 默认使用文件持久化，生产部署必须挂载本地卷或等价持久卷；`store-type`、`directory` 等配置项可以由 Nacos 下发，即使 Nacos 是集群，last-good 数据本身也要留在节点本地，保证 proxy 启动兜底不依赖远程读取。
- 写入 staged config。
- 通知本机 proxy apply 配置。
- 上报 apply 成功或失败原因。
- 上报节点状态、当前配置版本、路由健康、上游健康和简要运行指标。
- 控制面不可用时使用 last-good 启动。

禁止放：

- 配置编辑 API。
- 配置版本库。
- 发布审批。
- 发布策略决策。
- 业务流量转发。
- 管理页面。

### gatepilot-proxy

数据面网关。

职责：

- 接收业务流量。
- 执行已发布配置。
- 路由匹配。
- 路径改写。
- 上游转发，实际网络 I/O 交给 Spring Cloud Gateway。
- 上游端点负载均衡，实际选择算法交给 Spring Cloud LoadBalancer。
- 限流、熔断、重试、超时、请求体限制。
- 流量染色执行。
- 灰度和蓝绿发布执行。
- 运行审计事件采集。
- 运行指标采集。
- 向 agent 暴露本机 apply / health / state 能力。

限流执行规则：

- GatePilot 不自造通用限流算法，运行时通过 `getboot-limiter` 编程式入口申请许可。
- proxy 默认只依赖 getboot-limiter API，不把 `getboot-coordination` / Redisson 运行实现强制带入合包，避免未配置 Redis 时启动即连接本地 Redis。
- 需要分布式限流的部署形态必须显式引入和配置 getboot-coordination，未就绪时 proxy 对启用限流的路由返回限流组件不可用。

转发底座规则：

- `gatepilot-proxy` 必须以 Spring Cloud Gateway 作为 HTTP 数据面转发底座。
- GatePilot 负责 `PublishedConfig` 编译、路由命中、LoadBalancer 实例列表适配、染色、限流、熔断、重试策略适配、审计和指标采集。
- Spring Cloud Gateway 负责请求转发、连接管理、响应写回、HTTP 客户端细节和成熟过滤器能力。
- 禁止在 proxy 热路径重新维护一套 WebClient / Reactor Netty 手写业务转发器。
- 健康探测、agent 同步这类非业务转发可以使用 getboot 增强后的 HTTP 客户端，但必须和业务流量转发通道分开。

负载均衡执行规则：

- `gatepilot-proxy` 必须以 Spring Cloud LoadBalancer 作为上游端点负载均衡组件。
- GatePilot filter 只把已命中的 upstream 改写成 SCG `lb://` 目标地址，不在 filter 内选择具体 endpoint。
- `gatepilot-proxy/infrastructure/loadbalancer` 只负责把 `PublishedConfig` 中的 upstream endpoints 转换成 Spring Cloud `ServiceInstance` 列表，并按本机健康状态过滤实例。
- 轮询、随机和加权轮询交给 Spring Cloud LoadBalancer 的 `RoundRobinLoadBalancer`、`RandomLoadBalancer` 和 `WeightedServiceInstanceListSupplier`。
- 一致性哈希、最少连接等策略如果没有成熟组件支撑，必须先在控制面校验为不支持或引入明确组件适配，禁止回到 proxy 热路径手写算法。
- 健康探测状态可以影响实例列表，但健康探测本身必须保持非业务转发通道，不能替代 SCG 业务转发链路。

禁止放：

- 配置编辑。
- 配置存储。
- 配置版本管理。
- 发布审批。
- 回滚决策。
- 管理台页面。
- 面向用户的管理 API。

### gatepilot-console

前端管理台。

Console 页面设计必须遵守 [GatePilot Console 设计规范](console-design-guidelines.md)。

职责：

- 页面展示。
- 表单编辑。
- 诊断入口。
- 发布操作入口。
- 审计查询入口。
- 配置向导和部署模板填写入口。
- 只调用 apiserver API。

禁止放：

- 后端配置存储。
- 发布决策。
- 直接调用 proxy。
- 直接读取数据库。
- 直接读取后端配置文件。

配置复杂度治理：

- Console 后续应提供类似 Helm values 的配置向导页面，用中文表单收集项目、路由、上游、治理策略、灰度蓝绿和 Kubernetes 部署参数。
- 模板渲染和校验由 apiserver 提供，console 只负责填写、预览、diff 和提交。
- 渲染结果必须落成 GatePilot 声明式资源、发布请求或 Kubernetes 部署清单，不允许绕过资源模型直接改 runtime 配置。
- agent / proxy 不感知模板和表单来源，它们仍然只消费 `PublishedConfig`。

### gatepilot-embedded

单 JVM 嵌入式适配模块。

职责：

- 单体合包下把 controller-manager 的端口适配到 apiserver 应用服务。
- 单体合包下把 agent 的 proxy apply 端口适配到本进程 proxy。
- 单体合包下把 proxy 运行审计事件适配到 agent 上报器。
- 只处理进程内端口桥接和模型转换，不承载控制面、数据面或发布策略。

禁止放：

- REST Controller。
- 资源 CRUD 或数据库实现。
- 发布策略决策。
- proxy filter 实现。
- 管理页面。
- 通用工具类。

`gatepilot-embedded` 是为了保证 `gatepilot-app` 只做启动和静态资源装配，同时避免 apiserver 反向依赖 controller-manager。分服务部署不依赖这个模块。

装配规则：

- `gatepilot.mode=standalone` 时启用 `gatepilot-embedded`，用于单 JVM 合包运行。
- `gatepilot.mode=cluster` 或未配置部署模式时不启用 `gatepilot-embedded`，避免 classpath 中存在模块就自动产生进程内桥接。
- 禁止再新增 `gatepilot.embedded.enabled` 这类第二开关，单体和集群切换只能由 `gatepilot.mode` 决定。

### gatepilot-app

合包启动器。

职责：

- 装配 apiserver。
- 装配 controller-manager。
- 装配 agent。
- 装配 proxy。
- 依赖 `gatepilot-embedded` 获得单 JVM 进程内适配能力。
- 承载 console 静态资源。
- 提供单 jar 启动入口。

禁止放：

- 业务逻辑。
- 配置治理实现。
- proxy filter 实现。
- 发布控制器实现。
- Repository。
- Controller 业务代码。

app 模块只是部署装配层。单体大包坏了，问题应该能定位到 apiserver、controller-manager、agent、proxy 或 console，而不是 app 自己。

## 3. 发布链路

配置发布必须走固定链路：

```text
console
  -> apiserver 保存草稿资源
  -> apiserver 校验资源
  -> apiserver 创建发布请求
  -> controller-manager reconcile
  -> controller-manager 生成 PublishedConfig
  -> agent watch / pull PublishedConfig
  -> agent 校验并写入 staged config
  -> agent 通知 proxy apply
  -> proxy 原子切换运行状态
  -> agent 上报 apply result
  -> controller-manager 汇总发布状态
  -> apiserver 提供查询
  -> console 展示结果
```

关键规则：

- proxy 不消费草稿配置，只消费 `PublishedConfig`。
- agent 不决定灰度比例，只同步和应用控制面发布的结果。
- controller-manager 负责发布编排和状态推进。
- apiserver 负责存储、权限、审计和查询。
- console 只是操作入口，不拥有发布逻辑。

## 4. Agent 协议

agent 和 apiserver 之间先按这些能力设计接口，具体 HTTP 路径可以后续实现时细化：

- 节点注册：`register node`
- 节点心跳：`heartbeat`
- 配置拉取：`pull published config`
- 配置订阅：`watch published config`
- 应用结果上报：`report apply result`
- 节点健康上报：`report node health`
- 上游健康上报：`report upstream health`
- 指标摘要上报：`report metrics summary`
- 运行事件上报：`report runtime event`

agent 本地至少要维护：

- node identity。
- current config version。
- staged config。
- last-good config。
- last apply result。
- control-plane connection state。

## 5. 资源模型规范

声明式资源统一采用三段式结构：

```text
metadata
spec
status
```

`metadata` 放资源标识和管理信息：

- id / name。
- namespace 或 tenant。
- labels。
- annotations。
- generation。
- createdAt。
- updatedAt。

`spec` 放期望状态：

- 项目。
- 路由。
- 上游。
- 认证策略。
- 流控策略。
- 熔断策略。
- 灰度策略。
- 蓝绿策略。

`status` 放实际状态：

- observedGeneration。
- phase。
- conditions。
- lastTransitionTime。
- currentPublishedVersion。
- nodeApplySummary。
- error message。

规则：

- 用户和 console 主要改 `spec`。
- controller-manager 写 `status`。
- proxy 不写资源，只通过 agent 上报状态。
- `PublishedConfig` 是给 agent / proxy 消费的发布产物，不是草稿配置。

## 6. 依赖方向

允许的依赖方向：

```text
console -> apiserver HTTP API
apiserver -> gatepilot-domain
controller-manager -> gatepilot-domain
controller-manager -> own domain ports
agent -> gatepilot-domain
agent -> apiserver client
agent -> own proxy apply port
proxy -> gatepilot-domain
embedded -> apiserver / controller-manager / agent / proxy
app -> embedded / console static
```

禁止的依赖方向：

```text
gatepilot-domain -> apiserver
gatepilot-domain -> controller-manager
gatepilot-domain -> agent
gatepilot-domain -> proxy
proxy -> apiserver implementation
proxy -> console
proxy -> controller-manager
agent -> console
apiserver -> controller-manager
console -> proxy
app -> 业务实现代码
```

## 7. 新能力放置规则

开发新能力前先回答一个问题：它改变的是资源、控制、同步、流量，还是页面？

- 新资源字段：放 `gatepilot-domain`。
- 配置 CRUD：放 `gatepilot-apiserver`。
- 发布、回滚、灰度推进：放 `gatepilot-controller-manager`。
- 节点注册、配置同步、last-good：放 `gatepilot-agent`。
- 接流量、转发、过滤链执行：放 `gatepilot-proxy`。
- 页面、表单、图表：放 `gatepilot-console`。
- 单 JVM 端口桥接：放 `gatepilot-embedded`。
- 单 jar 装配：放 `gatepilot-app`。

禁止因为“很多模块都要用”就新建 `common`、`core`、`shared`。

确实跨模块复用时，先判断它是什么：

- 是资源模型：放 `gatepilot-domain`。
- 是控制面逻辑：放 `apiserver` 或 `controller-manager`。
- 是节点同步逻辑：放 `agent`。
- 是数据面执行逻辑：放 `proxy`。
- 是纯前端展示：放 `console`。

## 8. 部署形态

### 单体大包

```text
gatepilot-app
  gatepilot-embedded
  apiserver
  controller-manager
  agent
  proxy
  console static
```

适合本地开发、小规模部署、快速试用。

要求：

- 运行配置使用 `gatepilot.mode=standalone`。
- app 只负责装配。
- embedded 只负责进程内端口桥接。
- 内部仍按模块接口协作。
- proxy 仍只消费 PublishedConfig。
- console 仍只调 apiserver。

### 分服务部署

```text
gatepilot-apiserver
gatepilot-controller-manager
gatepilot-proxy + gatepilot-agent
gatepilot-console
```

适合多网关节点、高可用、高流量场景。

要求：

- 运行配置使用 `gatepilot.mode=cluster`。
- agent 跟 proxy 同节点或同 Pod 部署。
- proxy 不直接连数据库。
- proxy 不直接读草稿配置。
- 控制面故障时，proxy 继续使用 last-good 配置。

## 9. 集群副本与高可用约束

GatePilot 必须天然支持横向扩容，不能只适配单节点网关。扩副本时遵守下面规则：

- proxy 副本保持无状态，不在本地保存草稿、版本库、发布决策或管理查询数据。
- 每个 proxy 副本旁边至少有一个 agent，agent 使用稳定 `nodeId` 注册到 apiserver。
- `PublishedConfig` 是所有 proxy 副本共同消费的发布产物，节点是否应用成功通过 agent 上报。
- controller-manager reconcile 必须幂等，同一个发布版本重复推进不能产生不同结果。
- controller-manager 多副本部署时只能有一个 active leader 推进发布，其他副本 standby 或只读观察。
- apiserver 多副本部署时必须共享同一个配置存储和版本存储，不能使用各进程本地内存作为事实来源。
- agent pull / watch 需要带上当前配置版本、nodeId、zone 和能力信息，避免控制面误判节点状态。
- proxy 启动时控制面不可用，必须通过 agent 使用 last-good 配置启动；如果没有 last-good，要保持不接流量并上报原因。
- 发布状态必须按节点聚合，至少能看到 desired、applied、failed 和每个失败节点的原因。
- 同 zone 或同机房发布应支持分批推进，避免一次性把所有副本切到坏配置。

这些约束意味着：扩一个网关副本，本质上只是新增一个 `GatewayNode`，由 agent 拉取同一份 `PublishedConfig`，proxy 原子应用配置，controller-manager 汇总节点应用结果。不能让 proxy 副本之间互相依赖，也不能让某个 proxy 副本成为配置主节点。

## 10. 大规模流量硬约束

GatePilot 的长期目标是接入大量项目和高并发大流量。架构上必须做到：项目数量、路由数量、流量规模增长时，主要通过扩副本、扩分片、扩存储和扩控制面实例解决，不能要求修改 proxy 转发代码。

硬约束：

- 任何项目接入都必须表达为资源和策略，不允许为单个项目写专属转发代码。
- proxy 必须保持无状态，业务流量处理路径不访问数据库、不调用 apiserver、不等待 controller-manager。
- proxy 运行态必须使用预编译、不可变的路由索引和策略快照，配置切换只能做原子引用替换。
- 路由匹配不能随项目数线性扫描，后续实现必须按 host、path prefix、method、priority 建立索引。
- `PublishedConfig` 必须支持按 project、namespace、zone、isolationGroup、configShard 下发，避免所有节点消费全部配置。
- agent pull / watch 必须携带 `nodeId`、`zone`、`isolationGroup`、`configShards`、当前版本和当前序号。
- apiserver 的列表、审计、事件、发布历史接口必须分页或游标化，禁止一次性返回全量。
- controller-manager 生成配置必须按分片幂等推进，单个项目发布不能阻塞所有项目。
- 高流量项目必须能通过资源字段调度到独立隔离组，不需要改代码。
- 发布策略、染色规则、蓝绿/灰度权重都必须是数据驱动，不能写死在 filter 里。
- 可观测数据按节点、项目、路由、上游维度聚合，明细查询走时间窗口和分页。

规模化路径：

```text
增加项目
  -> 新增 GatewayProject / GatewayRoute / Upstream / Policy 资源
  -> controller-manager 生成对应分片 PublishedConfig
  -> 目标 agent 按 configShard 拉取
  -> proxy 原子替换本地路由索引

增加流量
  -> 增加 proxy + agent 副本
  -> 新节点注册为 GatewayNode
  -> controller-manager 将节点纳入目标集合
  -> agent 拉取对应 PublishedConfig
  -> proxy 接入负载均衡并开始承载流量
```

验收口径：

- 接 1000 个项目时，不需要新增 Java 代码。
- 单项目高流量时，通过调大 proxy 副本、独立 isolationGroup、独立上游和独立发布策略解决。
- 多项目高流量时，通过 configShard、zone、isolationGroup 分摊配置和运行压力。
- 控制面短时不可用时，proxy 继续用 last-good 配置承载已有流量。

实际容量必须用压测证明，不能只靠架构假设。后续 Phase 9 必须增加项目规模、路由规模、配置发布和数据面吞吐压测。

### 模块类比

为了降低理解成本，可以按下面类比理解各模块：

- `gatepilot-domain` 像 Kubernetes 资源类型定义，只定义资源长什么样。
- `gatepilot-apiserver` 像 Kubernetes apiserver，是资源登记处、查询入口、权限和审计入口。
- `gatepilot-controller-manager` 像控制器集合，持续把期望状态推进成已发布状态。
- `gatepilot-agent` 像 kubelet，守在每个 proxy 节点旁边，负责注册、拉配置、last-good 和上报状态。
- `gatepilot-proxy` 像真正承载业务流量的 Pod / 数据面，只执行本地已发布配置。
- `gatepilot-console` 像 Dashboard，只调 apiserver。
- `gatepilot-app` 像本地一体化启动包，只装配，不写业务。

### 大流量项目隔离例子

假设 `project-a` 是百亿级流量项目：

```text
GatewayProject(project-a)
  spec.trafficTier = critical
  spec.isolationGroup = project-a-high
  spec.configShard = shard-project-a

GatewayNode(proxy-001..proxy-200)
  spec.isolationGroup = project-a-high
  spec.configShards = [shard-project-a]
```

发布时 controller-manager 只为 `shard-project-a` 生成对应 `PublishedConfig`。这些 agent 拉到配置后通知本机 proxy 原子切换。业务流量只进入 `project-a-high` 这组 proxy 副本，扩容时新增 proxy + agent 节点即可，不需要改 Java 转发代码。

这不是唯一形态，但属于大型网关、服务网格和云控制面常用的可扩展路径：控制面管资源，数据面无状态扩容，高流量租户用隔离池和配置分片承载。

### xDS / Envoy 升级方向

Envoy 是业界常用的高性能代理数据面，xDS 是控制面向 Envoy 动态下发配置的一组协议族。它们解决的问题和 GatePilot 的长期方向类似：控制面生成配置，数据面热加载配置并承载流量。

当前 GatePilot 先使用基于 Spring Cloud Gateway 的 Java proxy，复用成熟 HTTP 转发和过滤器体系。后续如果需要更高性能或接入服务网格生态，可以增加 Envoy 数据面适配：

- controller-manager 在生成 `PublishedConfig` 的同时，也可以生成 xDS 需要的 listener、route、cluster、endpoint 配置。
- agent 可以扩展为 xDS 管理客户端或 Envoy sidecar 管理器。
- proxy 模块可以保留 Java 数据面，也可以新增 Envoy adapter，不影响 apiserver、console 和资源模型。

这只是升级方向，不是当前阶段的必选复杂度。当前阶段更重要的是先把控制面 / agent / proxy 边界设计成类似 xDS 的单向配置下发模型，保证以后换数据面时不会推倒重来。

### Kubernetes 部署与联动

GatePilot 默认面向 Kubernetes 部署。生产环境推荐分服务部署，单体 `gatepilot-app` 用于开发、演示、小规模环境或应急合包，不作为大流量集群的唯一形态。

入口推荐拓扑：

```text
Client
  -> VIP(HAProxy + Keepalived)
  -> Nginx 集群
  -> Kubernetes Service(gatepilot-proxy-<isolationGroup>)
  -> gatepilot-proxy Pod + gatepilot-agent sidecar
  -> Upstream Service / EndpointSlice / 外部上游
```

职责边界：

- HAProxy + Keepalived 只负责 VIP 高可用、Nginx 节点健康检查和四层 / 七层入口转发，不承载 GatePilot 项目路由、灰度、染色和发布配置。
- Nginx 集群负责 TLS 卸载、基础 WAF、静态入口、粗粒度 host 转发和到 GatePilot proxy Service 的负载均衡，不维护项目级治理规则。
- GatePilot proxy 负责项目级路由、转发、限流、熔断、重试、染色、蓝绿 / 灰度执行和审计采集。
- GatePilot apiserver 仍然是配置事实来源，生产配置写数据库；Kubernetes 资源不能绕过 apiserver 直接改数据面。
- controller-manager 多副本运行时使用 Kubernetes Lease 或等价机制做 leader election，避免多个 controller 同时推进同一发布。
- 每个 proxy Pod 建议携带 agent sidecar；agent 只从网关配置读取 namespace、nodeId、zone、isolationGroup、configShards 后注册为 `GatewayNode`，这些配置由 Nacos 等配置中心统一下发。
- 高流量项目使用独立 Deployment、Service、HPA、PDB、configShard 和 isolationGroup，Nginx 按 host 或入口路径转发到对应 proxy Service。

近期只考虑 Kubernetes 基础部署联动，不做 CRD / Gateway API / Envoy / xDS 等深集成，避免把当前阶段复杂度拉高。下面能力只作为长期路线保留，不能插队影响当前开发主线：

| 层级 | 能力 | GatePilot 落点 | 说明 |
| --- | --- | --- | --- |
| L1 | Deployment / Service / HPA / PDB / readiness | 运维部署规范和 Helm / manifest | 先保证 proxy 能无状态扩副本，控制面可多副本。 |
| L2 | Service / EndpointSlice 服务发现 | `Upstream` 后续增加 Kubernetes discovery 引用 | 上游可从固定 endpoint 升级为发现 K8s Service endpoints。 |
| L3 | Lease leader election | `gatepilot-controller-manager/infrastructure/leader` | 多 controller 只允许一个 active reconciler。 |
| L4 | Gateway API / Ingress 入口集成 | 可选 K8s adapter | 只做入口资源对接或导入，不替代 GatePilot 数据库事实来源。 |
| L5 | CRD / Operator | 长期可选能力 | 允许 `kubectl apply` 管理 GatePilot 资源，但必须通过 apiserver admission 和持久化。 |
| L6 | Envoy / xDS 数据面 | 长期升级方向 | 高性能数据面可替换 Java proxy，但资源模型和控制面不推倒重来。 |

上层已经有 Nginx 集群和 HAProxy + Keepalived VIP 时，当前阶段不需要 GatePilot 去实现 L4 负载均衡。GatePilot 要做的是把自己变成一个可横向扩容、可分片、可观测、可被 Nginx 稳定代理的数据面服务。

参考官方能力：

- Kubernetes Gateway API: https://kubernetes.io/docs/concepts/services-networking/gateway/
- Kubernetes Service: https://kubernetes.io/docs/concepts/services-networking/service/
- Kubernetes EndpointSlice: https://kubernetes.io/docs/reference/kubernetes-api/service-resources/endpoint-slice-v1/
- Kubernetes client-go leader election: https://pkg.go.dev/k8s.io/client-go/tools/leaderelection

## 11. 历史模块退役约束

父级 Maven reactor 只保留 GatePilot 新模块：

- `gatepilot-domain`
- `gatepilot-apiserver`
- `gatepilot-controller-manager`
- `gatepilot-agent`
- `gatepilot-proxy`
- `gatepilot-console`
- `gatepilot-embedded`
- `gatepilot-app`

历史模块已物理删除，也不能被 GatePilot 新模块依赖、导入或复制：

- 历史管理模块和历史 admin-server 模块对应能力收敛到 `gatepilot-apiserver`。
- 发布编排能力重建到 `gatepilot-controller-manager`。
- 节点注册、配置同步、last-good 和状态上报重建到 `gatepilot-agent`。
- 历史 runtime 模块和历史 server 模块对应能力收敛到 `gatepilot-proxy`。
- 历史 admin-web 模块对应能力收敛到 `gatepilot-console`。
- 历史 legacy-config 模块只作为旧配置字段和测试样本参考，不允许成为生产事实来源。

历史源码是否物理删除，必须按能力对账结果推进：

- 已经按 GatePilot 新模型重建并有测试覆盖的能力，可以删除旧实现。
- 新模型尚未覆盖的能力，先保留为临时参考样本，不能进入主构建。
- 删除前必须在执行计划里标明对应新模块、覆盖状态和剩余缺口。
- 一旦出现新模块直接依赖历史模块，必须先修边界，再继续开发。

改造期间也必须遵守边界：

- 不再向数据面增加配置管理能力。
- 不再向管理面增加数据面转发能力。
- 不再新增 `core/common/shared/support` 这类泛化包。
- 资源元模型固定放在 `gatepilot-domain/.../resource/meta`，不能再新增 `resource/common`。
- 新代码优先按目标模块职责落位。

## 12. 旧能力改造清单

历史网关模块里已经实现了不少能力，但 GatePilot 已经换成资源模型、`PublishedConfig` 下发模型和 DDD 包边界，所以不能直接迁移。后续要做的是基于旧能力样本重建和改造：只能拿“领域逻辑、算法、测试用例和工程经验”，不能把旧模型、旧接口、旧包结构和旧职责混杂关系原样搬过来。

### 可改造能力

| 旧实现位置 | 已有能力 | 改造目标 | 当前覆盖状态 | 改造要求 |
| --- | --- | --- | --- | --- |
| 历史配置模块/GatewayProperties | 旧 YAML 配置模型，包含项目、路由、认证、CORS、健康检查、上下文头、染色、审计、治理策略 | `gatepilot-domain` 资源模型和 `gatepilot-apiserver` admission 校验 | 已覆盖并删除旧实现 | 只能作为字段设计参考，不能继续让生产依赖本地 YAML 作为事实来源 |
| 历史配置模块/GatewayRouteDefinitionLocator | 路由编译和路由命中 | `gatepilot-proxy/domain/runtime` + `interfaces/gateway` | 已覆盖编译索引、最长前缀命中，并接入 Spring Cloud Gateway 承担业务转发 | 改成从 `PublishedConfig` 预编译，不再从 `GatewayProperties` 读取 |
| GatePilot 早期 UpstreamEndpointSelector | 上游多端点轮询、随机、加权和哈希选择 | `gatepilot-proxy/infrastructure/loadbalancer` + Spring Cloud LoadBalancer | 已删除手写选择器，已改为 SCG `lb://` + Spring Cloud LoadBalancer 实例选择 | GatePilot 只适配 ServiceInstance 列表和本机健康状态；一致性哈希、最少连接没有成熟组件前不得在 proxy 热路径自造 |
| 历史配置模块/GatewayTrafficColorResolver | Header、Cookie、Query、IP 等染色解析 | `gatepilot-proxy/domain/runtime` + `interfaces/gateway` | 已覆盖 Header / Cookie / Query / IP / 权重 / 默认色解析，并通过 SCG 治理过滤器执行请求头透传和响应头回写 | 保留解析规则，输入改成 proxy 运行态请求上下文和已发布策略 |
| 历史配置模块/GatewayPropertiesValidator | 配置合法性校验 | `gatepilot-apiserver/application` 和 `gatepilot-controller-manager/application` | 已覆盖发布 dry-run、引用校验、PublishedConfig 编译校验并删除旧实现 | 拆成资源 admission 校验、发布 dry-run 校验、PublishedConfig 编译校验 |
| 历史 runtime 模块/GatewayAuthenticationFilter | 路由级认证 | `gatepilot-proxy` | 已覆盖策略判断并接入 getboot-auth，旧实现已删除 | 参考旧判断逻辑，继续复用 getboot-auth，策略来自 `PublishedConfig`，失败响应使用 getboot 统一规则 |
| 历史 runtime 模块/GatewayMethodAccessFilter | HTTP 方法白名单 | `gatepilot-proxy` | 已覆盖策略判断和 WebFlux 405 执行 | 改为读取编译后的 route policy，热路径不能访问控制面 |
| 历史 runtime 模块/InternalRouteAccessFilter | 内部运维入口保护 | `gatepilot-proxy` 或 `gatepilot-apiserver` 各自入口保护 | 已覆盖 proxy 内部入口保护并删除旧实现 | 按入口分开，proxy 保护本机 apply / health / state，apiserver 保护管理 API |
| 历史 runtime 模块/GatewayTrafficColorFilter | 流量染色执行和响应头回写 | `gatepilot-proxy` | 已覆盖解析、请求头透传和响应头回写，旧实现已删除 | 与灰度、蓝绿选择统一走运行态策略快照 |
| 历史 runtime 模块/GatewayCircuitBreakerFilter | 轻量熔断和 fallback | `gatepilot-proxy` | 已覆盖本机滑动窗口状态机、OPEN / HALF_OPEN / CLOSED 转换、getboot `ApiResponse` fallback 和 getboot-governance / Sentinel 规则发布，旧实现已删除 | 策略来自 `PublishedConfig`，状态只存在 proxy 本机内存，后续限流和治理公共能力仍优先接 getboot |
| 历史 runtime 模块/GatewayAccessAuditFilter | 访问审计采集 | `gatepilot-proxy` 采集，`gatepilot-agent` 上报，`gatepilot-apiserver` 持久化查询 | 已覆盖 proxy 采集、agent 批量上报和 apiserver 持久化查询 | proxy 不保留管理查询 API，审计明细必须分页和持久化 |
| 历史 runtime 模块/UpstreamHealthIndicator | 上游健康探测 | `gatepilot-agent` 或 `gatepilot-proxy` 本机指标采集 | 已覆盖 proxy 主动探测、agent 状态上报和 console 节点展示，旧实现已删除 | agent 统一上报节点和上游健康，apiserver 负责查询展示 |
| 历史 server 模块/GatewaySentinelRuleRegistrar | Sentinel 网关规则注册 | `gatepilot-proxy/infrastructure` | 已覆盖 `PublishedConfig` -> getboot-limiter 运行时适配和 Sentinel 网关规则发布，旧实现已删除 | 当前不直接迁移旧 YAML Sentinel 注册器；后续若接 Sentinel，仍由 `PublishedConfig` 编译生成规则 |
| 历史管理模块/GatewayDiagnosticsService | 路由诊断、策略诊断、染色和发布变体解释 | `gatepilot-apiserver/application` 和 `gatepilot-console` 页面 | 已覆盖 apiserver 诊断用例和 console 页面，旧实现已删除 | 诊断基于已发布配置、节点状态和审计数据，不直接读取 proxy 内存 |
| 历史管理模块/GatewayManagementService | 配置导出、dry-run、diff、版本快照 | `gatepilot-apiserver` 和 `gatepilot-controller-manager` | 已覆盖资源查询、dry-run、diff、快照摘要和数据库持久化，旧实现已删除 | 管理 API 和发布编排拆开，快照和版本必须持久化到数据库 |
| 历史管理模块/GatewayAccessAuditController | 审计查询 API | `gatepilot-apiserver/interfaces/rest` | 已覆盖 apiserver 持久化查询 API | 查询 apiserver 持久化审计，不查 proxy 本地内存 |
| 历史管理模块/GatewayRouteCatalogEndpoint | 当前路由目录展示 | `gatepilot-apiserver` 查询 API 和 `gatepilot-console` | 已覆盖 route catalog、PublishedConfig 和节点 apply 摘要展示，旧实现已删除 | 展示资源、PublishedConfig 和节点 apply 状态，不再做 Actuator 私有端点 |

### 改造顺序

1. 先参考 `GatewayRouteDefinitionLocator` 和 `GatewayTrafficColorResolver` 的核心算法，在 proxy 运行态编译链路里重建。
2. 再按 `PublishedConfig` 运行快照改造认证、方法白名单、染色、熔断、审计采集这些数据面过滤器。
3. 然后参考 `GatewayPropertiesValidator` 的校验样本，拆成资源 admission 校验、发布 dry-run 和 PublishedConfig 编译校验。
4. 再把诊断、diff、快照、审计查询按 apiserver 查询用例和 console 页面重建。
5. 最后改造 Sentinel、健康检查、压测和分片发布能力。

### 改造红线

- 不把旧 runtime 模块的配置查看、版本快照、Web 管理、审计查询搬进 `gatepilot-proxy`。
- 不把旧管理模块的内存快照仓库作为生产实现。
- 不把旧 YAML 配置模型作为 GatePilot 生产事实来源。
- 不按旧 package 结构搬代码，必须按 DDD 归属重新落位。
- 改造公共能力前仍然先查 getboot，不能因为旧模块里有实现就直接复制成 GatePilot 公共能力。
- 每改造一个能力，必须同时改造对应测试或补新的边界测试。
