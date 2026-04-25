# GatePilot Console 设计规范

更新时间：2026-04-25

本文档约束 GatePilot Console 的产品和视觉设计。Console 是运维控制台，不是营销站、科技大屏或装饰型后台。目标是高级、克制、稳定、可排障、可长期使用。

## 1. 设计目标

GatePilot Console 应该像成熟的基础设施控制面：

- 信息密度高，但不拥挤。
- 状态清晰，故障能快速定位。
- 操作路径稳定，可反复使用。
- 视觉克制，不靠炫酷动画、渐变大屏、发光装饰制造高级感。
- 所有页面使用同一套布局、组件、间距、颜色和交互模式。
- 中文优先：默认界面语言、表格列名、状态文案、错误提示、确认弹窗和帮助信息都先使用中文。
- 预留国际化：代码层面避免把文案散落在业务逻辑里，后续可以再扩展英文等语言包。

禁止出现：

- 科技大屏风格。
- 营销落地页风格。
- 大面积渐变背景。
- 装饰性光斑、球体、毛玻璃堆叠。
- 每个页面一套不同的卡片、按钮、表格、状态色。
- 用大标题和大留白代替信息组织。
- 中英文混杂的导航、按钮和状态文案。

## 2. 参考系统

后续 Console 页面优先参考以下系统的产品气质和信息结构，不能随意混搭风格。

### Kong Konnect Gateway Manager

参考点：

- 控制面和数据面节点分开管理。
- 能查看 control planes、data plane nodes、连接状态和配置状态。
- 适合作为 GatePilot apiserver / agent / proxy 管理模型的参考。

参考链接：[Kong Konnect Gateway Manager](https://docs.konghq.com/konnect/gateway-manager/)

### Kubernetes Dashboard

参考点：

- 资源列表、资源详情、状态、事件和层级关系。
- 适合 GatewayProject、GatewayRoute、PublishedConfig、GatewayNode 等资源管理页面。
- 页面应该服务于“看资源当前状态”和“定位异常”，而不是展示炫酷图形。

参考链接：[Kubernetes Dashboard](https://kubernetes.io/docs/tasks/web-ui-dashboard/)

### Argo CD

参考点：

- desired state / live state。
- sync status / health status。
- 发布、回滚、diff 和资源状态的可解释性。
- 适合 GatePilot 发布链路、配置 diff、PublishedConfig 应用结果页面。

参考链接：[Argo CD 状态模型](https://argo-cd.readthedocs.io/en/stable/operator-manual/health/)

### Grafana

参考点：

- 可观测页面的信息组织。
- Dashboard 一致性。
- 指标、日志、Trace 和告警的分层展示。
- 适合总览大盘、路由指标、节点健康、上游健康和排障页面。

参考链接：[Grafana Dashboard Best Practices](https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/best-practices/)

## 3. 页面开工前规范

每个 Console 页面开工前必须先做页面设计说明，至少包含：

- 页面目标：这个页面解决什么运维问题。
- 目标用户：平台管理员、发布负责人、排障人员还是只读观察者。
- 参考系统：明确采用上面哪个参考系统的哪类页面结构。
- 主流程：用户从进入页面到完成任务的关键路径。
- 信息层级：首页、列表、详情、编辑、发布、审计分别展示什么。
- 中文文案：页面标题、导航、按钮、状态、错误提示和确认提示必须提前列出。
- 状态设计：loading、empty、error、readonly、dirty、publishing、success、failed。
- 风险操作：发布、回滚、删除、禁用、调权必须有确认和结果反馈。

没有页面设计说明，不开始写前端代码。

## 4. 信息架构

Console 默认采用稳定的运维控制台结构：

```text
左侧主导航
顶部环境和全局状态栏
主内容区
  列表页
  详情页
  编辑页
  发布页
  诊断页
右侧抽屉或弹窗用于局部操作
```

一级导航建议：

- 总览
- 项目
- 路由
- 策略
- 发布
- 节点
- 上游
- 诊断
- 审计
- 设置

页面组织规则：

- 列表页以表格为主，支持筛选、搜索、排序、分页和状态筛选。
- 详情页使用 tabs，避免一个页面无限向下堆。
- 复杂对象使用 resource summary + conditions + events。
- 发布链路使用 timeline / stepper 展示状态推进。
- 配置 diff 使用 before / after 或字段级变化列表。
- 总览页只放关键指标和异常入口，不做大屏。

## 5. 视觉规范

整体风格：

- 中性色为主，少量状态色。
- 背景干净，层级靠边框、分割线、阴影克制表达。
- 字号克制，表格和表单优先保证扫描效率。
- 圆角保持小半径，默认不超过 8px。
- 按钮、输入框、表格、tabs、badge、status chip 必须统一。

颜色规则：

- 成功：绿色。
- 警告：黄色或琥珀色。
- 失败：红色。
- 进行中：蓝色。
- 未知或禁用：灰色。

禁止：

- 每个模块使用不同主色。
- 大面积紫蓝渐变。
- 大面积深色科技背景。
- 装饰性图形作为主要视觉。
- 信息卡片套卡片。
- 只有图标没有文本的关键操作。

## 6. 组件规范

必须优先使用统一组件：

- Table
- FilterBar
- SearchInput
- StatusBadge
- ResourceHeader
- ConditionsList
- EventsTimeline
- DiffViewer
- ConfirmDialog
- DetailDrawer
- Tabs
- SegmentedControl
- Toast
- EmptyState

组件使用规则：

- 状态 badge 文案和颜色全局统一。
- 表格列顺序保持一致：名称、状态、版本、范围、更新时间、操作。
- 危险操作使用 destructive button，并要求确认。
- 编辑表单必须区分 draft、dirty、validating、validated、publishing。
- 所有失败状态必须给出下一步排查入口。

## 7. 中文与国际化

默认语言：

- GatePilot Console 默认面向中文用户，首屏和所有业务页面必须优先提供中文文案。
- 技术名词可以保留英文原词，例如 `PublishedConfig`、`agent`、`proxy`、`TraceId`，但需要在关键页面给出中文解释或上下文。
- 状态值展示优先中文，例如 `健康`、`异常`、`发布中`、`已同步`、`应用失败`。

文案管理：

- 前端实现时应集中管理中文文案，避免同一状态在不同页面出现不同翻译。
- 错误提示必须说明发生了什么、影响是什么、下一步可以怎么排查。
- 危险操作确认文案必须明确对象、动作和后果。

后续扩展：

- 可以预留 `zh-CN` 作为默认 locale。
- 英文等其他语言属于后续增强，不阻塞中文优先版本。

## 8. 技术栈约束

Console 前端采用 Vue 技术栈，先按下面组合实现：

- Vue 3。
- Vite。
- TypeScript。
- Vue Router。
- Pinia。

约束：

- `gatepilot-console` 是独立前端模块，不写后端业务代码。
- Console 只调用 `gatepilot-apiserver` 提供的 REST/JSON API。
- 中文文案必须集中管理，默认 locale 为 `zh-CN`。
- 页面状态、表格列、状态 badge、确认弹窗和错误提示都从统一组件或统一文案入口取值。
- 不把接口请求散落在页面组件里，必须收口到前端 API client。

## 9. 关键页面方向

### 总览

参考 Grafana 的信息分层，但不能做大屏。

必须包含：

- 当前发布版本。
- 节点健康。
- 路由健康。
- 上游健康。
- 错误率和延迟摘要。
- 最近发布事件。
- 最近告警。

### 项目 / 路由

参考 Kubernetes Dashboard 的资源列表和详情页。

必须包含：

- metadata。
- spec 摘要。
- status。
- conditions。
- events。
- 关联 policies。
- 当前 PublishedConfig 版本。

### 发布

参考 Argo CD 的 desired / live / sync / health 思路。

必须包含：

- 草稿配置。
- diff。
- dry-run 结果。
- 发布计划。
- 节点应用进度。
- 成功和失败节点。
- 回滚入口。

### 节点

参考 Kong Konnect Gateway Manager 的控制面 / 数据面节点管理。

必须包含：

- nodeId。
- zone。
- proxy version。
- agent version。
- 当前配置版本。
- 心跳时间。
- apply 状态。
- last-good 状态。
- 上游健康摘要。

### 诊断

必须服务排障：

- 输入 method、path、headers、query、cookies。
- 返回路由命中。
- 返回认证判断。
- 返回染色结果。
- 返回发布变体。
- 返回上游。
- 返回限流、重试、熔断策略。
- 返回可能失败原因。

## 9. 一致性检查

每次新增或大改 Console 页面前，必须检查：

- 是否明确参考了本文档列出的系统。
- 是否沿用统一导航和页面骨架。
- 是否复用已有组件。
- 是否存在新的按钮、表格、badge 风格。
- 是否覆盖 loading / empty / error / success / failed。
- 是否默认使用中文导航、中文按钮、中文状态和中文错误提示。
- 是否能从错误状态跳到排障入口。
- 是否避免了炫酷装饰和营销风格。

如果页面需要突破规范，必须先更新本文档，再实现页面。
