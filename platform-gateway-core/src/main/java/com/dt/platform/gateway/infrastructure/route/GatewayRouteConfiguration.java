package com.dt.platform.gateway.infrastructure.route;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.cloud.gateway.support.RouteMetadataUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.regex.Pattern;

/**
 * 网关路由装配配置。
 */
@Configuration
public class GatewayRouteConfiguration {

    /**
     * 注册标准化路由定义定位器。
     *
     * @param properties 网关配置
     * @return 路由定义定位器
     */
    @Bean
    public GatewayRouteDefinitionLocator gatewayRouteDefinitionLocator(GatewayProperties properties) {
        return new GatewayRouteDefinitionLocator(properties);
    }

    /**
     * 注册生效路由目录端点。
     *
     * @param properties 网关配置
     * @param routeDefinitionLocator 路由定义定位器
     * @return 路由目录端点
     */
    @Bean
    public GatewayRouteCatalogEndpoint gatewayRouteCatalogEndpoint(GatewayProperties properties,
                                                                   GatewayRouteDefinitionLocator routeDefinitionLocator) {
        return new GatewayRouteCatalogEndpoint(properties, routeDefinitionLocator);
    }

    /**
     * 注册平台网关路由。
     *
     * @param builder 路由构建器
     * @param properties 网关配置
     * @param routeDefinitionLocator 路由定义定位器
     * @return 路由定位器
     */
    @Bean
    public RouteLocator gatewayRouteLocator(RouteLocatorBuilder builder,
                                            GatewayProperties properties,
                                            GatewayRouteDefinitionLocator routeDefinitionLocator) {
        RouteLocatorBuilder.Builder routes = builder.routes();
        for (GatewayRouteDefinition definition : routeDefinitionLocator.getRouteDefinitions()) {
            registerApiRoutes(routes, definition, properties);
            registerActuatorRoutes(routes, definition, properties);
        }
        return routes.build();
    }

    private void registerApiRoutes(RouteLocatorBuilder.Builder routes,
                                   GatewayRouteDefinition definition,
                                   GatewayProperties properties) {
        for (String apiPathRoot : definition.getApiPathRoots()) {
            String routeId = definition.buildRouteId("api", apiPathRoot);
            routes.route(routeId, route -> route.path(apiPathRoot, apiPathRoot + "/**")
                    .filters(filter -> applyApiFilters(filter, definition, apiPathRoot, properties))
                    .uri(definition.getServiceUri().toString()));
        }
    }

    private void registerActuatorRoutes(RouteLocatorBuilder.Builder routes,
                                        GatewayRouteDefinition definition,
                                        GatewayProperties properties) {
        for (String internalPathRoot : definition.getInternalPathRoots()) {
            String routeId = definition.buildRouteId("actuator", internalPathRoot);
            routes.route(routeId, route -> route.path(internalPathRoot, internalPathRoot + "/**")
                    .filters(filter -> applyActuatorFilters(filter, definition, internalPathRoot, properties))
                    .uri(definition.getActuatorUri().toString()));
        }
    }

    private UriSpec applyApiFilters(GatewayFilterSpec filter,
                                    GatewayRouteDefinition definition,
                                    String apiPathRoot,
                                    GatewayProperties properties) {
        GatewayFilterSpec gatewayFilterSpec = filter.rewritePath(buildRewritePattern(apiPathRoot),
                buildServiceTarget(definition.getServicePathPrefix()));
        gatewayFilterSpec = applyContextHeaders(gatewayFilterSpec, definition, properties);
        UriSpec uriSpec = gatewayFilterSpec;
        return applyRouteMetadata(uriSpec, definition);
    }

    private UriSpec applyActuatorFilters(GatewayFilterSpec filter,
                                         GatewayRouteDefinition definition,
                                         String internalPathRoot,
                                         GatewayProperties properties) {
        GatewayFilterSpec gatewayFilterSpec = filter.rewritePath(buildRewritePattern(internalPathRoot), "/${segment}");
        gatewayFilterSpec = applyContextHeaders(gatewayFilterSpec, definition, properties);
        UriSpec uriSpec = gatewayFilterSpec;
        return applyRouteMetadata(uriSpec, definition);
    }

    private GatewayFilterSpec applyContextHeaders(GatewayFilterSpec filterSpec,
                                                  GatewayRouteDefinition definition,
                                                  GatewayProperties properties) {
        GatewayProperties.ContextHeadersProperties contextHeaders = properties.getContextHeaders();
        if (!contextHeaders.isEnabled()) {
            return filterSpec;
        }
        GatewayFilterSpec updated = filterSpec;
        for (var entry : definition.buildContextHeaders(
                contextHeaders.getProjectHeaderName(),
                contextHeaders.getRouteHeaderName()
        ).entrySet()) {
            updated = updated.addRequestHeader(entry.getKey(), entry.getValue());
        }
        return updated;
    }

    private UriSpec applyRouteMetadata(UriSpec uriSpec, GatewayRouteDefinition definition) {
        UriSpec updated = uriSpec;
        if (definition.getConnectTimeoutMs() != null) {
            updated = updated.metadata(RouteMetadataUtils.CONNECT_TIMEOUT_ATTR, definition.getConnectTimeoutMs());
        }
        if (definition.getResponseTimeout() != null) {
            updated = updated.metadata(RouteMetadataUtils.RESPONSE_TIMEOUT_ATTR, definition.getResponseTimeout().toMillis());
        }
        return updated;
    }

    private String buildRewritePattern(String pathRoot) {
        return Pattern.quote(pathRoot) + "(?:/(?<segment>.*))?";
    }

    private String buildServiceTarget(String servicePathPrefix) {
        if (servicePathPrefix == null || servicePathPrefix.isBlank() || "/".equals(servicePathPrefix)) {
            return "/${segment}";
        }
        String normalized = servicePathPrefix.startsWith("/") ? servicePathPrefix : "/" + servicePathPrefix;
        normalized = normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
        return normalized + "/${segment}";
    }
}
