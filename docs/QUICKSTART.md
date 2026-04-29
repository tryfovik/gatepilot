# GatePilot 快速开始

这份文档用于本地启动 GatePilot，并用内置 demo 上游验证一次真实转发链路。

## 环境要求

- JDK 17
- Maven 3.8+
- Node.js 20+
- npm 9+

## 安装 GetBoot

GatePilot 复用 GetBoot 的公共能力。第一次构建前先安装 GetBoot 到本地 Maven 仓库：

```bash
git clone https://github.com/tryfovik/getboot.git
cd getboot
mvn -q -DskipTests install
```

## 构建 GatePilot

```bash
git clone https://github.com/tryfovik/gatepilot.git
cd gatepilot
mvn -q -DskipTests package
```

## 启动 Console

```bash
cd gatepilot-console
npm install
npm run dev
```

访问：

```text
http://127.0.0.1:5174
```

Console 默认把 `/api/gatepilot` 转发到：

```text
http://127.0.0.1:18080
```

## 启动 Demo 上游

```bash
java -jar gatepilot-demo-upstream/target/gatepilot-demo-upstream.jar --spring.profiles.active=stable
java -jar gatepilot-demo-upstream/target/gatepilot-demo-upstream.jar --spring.profiles.active=green
```

默认端口：

| 实例 | 端口 |
| --- | --- |
| stable | `19081` |
| green | `19082` |

## 验证路径

1. 打开 Console 的项目接入页面
2. 创建命名空间、团队、环境、配置分片和入口域名
3. 创建项目和路由
4. 上游选择固定端点或 Nacos 服务发现
5. 创建治理策略和发布策略
6. 在发布中心执行 dry-run
7. dry-run 通过后创建发布
8. 在节点页查看 apply 状态、当前版本和 last-good
9. 在诊断页模拟请求，确认路由、上游、染色和治理命中结果

## 常见问题

### Console 显示控制面不可用

确认 apiserver 或一体化应用已经启动，并且 Console 的代理地址仍然是 `http://127.0.0.1:18080`。

### 上游实例很多，不想维护 IP

使用 Nacos 注册中心。GatePilot 上游资源只需要引用注册中心和服务名，proxy 会在后台订阅实例变化。

### 审计会不会拖慢业务请求

运行审计是旁路能力。proxy 主链路只做轻量采集并写入内存队列，写库和上报异步批量执行，队列满时允许丢弃，不能阻塞转发。
