# GatePilot Demo Upstream

这是 GatePilot 自带的示例上游服务，用来验证路由转发、流量染色、灰度和蓝绿发布链路。它不是网关运行时的一部分，只作为演示和验收包发布。

同一个 jar 可以启动两个实例：

```bash
java -jar gatepilot-demo-upstream.jar --spring.profiles.active=stable
java -jar gatepilot-demo-upstream.jar --spring.profiles.active=green
```

默认端口：

| Profile | Port | Version | Color |
| --- | --- | --- | --- |
| `stable` | `19081` | `stable-v1` | `stable` |
| `green` | `19082` | `green-v2` | `green` |

服务会回显上游收到的路径、查询串、版本、颜色和关键请求头，方便确认请求是否经过 GatePilot 转发。
