/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        // 真实路由命中和 LoadBalancer 目标改写由运行态过滤器完成
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
