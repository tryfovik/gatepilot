# Platform Gateway

独立发布的平台网关工程。

当前结构：

- `platform-gateway-core`
  路由、认证、内部入口保护、健康检查、配置校验等发布内核。
- `platform-gateway-assembly`
  可执行交付件，装配观测、治理与默认路由配置。

构建前提：

1. `com.dt:getboot-spring-boot-starter-parent:1.0.0` 已可从本地或私服解析。
2. 本仓库中的 `getboot-auth`、`getboot-observability`、`getboot-governance` 已安装到本地 Maven 仓库。
3. 在当前目录执行 `mvn -q -DskipTests package` 可产出可执行网关 JAR。
