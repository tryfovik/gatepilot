# GatePilot App

`gatepilot-app` 是 GatePilot 单体合包装配模块，只负责把 apiserver、controller-manager、agent、proxy 和 Console 静态资源放到一个启动包里。

## Console 静态资源

打包单体应用前先在 `gatepilot-console` 执行：

```bash
npm run build
```

`gatepilot-app` 会把 `gatepilot-console/dist` 作为 Spring Boot 静态资源打进包里。

## 边界

- 不写业务 Controller。
- 不写配置治理逻辑。
- 不写 proxy filter。
- 不写 Repository。
- 不绕过各模块既有边界。

单体模式用于本地开发、小规模部署和快速试用；生产高流量场景仍建议按控制面、agent、proxy、console 分服务部署。

## getboot 公共能力

合包默认启用 `getboot-observability`：

- Trace 请求头：`X-Trace-Id`
- MDC 日志键：`traceId`
- 响应头回写：开启
- Reactor 上下文传播：开启
- Prometheus 指标：开启

日志格式沿用 getboot 示例中的 `[tid:%X{traceId}]`，不要在 GatePilot 内自定义另一套 Trace 字段。
