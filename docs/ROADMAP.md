# GatePilot 路线图

GatePilot 的目标是成为企业内部可以长期演进的平台级网关控制系统。路线图会随着真实接入和压测结果持续调整。

## 已完成

- 控制面 / 数据面 / agent / console 模块边界
- 声明式资源模型
- 项目接入向导
- 资源中心和平台配置资源管理
- dry-run、发布请求、PublishedConfig、快照和回滚
- Spring Cloud Gateway 数据面转发
- Nacos 服务发现和 Spring Cloud LoadBalancer 接入
- 蓝绿 / 灰度 / 染色配置模型
- 节点注册、心跳、last-good 和 apply 状态
- 诊断工作台、运行审计和控制面事件
- GitHub CI、Issue 模板、PR 模板、贡献指南和 License Header 校验

## 近期重点

- 完善控制面 MySQL 初始化脚本和数据库迁移策略
- 补齐 Console 端到端用例和关键页面截图回归
- 加强发布状态聚合和失败原因标准化
- 完善 Nacos 认证、命名空间、分组和多注册中心场景
- 压测数据面热路径，确认高并发下路由匹配、治理策略和审计队列开销
- 补齐 GetBoot 公共能力验证样例，确保 TraceId、Limiter、Lock、Database 等能力接入稳定

## 中期计划

- 支持更细的发布批次控制和按节点分批 apply
- 支持策略模板和类似 Helm values 的高级参数模板
- 支持更多可观测指标导出方式
- 支持 Console 权限模型和操作审计闭环
- 支持更多服务发现来源的适配扩展

## 不做什么

- 不让业务请求热路径访问数据库
- 不在 Console 中直接编辑 proxy 本地配置
- 不要求用户手工维护大量下游 IP
- 不在 GatePilot 里重复实现 GetBoot 已有公共能力
- 不把一体化启动包变成业务实现包
