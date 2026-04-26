# GatePilot

GatePilot 是一个面向多项目、高并发和大流量场景的平台级网关系统。它把网关配置、发布控制、节点同步、流量治理和排障观测拆成清晰的控制面与数据面，让团队可以用声明式资源管理入口流量，用可视化控制台完成项目接入、灰度发布、蓝绿切换和问题诊断。

如果你的系统正在接入越来越多的业务项目，网关配置越来越复杂，发布风险越来越高，排障链路越来越长，GatePilot 希望把这些事情收回到一个稳定、可扩展、可观察的平台里。

## 亮点

- **声明式配置**：用 `GatewayProject`、`GatewayRoute`、`Upstream`、`TrafficPolicy`、`ReleasePolicy` 等资源描述期望状态。
- **控制面 / 数据面分离**：控制面负责配置、发布、回滚、快照和审计；数据面只消费已发布配置并承载业务流量。
- **高并发友好**：proxy 本地预编译路由索引，热路径不访问数据库和控制面，配置切换走原子替换。
- **多副本可扩展**：apiserver、controller-manager、agent、proxy 都支持按职责部署和扩副本，controller-manager 使用分布式锁避免重复推进发布。
- **灰度与蓝绿发布**：支持候选上游、权重分流、流量染色、发布请求、版本快照和回滚。
- **治理能力内置**：支持路由转发、认证、限流、熔断、重试、上游健康检查、访问审计和运行指标。
- **可视化控制台**：Vue Console 提供中文优先的项目接入向导、资源查看、节点状态、路由诊断、快照对比和审计查询。
- **Kubernetes 友好**：proxy 可以独立扩容，适合接在 VIP / Nginx / Service 后面作为统一治理数据面。

## 架构

```text
Console
  -> Apiserver
  -> Controller Manager
  -> PublishedConfig
  -> Agent
  -> Proxy
  -> Upstream Services
```

一次典型发布流程：

```text
填写项目接入向导
  -> 渲染声明式资源
  -> dry-run 校验
  -> 保存资源
  -> 创建发布请求
  -> controller-manager 生成 PublishedConfig
  -> agent 拉取并写入 staged / last-good
  -> proxy 原子切换运行态
  -> agent 上报应用结果
  -> console 展示发布状态
```

业务请求只进入 proxy。proxy 不编辑配置、不保存版本、不提供管理页面，控制面短时不可用时可以继续使用 last-good 配置承载已有流量。

## 模块

| 模块 | 说明 |
| --- | --- |
| `gatepilot-domain` | 资源模型、枚举和值对象 |
| `gatepilot-apiserver` | 管理 API、资源存储、模板渲染、发布入口、快照、审计和 agent 协议 |
| `gatepilot-controller-manager` | 发布 reconcile、配置快照生成、发布状态聚合 |
| `gatepilot-agent` | 节点注册、心跳、配置拉取、last-good、proxy apply 协调和状态上报 |
| `gatepilot-proxy` | 基于 Spring Cloud Gateway 的数据面转发、治理和审计采集 |
| `gatepilot-console` | Vue 管理控制台 |
| `gatepilot-embedded` | 单体模式下的进程内适配 |
| `gatepilot-app` | 一体化启动包，只负责装配 |

## 适合场景

- 一个网关要接入大量业务项目，需要统一配置、统一发布和统一排障。
- 高流量项目需要独立扩 proxy 副本，不希望每次扩容都改 Java 代码。
- 团队希望把灰度、蓝绿、染色、限流、熔断、审计和诊断做成平台能力。
- 配置需要持久化、可回滚、可追踪，不能只靠本地 YAML 或临时脚本。
- 运维入口在 Kubernetes、Nginx、VIP 等体系之上，网关只专注业务流量治理。

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

如果要体验完整闭环，请先启动 apiserver 或一体化应用，再打开 Console 的“接入”页面创建项目、保存资源并发布。

## 生产部署建议

- 控制面：apiserver + controller-manager，可多副本部署。
- 数据面：proxy + agent 跟随部署，按流量水平横向扩容。
- 存储：配置资源、发布产物、快照、节点状态和审计数据使用数据库持久化。
- 入口：可以使用 VIP / Nginx / Kubernetes Service 承接外部流量，GatePilot proxy 负责项目级路由和治理。
- 观测：指标、Trace、日志上下文和 Prometheus 暴露复用 getboot 可观测能力。

## 开发约定

- 公共能力优先复用 getboot，缺公共能力先补 getboot，再让 GatePilot 接入。
- 后端按 DDD 分层：`interfaces / application / domain / infrastructure`。
- 数据面热路径不能访问控制面和数据库。
- Console 只调用 apiserver API，不直连数据库、agent 或 proxy。
- 一体化启动包只做装配，不写业务实现。

GatePilot 的目标很简单：让网关既能扛流量，也能被人看懂、管住、排得动问题。
