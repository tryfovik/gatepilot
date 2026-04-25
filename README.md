# GatePilot

GatePilot 是网关控制与运行系统，目标是支撑多项目、高并发、大流量下的统一入口、治理、发布和排障能力。

父级 Maven 主构建只保留 GatePilot 新模块。历史模块源码暂时只作为能力对账样本存在，不参与主构建，不能被新模块依赖、导入或复制；等旧能力都按新模型重建并有测试覆盖后，再物理删除。

## 文档

- [架构边界规范](docs/architecture-boundaries.md)
- [执行计划](docs/gatepilot-execution-plan.md)
- [Console 设计规范](docs/console-design-guidelines.md)
- [Console 开发说明](gatepilot-console/README.md)

## 模块

| 模块 | 职责 |
| --- | --- |
| `gatepilot-domain` | 声明式资源定义、枚举和值对象。 |
| `gatepilot-apiserver` | 管理后端，负责资源存储、查询、发布入口、回滚入口、快照、事件、agent 协议和审计查询。 |
| `gatepilot-controller-manager` | 控制器集合，负责 watch / list 发布意图，reconcile 期望状态并生成 `PublishedConfig`。 |
| `gatepilot-agent` | 节点侧代理，负责注册、心跳、拉取 `PublishedConfig`、staged / last-good、调用本机 proxy apply 和上报结果。 |
| `gatepilot-proxy` | 数据面网关，只消费 `PublishedConfig` 并执行路由、转发、限流、熔断、重试、染色、蓝绿 / 灰度和审计采集。 |
| `gatepilot-console` | Vue 管理台，只调用 apiserver API。 |
| `gatepilot-app` | 单体合包装配模块，只组合各模块和 console 静态资源，不放业务代码。 |

历史 `platform-gateway-*` 模块已经从父 POM 摘掉。它们不是开发入口，只用于确认旧能力没有丢失。

## 边界

- runtime 数据面不保存配置草稿，不做配置查看、版本快照、Web 管理和审计查询。
- admin 能力落在 `gatepilot-apiserver`，负责配置存储、查看、发布、回滚、快照、审计和查询。
- console 是前端，只调用 apiserver API。
- controller-manager 只推进发布和回写状态，不对外提供管理查询 API。
- agent 只做节点侧配置同步和 apply 协调，不决定发布策略。
- app 只装配，不写业务逻辑。

GatePilot 内部按 DDD 分层：`interfaces / application / domain / infrastructure`。禁止新增 `core`、`common`、`shared`、`support` 这类泛化垃圾包。公共能力先查 getboot，getboot 没有就先补 getboot，再由 GatePilot 依赖。

## 运行链路

```text
console
  -> apiserver admission 校验并保存声明式资源
  -> apiserver 创建发布意图资源
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

热路径只在 proxy 本地运行态快照中完成，不能访问控制面。

## 开发

```bash
mvn -q test
mvn -q -DskipTests package
```

前端使用 Vue 3 + Vite + TypeScript，页面默认中文优先，风格参考成熟运维控制台，避免炫技式视觉。
