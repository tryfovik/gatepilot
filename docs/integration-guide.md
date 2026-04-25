# GatePilot 接入手册

这份文档面向准备把自己的项目接入 `platform-gateway` 的开发者和运维同学。

目标不是做“勉强能转发”的临时代理，而是按统一入口、统一治理、统一观测的方式把项目正式挂到平台网关上。

## 1. 接入前先确认的约束

接入前先把下面四个约束对齐，否则后面一定反复返工：

1. 每个项目都走统一入口模型，只允许 `project -> route` 两级结构。
2. 对外入口固定为 `/api/{project}/{route}/**`，内部入口固定为 `/internal/{project}/{route}/**`。
3. 网关不为历史路径保留兼容别名，接入时直接切到标准入口。
4. 每条路由都必须明确上游服务地址、上游路径前缀、认证策略和健康检查地址。

一个项目接入后，使用方通常只需要记住两类路径：

- 业务请求：`/api/game/admin/system/ping`
- 内部运维：`/internal/game/admin/actuator/health`

## 2. 你需要准备什么

接入一个新项目前，先准备好这些信息：

- 项目标识 `project key`，例如 `game`
- 项目路径段 `path-segment`，通常与项目标识保持一致
- 路由标识 `route key`，例如 `admin`、`open`
- 路由路径段 `path-segment`
- 业务服务地址 `service-uri`
- API 允许的方法列表 `api-methods`
- API 最大请求体大小 `api-max-request-size`
- 上游业务路径前缀 `service-path-prefix`
- 上游运维地址 `actuator-uri`
- 内部运维允许的方法列表 `internal-methods`
- 内部运维最大请求体大小 `internal-max-request-size`
- 认证要求 `auth.required`
- 允许匿名访问的相对路径 `auth.public-paths`
- Trace 请求头名称 `getboot.observability.trace.header-name`
- 是否由网关补齐 Trace 请求头和响应头 `request-header-propagation-enabled` / `response-header-enabled`
- 需要做瞬时失败保护的路由重试策略 `governance.retry.*`
- 流量染色配置 `traffic-color.*`
- 灰度 / 蓝绿发布变体配置 `release.variants.*`

如果一个项目内部已经区分后台接口和开放接口，通常会拆成：

- `admin`
- `open`

如果只有一类业务入口，也仍然建议保留 route 维度，不要把所有能力直接堆在 project 根上。

## 3. 最小接入配置

下面是一份可以直接参考的最小配置模板：

```yaml
platform:
  gateway:
    api-prefix: /api
    internal-prefix: /internal
    projects:
      village-care:
        enabled: true
        path-segment: village-care
        display-name: Village Care
        routes:
          admin:
            enabled: true
            api-enabled: true
            actuator-enabled: true
            path-segment: admin
            service-uri: http://127.0.0.1:19080
            api-methods:
              - GET
              - POST
            api-max-request-size: 10MB
            service-path-prefix: /admin
            actuator-uri: http://127.0.0.1:19080
            internal-methods:
              - GET
            internal-max-request-size: 1MB
            connect-timeout-ms: 2000
            response-timeout: 5s
            auth:
              required: true
              public-paths:
                - /system/ping
                - /auth/**
          open:
            enabled: true
            api-enabled: true
            actuator-enabled: true
            path-segment: open
            service-uri: http://127.0.0.1:19080
            service-path-prefix: /open
            actuator-uri: http://127.0.0.1:19080
            auth:
              required: false
```

关键字段说明：

- `path-segment`
  决定实际对外暴露的 URL 路径段。
- `service-uri`
  API 请求要转发到的上游地址。
- `api-methods`
  当前 API 路由允许的方法列表；为空时表示不限制。
- `api-max-request-size`
  当前 API 路由允许的最大请求体大小，支持 `10MB`、`2MB`、`512KB` 等写法。
- `service-path-prefix`
  网关转发到上游时，重写后的路径前缀。
- `actuator-uri`
  `/internal/**` 和健康检查访问的上游地址。
- `internal-methods`
  当前内部运维入口允许的方法列表；为空时表示不限制。
- `internal-max-request-size`
  当前内部运维入口允许的最大请求体大小。
- `auth.required`
  是否要求登录态或认证上下文。
- `auth.public-paths`
  在当前 route 下允许匿名访问的相对路径，写相对路径，不要把 `/api/{project}/{route}` 前缀重复写进去。

如果配置了方法白名单，网关会在路径命中后继续校验请求方法：

- 命中允许的方法，继续转发到上游
- 命中不允许的方法，直接返回 `405 Method Not Allowed`
- 响应头会带 `Allow`，方便调用方修正请求

如果配置了请求体上限，网关还会在转发前校验请求体大小：

- 未超过上限，继续转发
- 超过上限，直接返回 `413 Payload Too Large`
- 这种限制适合用于登录、上传、导入、批量写入等接口的边界保护

如果要为 API 路由补充瞬时失败重试，可以按 route 维度声明 `governance.retry.*`：

```yaml
platform:
  gateway:
    projects:
      village-care:
        routes:
          open:
            governance:
              retry:
                enabled: true
                retries: 2
                methods:
                  - GET
                statuses:
                  - 502
                  - 503
                  - 504
                exceptions:
                  - io
                  - timeout
                backoff:
                  first-backoff: 20ms
                  max-backoff: 200ms
                  factor: 2
                  based-on-previous-value: true
```

建议把重试只用于显式可重放的读请求或幂等请求，不要默认给扣款、下单、状态变更这类写接口打开重试。

如果要给路由增加上游故障兜底，可以开启路由级熔断 fallback：

```yaml
platform:
  gateway:
    projects:
      village-care:
        routes:
          open:
            governance:
              circuit-breaker:
                enabled: true
                status-codes:
                  - 500
                  - 502
                  - 503
                  - 504
                sliding-window-size: 100
                minimum-number-of-calls: 20
                failure-rate-threshold: 50
                wait-duration-in-open-state: 30s
                permitted-number-of-calls-in-half-open-state: 10
                slow-call-duration-threshold: 5s
                slow-call-rate-threshold: 80
                fallback-status: 503
                fallback-code: 503
                fallback-message: Service temporarily unavailable
```

未配置 `fallback-uri` 时，网关会返回内置 UTF-8 JSON fallback；需要自定义 fallback 时，目前只支持 `forward:` URI。

如果要做灰度或蓝绿发布，还需要把“流量染色”和“发布变体”一起配好：

```yaml
platform:
  gateway:
    traffic-color:
      enabled: true
      header-name: X-Traffic-Color
      response-header-enabled: true
      trust-request-header: false
      default-color: stable
      rules:
        - name: canary-header
          source: header
          field-name: X-Canary
          pattern: true
          match-strategy: exact
          color: green
    projects:
      village-care:
        routes:
          open:
            release:
              weight-hash-headers:
                - X-User-Id
                - X-Tenant-Id
                - X-Trace-Id
              variants:
                green:
                  service-uri: http://127.0.0.1:29080
                  actuator-uri: http://127.0.0.1:29080
                  weight: 10
                  match-colors:
                    - green
```

网关会先算出当前请求的 `X-Traffic-Color`，再用这个颜色优先命中发布变体；如果没有显式颜色且配置了 `weight`，网关会按稳定哈希做权重灰度；如果没有变体命中，流量会自动回落到默认上游。

## 4. 标准接入步骤

### 第一步：给项目和路由定名

建议命名规则尽量稳定：

- `project key` 用业务域标识，例如 `game`、`village-care`
- `route key` 用能力边界标识，例如 `admin`、`open`
- `path-segment` 和 key 尽量保持一致，减少记忆成本

不要做的事情：

- 不要用版本号做 `project key`
- 不要把环境名写进路径里
- 不要为了兼容旧路径增加别名 route

### 第二步：对齐上游接口布局

网关会把请求改写到 `service-path-prefix` 下，所以接入前先确认：

- `/api/{project}/{route}/system/ping` 转发后应该落到哪里
- `/api/{project}/{route}/auth/login` 转发后应该落到哪里
- `/internal/{project}/{route}/actuator/health` 是否能命中上游 actuator

例如：

- 网关入口：`/api/game/admin/users/list`
- `service-path-prefix: /admin`
- 上游实际收到：`/admin/users/list`

### 第三步：配置认证策略

一般建议：

- 后台接口 `admin` 默认 `auth.required: true`
- 开放接口 `open` 根据业务决定是否要求认证
- 公共探活接口、登录接口、验证码接口通过 `auth.public-paths` 单独放行

`public-paths` 的写法是 route 内相对路径，例如：

```yaml
auth:
  required: true
  public-paths:
    - /system/ping
    - /auth/**
```

这里表示：

- `/api/game/admin/system/ping` 可匿名访问
- `/api/game/admin/auth/login` 可匿名访问
- `/api/game/admin/**` 下的其他路径仍然受认证保护

### 第四步：对齐 Trace 约定

平台网关不再单独造一套 Trace 规则，直接复用 `getboot-observability` 的约定。

推荐直接保持默认配置：

```yaml
getboot:
  observability:
    trace:
      enabled: true
      header-name: X-Trace-Id
      request-header-propagation-enabled: true
      response-header-enabled: true
```

联动规则很简单：

- 客户端已带 `X-Trace-Id`，网关直接复用，并继续透传给上游。
- 客户端没带 `X-Trace-Id`，网关生成新的 TraceId，并补齐到请求头和响应头。
- 下游如果也接了 `getboot-observability`，会自动沿用同一个 TraceId。
- 下游如果没接 `getboot-observability`，也应该至少读取并继续透传同一个 Trace 头。

这里最重要的不是“网关自己有 Trace”，而是整个调用链只认一套 header 名称，不要网关一套、下游另一套。

### 第五步：按需配置重试治理

重试治理只作用在 API 路由，适合处理瞬时网络波动、上游短暂 `502/503/504` 这类场景。

接入时建议先确定三件事：

- 当前 route 是否真的允许自动重试
- 哪些方法可以重试，优先限制在 `GET` 或明确幂等的方法
- 失败判定条件是按状态码、状态码分组还是异常类型触发

如果还没把上游幂等性边界想清楚，就先不要开重试。

### 第六步：配置流量染色与发布变体

这一步决定平台网关能不能真正支撑生产发布。

建议按这个顺序理解：

- 先定义流量颜色头，默认就是 `X-Traffic-Color`
- 再决定颜色从哪里来，按 header、cookie 还是 query 染色
- 最后给需要灰度 / 蓝绿发布的 route 挂发布变体

一个比较稳妥的默认策略是：

- 外部公网请求先不直接信任 `X-Traffic-Color`，由网关根据规则生成
- 内部联调或多跳网关场景，再按需开启 `trust-request-header`
- 变体只挂在 API 路由上，不额外改变认证、方法限制和请求体约束

### 第七步：启动网关

本地调试可以直接基于示例配置启动：

```bash
mvn -q -DskipTests package
java -jar platform-gateway-server/target/platform-gateway-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=local
```

如果是第一次落本地配置，可以先参考：

- `platform-gateway-server/src/main/resources/application.yml`
- `platform-gateway-server/src/main/resources/application-local.example.yml`

## 5. 联调时先验这些关键点

建议先跑最小联调闭环，不要一上来就拿完整业务流压测：

1. 公开路由能通。
2. 受保护路由的认证结果符合预期。
3. 方法白名单在不符合约束时返回 `405` 和 `Allow`。
4. 请求体超限时返回 `413`。
5. 客户端自带 `X-Trace-Id` 时，网关响应头和下游日志里仍然是同一个 TraceId。
6. 客户端不带 TraceId 时，网关会自动生成并在响应头里回写。
7. 如果启用了流量染色，网关响应头里的 `X-Traffic-Color` 和预期一致。
8. 如果当前 route 配了灰度 / 蓝绿变体，带颜色的请求能命中正确上游。
9. 如果当前 route 开了重试，瞬时失败会按预期重试，而且只作用在显式允许的方法上。
10. 内部运维入口只能走 `/internal/**`。
11. Actuator 路由目录和实际配置一致。
12. 路由诊断 API 能解释当前请求的路由命中、认证、染色、变体和治理策略。
13. 访问审计 API 能按 traceId、项目、路由、状态码、颜色和变体查询最近请求。
14. 管理配置 API 能导出当前配置，并对候选配置做 dry-run 校验、diff、版本快照和发布事件查询。

示例命令：

```bash
curl -i http://127.0.0.1:18082/api/game/open/system/ping
curl -i http://127.0.0.1:18082/api/game/admin/system/ping
curl -i -X DELETE http://127.0.0.1:18082/api/game/admin/games/catalog
curl -i -X POST http://127.0.0.1:18082/api/game/admin/import/jobs --data-binary @large-payload.json
curl -i -H 'X-Trace-Id: trace-demo-001' http://127.0.0.1:18082/api/game/open/system/ping
curl -i -H 'X-Canary: true' http://127.0.0.1:18082/api/game/open/system/ping
curl -i http://127.0.0.1:18082/internal/game/admin/actuator/health
curl -s http://127.0.0.1:18082/actuator/platformGatewayRoutes
curl -s -X POST http://127.0.0.1:18083/internal/_platform-gateway/diagnostics/route \
  -H 'Content-Type: application/json' \
  -d '{"method":"GET","path":"/api/game/admin/system/ping","headers":{"X-Canary":["true"]},"query":{},"cookies":{},"remoteAddress":"127.0.0.1"}'
curl -s 'http://127.0.0.1:18083/internal/_platform-gateway/audits?projectKey=game&routeKey=admin&limit=20'
curl -s 'http://127.0.0.1:18083/internal/_platform-gateway/audits?traceId=trace-demo-001'
curl -s 'http://127.0.0.1:18083/internal/_platform-gateway/config/effective'
curl -s -X POST http://127.0.0.1:18083/internal/_platform-gateway/config/validate \
  -H 'Content-Type: application/json' \
  --data-binary @candidate-gateway-config.json
curl -s -X POST http://127.0.0.1:18083/internal/_platform-gateway/config/diff \
  -H 'Content-Type: application/json' \
  --data-binary @candidate-gateway-config.json
curl -s -X POST http://127.0.0.1:18083/internal/_platform-gateway/config/versions \
  -H 'Content-Type: application/json' \
  --data-binary @candidate-gateway-snapshot.json
curl -s 'http://127.0.0.1:18083/internal/_platform-gateway/config/versions?limit=20'
curl -s 'http://127.0.0.1:18083/internal/_platform-gateway/config/releases?limit=20'
```

接入成功后，至少应该能从 `platformGatewayRoutes` 看见：

- `apiPrefix`
- `internalPrefix`
- `contextHeaders.enabled`
- `contextHeaders.projectHeaderName`
- `contextHeaders.routeHeaderName`
- `trafficColor.enabled`
- `trafficColor.headerName`
- `trafficColor.defaultColor`
- `trafficColor.rules[].source`
- `trafficColor.rules[].fieldName`
- `trafficColor.rules[].color`
- `projects[].projectKey`
- `projects[].routes[].apiPathRoots`
- `projects[].routes[].internalPathRoots`
- `projects[].routes[].apiMethods`
- `projects[].routes[].apiMaxRequestSizeBytes`
- `projects[].routes[].internalMethods`
- `projects[].routes[].internalMaxRequestSizeBytes`
- `projects[].routes[].authRequired`
- `projects[].routes[].publicPaths`
- `projects[].routes[].connectTimeoutMs`
- `projects[].routes[].responseTimeoutMs`
- `projects[].routes[].retry.enabled`
- `projects[].routes[].retry.retries`
- `projects[].routes[].retry.methods`
- `projects[].routes[].retry.statuses`
- `projects[].routes[].retry.series`
- `projects[].routes[].retry.exceptions`
- `projects[].routes[].retry.backoff.firstBackoffMs`
- `projects[].routes[].retry.backoff.maxBackoffMs`
- `projects[].routes[].retry.backoff.factor`
- `projects[].routes[].retry.backoff.basedOnPreviousValue`
- `projects[].routes[].releaseVariants[].variantKey`
- `projects[].routes[].releaseVariants[].matchColors`
- `projects[].routes[].releaseVariants[].serviceUri`
- `projects[].routes[].releaseVariants[].actuatorUri`

路由诊断 API 会返回：

- `matched`
- `routeType`
- `route.projectKey`
- `route.routeKey`
- `access.methodAllowed`
- `access.authenticationRequired`
- `traffic.color`
- `traffic.selectedReleaseVariant`
- `upstream.uri`
- `governance.retry`
- `governance.flowControl`
- `governance.circuitBreaker`
- `warnings`

访问审计 API 会返回最近审计事件：

- `timestamp`
- `traceId`
- `clientIp`
- `method`
- `path`
- `routeType`
- `projectKey`
- `routeKey`
- `status`
- `latencyMs`
- `trafficColor`
- `releaseVariant`
- `upstreamUri`
- `methodAllowed`
- `authRequired`
- `publicPath`
- `fallback`
- `outcome`
- `error`

当前审计 API 保存的是最近事件，适合联调和管理台排障底座；长期留存、跨实例检索和审计报表后续应接日志平台或持久化存储。

管理配置 API 会返回：

- `config/effective.summary`
- `config/effective.properties`
- `config/validate.valid`
- `config/validate.errors`
- `config/validate.warnings`
- `config/validate.summary`
- `config/diff.valid`
- `config/diff.currentSummary`
- `config/diff.candidateSummary`
- `config/diff.changes[].category`
- `config/diff.changes[].projectKey`
- `config/diff.changes[].routeKey`
- `config/diff.changes[].field`
- `config/diff.changes[].before`
- `config/diff.changes[].after`
- `config/versions[].versionId`
- `config/versions[].status`
- `config/versions[].active`
- `config/versions[].operator`
- `config/versions[].reason`
- `config/versions[].summary`
- `config/versions[].errors`
- `config/versions[].warnings`
- `config/versions[].changes`
- `config/releases[].recordId`
- `config/releases[].versionId`
- `config/releases[].action`
- `config/releases[].status`
- `config/releases[].operator`
- `config/releases[].message`

发布前建议先跑 `config/validate`，再跑 `config/diff` 给人确认变更范围；确认后可以调用 `config/versions` 保存候选版本快照。当前 API 只做只读导出、预检、差异计算和内存事件记录，不会直接替换线上配置。

## 6. 接入方最容易踩的坑

### 1) 返回 404，但你以为是上游没启动

先区分是网关没命中，还是上游返回 404：

- 如果路径不符合标准入口，网关本身就不会命中路由
- 如果 `project` 或 `route` 的 `path-segment` 配错，也会直接 miss
- 如果网关命中了但 `service-path-prefix` 配错，上游才会返回 404

先看 `/actuator/platformGatewayRoutes` 里是否存在这条生效路由，再决定是查网关还是查上游。

### 2) 后台接口一直 401/403

优先检查：

- `auth.required` 是否开启
- 当前请求路径是否在 `auth.public-paths` 里
- 网关认证组件是否已经装配并读取到了正确 token

一个常见错误是把 `public-paths` 写成完整路径，例如：

- 错误：`/api/game/admin/system/ping`
- 正确：`/system/ping`

### 3) 返回了 405

这通常不是上游挂了，而是网关已经命中了 route，但请求方法不在白名单里。

优先检查：

- `api-methods` 是否包含当前请求方法
- `internal-methods` 是否包含当前请求方法
- 响应头里的 `Allow` 是不是和配置一致
- `/actuator/platformGatewayRoutes` 暴露出来的方法列表是否正确

### 4) 返回了 413

这通常表示请求已经命中 route，但请求体超过了当前 route 配置的上限。

优先检查：

- `api-max-request-size` 或 `internal-max-request-size` 是否设置过小
- 当前接口是不是上传、导入、批量写入这类大报文接口
- 调用方传输的真实体积是否和预估一致
- `/actuator/platformGatewayRoutes` 暴露出来的字节上限是否正确

### 5) TraceId 前后不一致

优先检查：

- 网关和下游项目的 `getboot.observability.trace.header-name` 是否一致
- 网关是否开启了 `request-header-propagation-enabled` 和 `response-header-enabled`
- 下游服务是不是又自己生成了一次新的 TraceId
- 浏览器场景下，CORS 的 `exposed-headers` 是否暴露了 `X-Trace-Id`

如果下游没接 `getboot-observability`，也至少要保证它会读取当前请求头里的 TraceId，并继续往自己的下游传。

### 6) 重试没生效，或者重试到了不该重试的请求

优先检查：

- 当前配置的是不是 API 路由；`/internal/**` 不参与这套重试治理
- `governance.retry.enabled` 是否开启
- `methods` 是否包含当前请求方法
- `statuses`、`series`、`exceptions` 是否真的覆盖到了当前失败场景
- `retries` 和 `backoff.*` 是否设置得过于保守或过大

如果这是写请求，先回到业务幂等性设计本身，不要用网关重试去赌正确性。

### 7) 灰度 / 蓝绿流量没有命中预期变体

优先检查：

- `platform.gateway.traffic-color.enabled` 是否开启
- 网关响应头里的 `X-Traffic-Color` 实际算出来是什么
- 染色规则取值来源、字段名和匹配模式是否真的命中了当前请求
- `release.variants.<variant>.match-colors` 是否覆盖了当前颜色
- 变体上游地址是否已配置，并且变体路由在 `/actuator/platformGatewayRoutes` 里可见

如果响应头里的 `X-Traffic-Color` 已经正确，但流量仍然走默认上游，优先查发布变体配置；如果响应头本身就不对，先回去查染色规则。

### 8) `/internal/**` 调不通

优先检查：

- `actuator-enabled` 是否开启
- `actuator-uri` 是否正确
- 上游服务是否真的暴露了 actuator 端点
- 上游健康检查路径是否与 `platform.gateway.health.path` 对齐

### 9) 前端跨域失败

优先检查：

- `platform.gateway.cors.enabled`
- `allowed-origin-patterns`
- `allowed-methods`
- `allowed-headers`
- `exposed-headers`

本地联调时默认配置已经放开了 `localhost` 和 `127.0.0.1`。

### 10) 中文响应出现乱码

优先检查：

- 响应头里的 `Content-Type` 是否包含 `application/json;charset=UTF-8`
- 认证失败、方法拦截、内部入口拦截这类网关直接返回的响应，是否走到了网关默认 JSON 输出
- 如果启用了 Sentinel Gateway fallback，fallback 的 `content-type` 是否也明确写成 UTF-8
- 前端或调用方是否强行按错误编码解析了响应体

当前仓库默认已经把网关错误响应的 JSON `Content-Type` 固定为 `application/json;charset=UTF-8`。

## 7. 观测与排障入口

平台网关默认暴露这些关键观测入口：

数据面网关 `platform-gateway-server` 默认端口是 `18082`，负责业务转发和基础观测：

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/platformGatewayRoutes`

管理面后端 `platform-gateway-admin-server` 默认端口是 `18083`，负责排障和配置治理：

- `/actuator/health`
- `/actuator/platformGatewayRoutes`
- `/internal/_platform-gateway/diagnostics/route`
- `/internal/_platform-gateway/audits`
- `/internal/_platform-gateway/config/effective`
- `/internal/_platform-gateway/config/validate`
- `/internal/_platform-gateway/config/diff`
- `/internal/_platform-gateway/config/versions`
- `/internal/_platform-gateway/config/releases`

推荐排查顺序：

1. 先看 `/actuator/health`，确认网关自身是活的。
2. 再看 `/actuator/platformGatewayRoutes`，确认路由已经按预期编译生效。
3. 再访问目标 `/api/**` 或 `/internal/**` 路径，判断是网关拦截还是上游报错。
4. 如果发布链路有问题，先看响应头里的 `X-Traffic-Color`，再看 `/actuator/platformGatewayRoutes` 里的发布变体。
5. 如果链路排查需要串日志，直接看请求头和响应头里的 `X-Trace-Id` 是否贯通。
6. 如果不确定请求会命中哪里，先调用 `/internal/_platform-gateway/diagnostics/route` 做一次模拟诊断。
7. 如果请求已经发生，按 traceId 或 project / route 调 `/internal/_platform-gateway/audits` 查最近审计事件。
8. 如果准备改配置，先用 `/internal/_platform-gateway/config/validate` 和 `/internal/_platform-gateway/config/diff` 做发布前校验。

## 8. 上线前检查清单

接入完成后，至少过一遍下面这张清单：

- `project` 和 `route` 的 `path-segment` 已定稿，不再依赖历史别名
- 每条 route 的 `service-uri`、`service-path-prefix`、`actuator-uri` 已核对
- `admin` 和 `open` 的认证策略已拆清楚
- 需要受限的方法已经写入 `api-methods` / `internal-methods`
- 大报文接口已经写入合适的 `api-max-request-size` / `internal-max-request-size`
- `auth.public-paths` 使用的是相对路径
- 网关和下游的 Trace 头约定已经统一，默认使用 `X-Trace-Id`
- 浏览器场景已确认 `X-Trace-Id` 在 CORS `exposed-headers` 里可见
- 流量染色规则已经验证，`X-Traffic-Color` 的值和预期一致
- 浏览器场景已确认 `X-Traffic-Color` 在 CORS `exposed-headers` 里可见
- 灰度 / 蓝绿发布变体已经验证，染色请求会命中正确上游
- `platform.gateway.audit.enabled` 已按环境确认，最近事件保留条数和结构化日志输出符合排障要求
- 候选配置已经通过 `/internal/_platform-gateway/config/validate`，并用 `/internal/_platform-gateway/config/diff` 确认过变更范围，必要时已保存候选版本快照
- 开启重试的 route 已确认是可安全重放的请求，并验证过失败触发条件
- `connect-timeout-ms` 和 `response-timeout` 已按上游实际延迟设置
- `/actuator/platformGatewayRoutes` 返回内容和配置一致
- `/api/**` 和 `/internal/**` 的联调命令已经跑通
- 上游的 `/actuator/health` 可以被网关正常探测

## 9. 仓库内可直接参考的文件

如果你准备直接在这个仓库里接项目，优先看这几个文件：

- `README.md`
- `platform-gateway-server/src/main/resources/application.yml`
- `platform-gateway-server/src/main/resources/application-local.example.yml`
- `gatepilot-console/README.md`

前三个文件保留了历史数据面配置模板；GatePilot 的新控制面由 `gatepilot-apiserver` 承载，`gatepilot-console` 是 Vue 前端管理台，通过 REST/JSON 调用 apiserver，不嵌入网关数据面发布件。

本地查看管理台：

```bash
cd gatepilot-console
npm run dev
```

打开 Vite 输出的本地地址，默认通过 `/api/gatepilot` 代理到 `http://127.0.0.1:18080`。
