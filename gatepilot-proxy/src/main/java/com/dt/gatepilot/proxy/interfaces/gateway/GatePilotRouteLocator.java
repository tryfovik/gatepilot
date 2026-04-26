package com.dt.gatepilot.proxy.interfaces.gateway;

import com.dt.gatepilot.proxy.interfaces.web.ProxyHttpConstants;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import reactor.core.publisher.Flux;

/**
 * GatePilot 数据面入口路由定位器。
 */
public class GatePilotRouteLocator implements RouteLocator {

    /**
     * 数据面兜底路由。
     */
    private final Route proxyRoute;

    /**
     * 创建 GatePilot 数据面入口路由定位器。
     */
    public GatePilotRouteLocator() {
        this.proxyRoute = Route.async()
                .id(GatePilotGatewayConstants.PROXY_ROUTE_ID)
                .order(GatePilotGatewayConstants.PROXY_ROUTE_ORDER)
                .predicate(exchange -> proxyPath(exchange.getRequest().getURI().getRawPath()))
                .uri(GatePilotGatewayConstants.PROXY_PLACEHOLDER_URI)
                .build();
    }

    /**
     * 获取 Spring Cloud Gateway 路由。
     *
     * @return SCG 路由流
     */
    @Override
    public Flux<Route> getRoutes() {
        // 真实路由命中和上游选择由 GatePilot 运行态过滤器完成
        return Flux.just(proxyRoute);
    }

    private boolean proxyPath(String path) {
        if (path == null || ProxyHttpConstants.GATEPILOT_API_BASE.equals(path)
                || path.startsWith(ProxyHttpConstants.GATEPILOT_API_PREFIX)) {
            return false;
        }
        return prefixed(path, ProxyHttpConstants.API_PROXY_PREFIX)
                || prefixed(path, ProxyHttpConstants.INTERNAL_PROXY_PREFIX);
    }

    private boolean prefixed(String path, String prefix) {
        return prefix.equals(path) || path.startsWith(prefix + ProxyHttpConstants.PATH_SEPARATOR);
    }
}
