package com.dt.platform.gateway.infrastructure.route;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关路由装配配置。
 */
@Configuration
public class GatewayRouteConfiguration {

    /**
     * 注册平台网关路由。
     *
     * @param builder 路由构建器
     * @param properties 网关配置
     * @return 路由定位器
     */
    @Bean
    public RouteLocator gatewayRouteLocator(RouteLocatorBuilder builder,
                                            GatewayProperties properties) {
        RouteLocatorBuilder.Builder routes = builder.routes();
        properties.getUpstreams().forEach((upstreamKey, upstream) -> {
            registerApiRoute(routes, buildRouteId(upstreamKey, "api"), properties.getApiPrefix(), upstream);
            registerActuatorRoute(routes, buildRouteId(upstreamKey, "actuator"), properties.getInternalPrefix(), upstream);
        });
        return routes.build();
    }

    /**
     * 构建稳定的网关路由标识。
     *
     * @param upstreamKey 上游配置键
     * @param routeType 路由类型
     * @return 路由标识
     */
    private String buildRouteId(String upstreamKey, String routeType) {
        String normalized = upstreamKey == null ? "upstream" : upstreamKey.trim().replaceAll("[^a-zA-Z0-9-]", "-");
        return normalized + "-" + routeType;
    }

    /**
     * 注册对外业务路由。
     *
     * @param routes 路由构建器
     * @param routeId 路由标识
     * @param routePrefix 对外前缀
     * @param upstream 上游配置
     */
    private void registerApiRoute(RouteLocatorBuilder.Builder routes,
                                  String routeId,
                                  String routePrefix,
                                  GatewayProperties.UpstreamProperties upstream) {
        if (!upstream.isEnabled() || !upstream.isApiEnabled()) {
            return;
        }
        String normalizedPrefix = normalizePrefix(routePrefix);
        String routeSegment = normalizeRouteSegment(upstream.getRouteSegment());
        String pathPattern = normalizedPrefix + "/" + routeSegment + "/**";
        String rewritePattern = normalizedPrefix + "/" + routeSegment + "/(?<segment>.*)";
        String rewriteTarget = normalizeServiceTarget(upstream.getServicePathPrefix());
        routes.route(routeId, route -> route.path(pathPattern)
                .filters(filter -> filter.rewritePath(rewritePattern, rewriteTarget))
                .uri(upstream.getServiceUri().toString()));
    }

    /**
     * 注册内部运维路由。
     *
     * @param routes 路由构建器
     * @param routeId 路由标识
     * @param routePrefix 内部前缀
     * @param upstream 上游配置
     */
    private void registerActuatorRoute(RouteLocatorBuilder.Builder routes,
                                       String routeId,
                                       String routePrefix,
                                       GatewayProperties.UpstreamProperties upstream) {
        if (!upstream.isEnabled() || !upstream.isActuatorEnabled()) {
            return;
        }
        String normalizedPrefix = normalizePrefix(routePrefix);
        String routeSegment = normalizeRouteSegment(upstream.getRouteSegment());
        String pathPattern = normalizedPrefix + "/" + routeSegment + "/**";
        String rewritePattern = normalizedPrefix + "/" + routeSegment + "/(?<segment>.*)";
        routes.route(routeId, route -> route.path(pathPattern)
                .filters(filter -> filter.rewritePath(rewritePattern, "/${segment}"))
                .uri(upstream.getActuatorUri().toString()));
    }

    /**
     * 规范化前缀路径。
     *
     * @param prefix 原始前缀
     * @return 规范化后的前缀
     */
    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    /**
     * 规范化路由分段。
     *
     * @param routeSegment 原始路由分段
     * @return 规范化后的分段
     */
    private String normalizeRouteSegment(String routeSegment) {
        if (routeSegment == null || routeSegment.isBlank()) {
            throw new IllegalArgumentException("gateway route segment must not be blank");
        }
        String normalized = routeSegment.startsWith("/") ? routeSegment.substring(1) : routeSegment;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    /**
     * 规范化服务目标路径。
     *
     * @param servicePathPrefix 原始服务路径前缀
     * @return 重写目标表达式
     */
    private String normalizeServiceTarget(String servicePathPrefix) {
        if (servicePathPrefix == null || servicePathPrefix.isBlank() || "/".equals(servicePathPrefix)) {
            return "/${segment}";
        }
        String normalized = servicePathPrefix.startsWith("/") ? servicePathPrefix : "/" + servicePathPrefix;
        normalized = normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
        return normalized + "/${segment}";
    }
}
