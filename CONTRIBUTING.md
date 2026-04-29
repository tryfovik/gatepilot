# 贡献指南

感谢你关注 GatePilot。这个项目优先保证架构边界清晰、运行链路可靠和控制台体验可排障。

## 开发前准备

GatePilot 依赖 GetBoot 公共能力。首次构建前请先安装 GetBoot：

```bash
git clone https://github.com/tryfovik/getboot.git
cd getboot
mvn -q -DskipTests install
```

然后构建 GatePilot：

```bash
git clone https://github.com/tryfovik/gatepilot.git
cd gatepilot
mvn test
```

Console 构建：

```bash
cd gatepilot-console
npm ci
npm run build
```

## 代码约定

- 后端按 `interfaces / application / domain / infrastructure` 分层
- 控制面负责配置、发布、回滚、审计查询和状态聚合
- 数据面只负责消费已发布配置、转发、治理和轻量审计采集
- agent 跟随 proxy 节点部署，负责配置同步、last-good、心跳和 apply 结果上报
- Console 只调用 apiserver API，不直接访问数据库、agent 或 proxy
- 公共能力优先放到 GetBoot，GatePilot 不重复造公共基础设施
- 新增页面必须有真实动作、错误态、空态和加载态

## 提交前检查

```bash
mvn test
cd gatepilot-console && npm run build
```

如果改动涉及发布、回滚、节点 apply、审计或 proxy 热路径，请补充对应测试。
