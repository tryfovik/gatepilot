package com.dt.platform.gateway.infrastructure.route;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 暴露当前网关生效路由目录的 Actuator 端点。
 */
@Endpoint(id = "platformGatewayRoutes")
public class GatewayRouteCatalogEndpoint {

    private final GatewayProperties properties;

    private final GatewayRouteDefinitionLocator routeDefinitionLocator;

    /**
     * 创建路由目录端点。
     *
     * @param properties 网关配置
     * @param routeDefinitionLocator 路由定义定位器
     */
    public GatewayRouteCatalogEndpoint(GatewayProperties properties,
                                       GatewayRouteDefinitionLocator routeDefinitionLocator) {
        this.properties = properties;
        this.routeDefinitionLocator = routeDefinitionLocator;
    }

    /**
     * 返回当前生效的项目与路由目录。
     *
     * @return 路由目录
     */
    @ReadOperation
    public GatewayRouteCatalogView routes() {
        Map<String, ProjectAccumulator> projects = new LinkedHashMap<>();
        for (GatewayRouteDefinition definition : routeDefinitionLocator.getRouteDefinitions()) {
            GatewayProperties.ProjectProperties projectProperties = properties.getProjects().get(definition.getProjectKey());
            ProjectAccumulator accumulator = projects.computeIfAbsent(
                    definition.getProjectKey(),
                    ignored -> new ProjectAccumulator(
                            definition.getProjectKey(),
                            definition.getProjectSegment(),
                            projectProperties == null ? null : projectProperties.getDisplayName()
                    )
            );
            accumulator.routes().add(new RouteView(
                    definition.getRouteKey(),
                    definition.getRouteSegment(),
                    definition.getApiPathRoots(),
                    definition.getInternalPathRoots(),
                    definition.getServiceUri(),
                    definition.getServicePathPrefix(),
                    definition.getActuatorUri(),
                    definition.isAuthRequired(),
                    definition.getPublicApiPaths(),
                    definition.getConnectTimeoutMs(),
                    definition.getResponseTimeout() == null ? null : definition.getResponseTimeout().toMillis()
            ));
        }
        return new GatewayRouteCatalogView(
                properties.getApiPrefix(),
                properties.getInternalPrefix(),
                new ContextHeadersView(
                        properties.getContextHeaders().isEnabled(),
                        properties.getContextHeaders().getProjectHeaderName(),
                        properties.getContextHeaders().getRouteHeaderName()
                ),
                projects.values().stream()
                        .map(ProjectAccumulator::toView)
                        .toList()
        );
    }

    private record ProjectAccumulator(String projectKey,
                                      String pathSegment,
                                      String displayName,
                                      List<RouteView> routes) {

        private ProjectAccumulator(String projectKey, String pathSegment, String displayName) {
            this(projectKey, pathSegment, displayName, new ArrayList<>());
        }

        private ProjectView toView() {
            return new ProjectView(projectKey, pathSegment, displayName, List.copyOf(routes));
        }
    }

    /**
     * 路由目录视图。
     *
     * @param apiPrefix API 前缀
     * @param internalPrefix 内部前缀
     * @param contextHeaders 上下文头配置
     * @param projects 项目路由目录
     */
    public record GatewayRouteCatalogView(String apiPrefix,
                                          String internalPrefix,
                                          ContextHeadersView contextHeaders,
                                          List<ProjectView> projects) {
    }

    /**
     * 上下文头视图。
     *
     * @param enabled 是否启用
     * @param projectHeaderName 项目标识头
     * @param routeHeaderName 路由标识头
     */
    public record ContextHeadersView(boolean enabled,
                                     String projectHeaderName,
                                     String routeHeaderName) {
    }

    /**
     * 项目路由视图。
     *
     * @param projectKey 项目标识
     * @param pathSegment 项目路径分段
     * @param displayName 项目显示名
     * @param routes 路由列表
     */
    public record ProjectView(String projectKey,
                              String pathSegment,
                              String displayName,
                              List<RouteView> routes) {
    }

    /**
     * 单条路由视图。
     *
     * @param routeKey 路由标识
     * @param pathSegment 路由路径分段
     * @param apiPathRoots 生效的 API 根路径
     * @param internalPathRoots 生效的内部根路径
     * @param serviceUri API 上游地址
     * @param servicePathPrefix API 上游路径前缀
     * @param actuatorUri 运维上游地址
     * @param authRequired 是否要求认证
     * @param publicPaths 匿名放行的相对路径
     * @param connectTimeoutMs 连接超时
     * @param responseTimeoutMs 响应超时
     */
    public record RouteView(String routeKey,
                            String pathSegment,
                            List<String> apiPathRoots,
                            List<String> internalPathRoots,
                            URI serviceUri,
                            String servicePathPrefix,
                            URI actuatorUri,
                            boolean authRequired,
                            List<String> publicPaths,
                            Integer connectTimeoutMs,
                            Long responseTimeoutMs) {
    }
}
