# GatePilot

GatePilot 是面向平台团队、网关团队和 SRE 团队的平台级网关控制系统。它帮助企业把多项目入口流量、发布变更、治理策略和故障排查收敛到一个统一平台里，让网关不只是“能转发请求”，还真正具备可管理、可发布、可观测、可扩展的生产能力。

当一个公司接入的业务越来越多时，网关往往会先变复杂：路由散在不同配置里，灰度和蓝绿依赖人工操作，限流熔断规则难以追踪，节点扩容后配置同步不可见，出问题时很难回答“这个请求命中了哪条路由、去了哪个上游、为什么被限流或降级”。GatePilot 解决的正是这类平台化问题。

## 适合谁

GatePilot 适合下面这些团队：

- **平台工程团队**：希望把项目接入、路由配置、发布流程和治理策略做成标准化平台能力。
- **网关 / 中间件团队**：需要支撑大量业务项目、高并发流量、多副本数据面和统一管理后台。
- **SRE / 运维团队**：关心发布可回滚、节点状态可见、故障链路可诊断、审计数据可追踪。
- **业务研发团队**：希望通过控制台完成接入和发布，不再为每个项目手写复杂网关配置。

## 解决什么问题

### 配置复杂，容易失控

业务项目多了以后，路由、上游、认证、限流、重试、熔断、染色、灰度和蓝绿配置会迅速膨胀。GatePilot 使用声明式资源管理期望状态，并提供中文控制台和项目接入向导，把复杂配置收敛成可校验、可预览、可发布的标准流程。

### 发布风险高，缺少闭环

网关变更通常影响面大，一次错误发布可能影响多个项目。GatePilot 把配置变更拆成 dry-run、发布请求、配置快照、节点应用结果和回滚流程，发布过程可追踪，失败原因可查看，回滚目标来自持久化快照。

### 流量治理分散，排障困难

限流、熔断、重试、认证、染色和上游健康如果分散在不同系统里，排障时很难还原现场。GatePilot 在控制面统一管理策略，在数据面本地执行策略，并通过运行审计、路由诊断、节点状态和指标把请求链路展示出来。

### 流量增长后，扩容不能靠改代码

面对高并发和大流量项目，正确做法应该是扩数据面副本、隔离配置分片、独立治理策略，而不是每次扩容都改 Java 代码。GatePilot 的 proxy 只消费已发布配置，节点通过 agent 注册、拉取配置并上报状态，可以按项目和流量水平横向扩容。

## 核心能力

| 能力 | 说明 |
| --- | --- |
| 项目接入 | 通过控制台填写项目、域名、路径、上游、治理和发布参数，自动生成声明式资源 |
| 路由转发 | 基于 Spring Cloud Gateway 承接成熟转发能力，支持路由匹配、路径处理和上游转发 |
| 上游治理 | 支持上游端点、负载均衡、健康检查、异常摘除和节点健康摘要 |
| 发布控制 | 支持 dry-run、发布请求、配置快照、发布状态聚合和版本回滚 |
| 蓝绿 / 灰度 | 支持稳定上游、绿色 / 候选上游、权重分流、流量染色和整体切换 |
| 流量策略 | 支持限流、重试、熔断、fallback、HTTP 方法控制和染色规则 |
| 认证策略 | 支持 API Key、JWT、OAuth2、Basic、双向 TLS 等认证模型 |
| 节点代理 | agent 负责节点注册、心跳、配置拉取、staged / last-good 和 apply 结果上报 |
| 可观测性 | 支持运行审计、路由诊断、节点状态、快照对比、指标和 Trace 上下文 |
| 多形态部署 | 支持一体化启动，也支持 apiserver、controller-manager、agent、proxy、console 分服务部署 |

## 产品视角

GatePilot 不只是一个反向代理，也不是简单的配置页面。它更像一套网关控制平台：

- 控制面负责“配置怎么管理、怎么校验、怎么发布、怎么回滚、怎么查看”
- 数据面负责“业务请求怎么匹配、怎么治理、怎么转发、怎么采集审计”
- 节点代理负责“配置怎么同步到每个 proxy、失败时怎么保留 last-good、状态怎么回传”
- 控制台负责“人怎么使用这个系统、怎么接入项目、怎么排查问题”

这样的分工可以让平台持续扩展，而不是随着功能增加把所有东西堆进一个越来越难维护的网关包里。

## 架构

```text
业务用户
  -> VIP / Nginx / Kubernetes Service
  -> GatePilot Proxy
  -> Upstream Services

平台用户
  -> GatePilot Console
  -> GatePilot Apiserver
  -> Controller Manager
  -> PublishedConfig
  -> Agent
  -> Proxy
```

业务请求只进入 proxy。proxy 使用本地已发布运行态处理请求，热路径不访问数据库、不访问控制面。控制面短时不可用时，proxy 可以继续基于 last-good 配置承载已有流量。

## 一次发布如何发生

```text
Console 填写项目接入或资源配置
  -> Apiserver 校验并保存声明式资源
  -> Console 发起 dry-run
  -> Apiserver 返回校验结果和资源摘要
  -> Console 创建发布请求
  -> Controller Manager 生成 PublishedConfig 和快照
  -> Agent 拉取配置并写入 staged / last-good
  -> Proxy 原子切换运行态
  -> Agent 上报应用结果
  -> Console 展示发布状态、节点状态和诊断信息
```

这个流程的目标很直接：让每次网关变更都有入口、有校验、有结果、有快照、有回滚。

## 控制台

GatePilot Console 默认中文优先，面向日常运维和平台运营场景设计，重点不是炫技，而是让问题看得见、查得动。

当前控制台包含：

- 总览：查看项目、路由、节点和配置分片概况
- 接入：通过向导生成项目、路由、上游、治理和发布资源
- 项目：查看项目资源和隔离信息
- 路由：查看已发布路由目录、上游和节点应用情况
- 策略：查看流量治理、蓝绿 / 灰度和认证策略
- 发布：执行 dry-run、创建发布、创建回滚、查看已发布配置
- 快照：查看发布快照，做版本 diff 和快照回滚
- 节点：查看 agent / proxy 副本、心跳、last-good 和上游健康
- 诊断：模拟一次请求，解释路由命中、染色、认证、上游和治理判断
- 审计：按 TraceId、节点、路由和结果查询运行审计

## 部署形态

### 一体化启动

适合本地体验、小规模环境或快速验证。`gatepilot-app` 只负责装配 apiserver、controller-manager、agent、proxy 和 console 静态资源，不放业务实现。

### 分服务部署

适合生产环境和高流量场景：

```text
gatepilot-apiserver
gatepilot-controller-manager
gatepilot-console
gatepilot-agent + gatepilot-proxy
```

控制面可以多副本部署，controller-manager 通过分布式锁避免重复推进发布。数据面按流量水平扩 proxy 副本，agent 跟随 proxy 节点部署。

### Kubernetes 部署

推荐入口链路：

```text
Client -> VIP / Nginx -> Kubernetes Service -> gatepilot-proxy -> Upstream
```

VIP、Nginx 和 Kubernetes Service 负责入口高可用和基础转发，项目级路由、蓝绿灰度、染色、限流、熔断、审计和诊断由 GatePilot 执行。

## 快速开始

构建后端：

```bash
mvn -q test
mvn -q -DskipTests package
```

启动 Console：

```bash
cd gatepilot-console
npm install
npm run dev
```

默认访问地址：

```text
http://127.0.0.1:5174
```

Console 默认把 `/api/gatepilot` 代理到：

```text
http://127.0.0.1:18080
```

要体验完整闭环，请先启动 apiserver 或一体化应用，再打开 Console 的“接入”页面创建项目、保存资源并发布。

### 示例上游

项目内置 `gatepilot-demo-upstream` 示例服务，用来验证 GatePilot 到业务上游的真实转发链路。同一个 jar 可以用 profile 启动 stable 和 green 两个实例：

```bash
java -jar gatepilot-demo-upstream/target/gatepilot-demo-upstream.jar --spring.profiles.active=stable
java -jar gatepilot-demo-upstream/target/gatepilot-demo-upstream.jar --spring.profiles.active=green
```

默认端口：

| 实例 | 端口 | 说明 |
| --- | --- | --- |
| stable | `19081` | 稳定版本上游 |
| green | `19082` | 绿色 / 候选版本上游 |

示例服务会回显请求路径、查询串、版本、颜色和关键请求头，适合验证路由、路径剥离、Trace 透传、染色和蓝绿 / 灰度策略。

## 模块说明

| 模块 | 职责 |
| --- | --- |
| `gatepilot-domain` | 声明式资源模型、枚举和值对象 |
| `gatepilot-apiserver` | 管理 API、资源存储、模板渲染、发布入口、快照、审计和 agent 协议 |
| `gatepilot-controller-manager` | 发布 reconcile、配置快照生成、发布状态聚合 |
| `gatepilot-agent` | 节点注册、心跳、配置拉取、last-good、proxy apply 协调和状态上报 |
| `gatepilot-proxy` | 基于 Spring Cloud Gateway 的数据面转发、治理和审计采集 |
| `gatepilot-console` | Vue 管理控制台 |
| `gatepilot-embedded` | 单体模式下的进程内适配 |
| `gatepilot-app` | 一体化启动包，只负责装配 |
| `gatepilot-demo-upstream` | 示例业务上游，用于演示和验收网关转发链路 |

## 工程约定

- 公共能力优先复用 getboot，缺公共能力先补 getboot，再让 GatePilot 接入
- 后端按 DDD 分层：`interfaces / application / domain / infrastructure`
- 数据面热路径不能访问控制面和数据库
- Console 只调用 apiserver API，不直连数据库、agent 或 proxy
- 一体化启动包只做装配，不写业务实现

GatePilot 希望成为团队可以长期依赖的网关平台：接入项目更轻，发布更稳，扩容更自然，排障更从容。
