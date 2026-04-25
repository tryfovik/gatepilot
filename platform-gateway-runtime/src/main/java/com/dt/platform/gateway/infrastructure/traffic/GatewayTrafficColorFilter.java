package com.dt.platform.gateway.infrastructure.traffic;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * 基于请求属性完成流量染色，并向下游透传流量颜色。
 */
public class GatewayTrafficColorFilter implements WebFilter {

    /**
     * 当前请求生效的流量颜色属性名。
     */
    public static final String TRAFFIC_COLOR_ATTRIBUTE =
            GatewayTrafficColorFilter.class.getName() + ".trafficColor";

    private final GatewayProperties.TrafficColorProperties trafficColorProperties;

    private final GatewayTrafficColorResolver trafficColorResolver;

    /**
     * 创建流量染色过滤器。
     *
     * @param trafficColorProperties 流量染色配置
     */
    public GatewayTrafficColorFilter(GatewayProperties.TrafficColorProperties trafficColorProperties) {
        this(trafficColorProperties, null);
    }

    /**
     * 创建流量染色过滤器。
     *
     * @param trafficColorProperties 流量染色配置
     * @param routeDefinitionLocator 路由定义定位器
     */
    public GatewayTrafficColorFilter(GatewayProperties.TrafficColorProperties trafficColorProperties,
                                     GatewayRouteDefinitionLocator routeDefinitionLocator) {
        this.trafficColorProperties = trafficColorProperties;
        this.trafficColorResolver = new GatewayTrafficColorResolver(trafficColorProperties, routeDefinitionLocator);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!trafficColorProperties.isEnabled()) {
            return chain.filter(exchange);
        }
        String trafficColor = resolveTrafficColor(exchange);
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> headers.set(trafficColorProperties.getHeaderName(), trafficColor))
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
        mutatedExchange.getAttributes().put(TRAFFIC_COLOR_ATTRIBUTE, trafficColor);
        if (trafficColorProperties.isResponseHeaderEnabled()) {
            mutatedExchange.getResponse().beforeCommit(() -> {
                mutatedExchange.getResponse().getHeaders().set(trafficColorProperties.getHeaderName(), trafficColor);
                return Mono.empty();
            });
        }
        return chain.filter(mutatedExchange);
    }

    private String resolveTrafficColor(ServerWebExchange exchange) {
        return trafficColorResolver.resolve(new GatewayTrafficColorResolver.TrafficColorRequest(
                exchange.getRequest().getPath().value(),
                exchange.getRequest().getURI().getRawQuery(),
                headerName -> exchange.getRequest().getHeaders().getFirst(headerName),
                cookieName -> {
                    HttpCookie cookie = exchange.getRequest().getCookies().getFirst(cookieName);
                    return cookie == null ? null : cookie.getValue();
                },
                queryName -> exchange.getRequest().getQueryParams().getFirst(queryName),
                remoteAddress(exchange)
        ));
    }

    private String remoteAddress(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress == null ? null : remoteAddress.getHostString();
    }
}
