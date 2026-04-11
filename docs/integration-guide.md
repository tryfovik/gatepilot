# Platform Gateway 接入手册

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
- 上游业务路径前缀 `service-path-prefix`
- 上游运维地址 `actuator-uri`
- 内部运维允许的方法列表 `internal-methods`
- 认证要求 `auth.required`
- 允许匿名访问的相对路径 `auth.public-paths`

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
            service-path-prefix: /admin
            actuator-uri: http://127.0.0.1:19080
            internal-methods:
              - GET
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
- `service-path-prefix`
  网关转发到上游时，重写后的路径前缀。
- `actuator-uri`
  `/internal/**` 和健康检查访问的上游地址。
- `internal-methods`
  当前内部运维入口允许的方法列表；为空时表示不限制。
- `auth.required`
  是否要求登录态或认证上下文。
- `auth.public-paths`
  在当前 route 下允许匿名访问的相对路径，写相对路径，不要把 `/api/{project}/{route}` 前缀重复写进去。

如果配置了方法白名单，网关会在路径命中后继续校验请求方法：

- 命中允许的方法，继续转发到上游
- 命中不允许的方法，直接返回 `405 Method Not Allowed`
- 响应头会带 `Allow`，方便调用方修正请求

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

### 第四步：启动网关

本地调试可以直接基于示例配置启动：

```bash
mvn -q -DskipTests package
java -jar platform-gateway-server/target/platform-gateway-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=local
```

如果是第一次落本地配置，可以先参考：

- `platform-gateway-server/src/main/resources/application.yml`
- `platform-gateway-server/src/main/resources/application-local.example.yml`

## 5. 联调时先验这四个点

建议先跑最小联调闭环，不要一上来就拿完整业务流压测：

1. 公开路由能通。
2. 受保护路由的认证结果符合预期。
3. 方法白名单在不符合约束时返回 `405` 和 `Allow`。
4. 内部运维入口只能走 `/internal/**`。
5. Actuator 路由目录和实际配置一致。

示例命令：

```bash
curl -i http://127.0.0.1:18082/api/game/open/system/ping
curl -i http://127.0.0.1:18082/api/game/admin/system/ping
curl -i -X DELETE http://127.0.0.1:18082/api/game/admin/games/catalog
curl -i http://127.0.0.1:18082/internal/game/admin/actuator/health
curl -s http://127.0.0.1:18082/actuator/platformGatewayRoutes
```

接入成功后，至少应该能从 `platformGatewayRoutes` 看见：

- `apiPrefix`
- `internalPrefix`
- `projects[].projectKey`
- `projects[].routes[].apiPathRoots`
- `projects[].routes[].internalPathRoots`
- `projects[].routes[].apiMethods`
- `projects[].routes[].internalMethods`
- `projects[].routes[].authRequired`
- `projects[].routes[].publicPaths`
- `projects[].routes[].connectTimeoutMs`
- `projects[].routes[].responseTimeoutMs`

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

### 4) `/internal/**` 调不通

优先检查：

- `actuator-enabled` 是否开启
- `actuator-uri` 是否正确
- 上游服务是否真的暴露了 actuator 端点
- 上游健康检查路径是否与 `platform.gateway.health.path` 对齐

### 5) 前端跨域失败

优先检查：

- `platform.gateway.cors.enabled`
- `allowed-origin-patterns`
- `allowed-methods`
- `allowed-headers`
- `exposed-headers`

本地联调时默认配置已经放开了 `localhost` 和 `127.0.0.1`。

## 7. 观测与排障入口

平台网关默认暴露这些关键观测入口：

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/platformGatewayRoutes`

推荐排查顺序：

1. 先看 `/actuator/health`，确认网关自身是活的。
2. 再看 `/actuator/platformGatewayRoutes`，确认路由已经按预期编译生效。
3. 再访问目标 `/api/**` 或 `/internal/**` 路径，判断是网关拦截还是上游报错。

## 8. 上线前检查清单

接入完成后，至少过一遍下面这张清单：

- `project` 和 `route` 的 `path-segment` 已定稿，不再依赖历史别名
- 每条 route 的 `service-uri`、`service-path-prefix`、`actuator-uri` 已核对
- `admin` 和 `open` 的认证策略已拆清楚
- 需要受限的方法已经写入 `api-methods` / `internal-methods`
- `auth.public-paths` 使用的是相对路径
- `connect-timeout-ms` 和 `response-timeout` 已按上游实际延迟设置
- `/actuator/platformGatewayRoutes` 返回内容和配置一致
- `/api/**` 和 `/internal/**` 的联调命令已经跑通
- 上游的 `/actuator/health` 可以被网关正常探测

## 9. 仓库内可直接参考的文件

如果你准备直接在这个仓库里接项目，优先看这几个文件：

- `README.md`
- `platform-gateway-server/src/main/resources/application.yml`
- `platform-gateway-server/src/main/resources/application-local.example.yml`

这三个文件已经覆盖了网关定位、默认能力和接入配置模板。
