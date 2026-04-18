package com.dt.platform.gateway.infrastructure.security;

import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinition;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * 基于路由策略的 HTTP 方法访问过滤器。
 */
public class GatewayMethodAccessFilter implements WebFilter {

    private static final MediaType RESPONSE_CONTENT_TYPE =
            MediaType.parseMediaType("application/json;charset=UTF-8");

    private static final byte[] METHOD_NOT_ALLOWED_RESPONSE =
            "{\"status\":\"fail\",\"code\":405,\"message\":\"Method Not Allowed\"}".getBytes(StandardCharsets.UTF_8);

    private final GatewayRouteDefinitionLocator routeDefinitionLocator;

    /**
     * 创建方法访问过滤器。
     *
     * @param routeDefinitionLocator 路由定义定位器
     */
    public GatewayMethodAccessFilter(GatewayRouteDefinitionLocator routeDefinitionLocator) {
        this.routeDefinitionLocator = routeDefinitionLocator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpMethod requestMethod = exchange.getRequest().getMethod();
        if (requestMethod == null) {
            return chain.filter(exchange);
        }
        String requestPath = exchange.getRequest().getPath().value();
        Optional<GatewayRouteDefinition> apiRoute = routeDefinitionLocator.findApiRoute(requestPath);
        if (apiRoute.isPresent()) {
            return validateRequestMethod(exchange, chain, apiRoute.get().getApiMethods(),
                    apiRoute.get().isApiMethodAllowed(requestMethod.name()));
        }
        Optional<GatewayRouteDefinition> internalRoute = routeDefinitionLocator.findInternalRoute(requestPath);
        if (internalRoute.isPresent()) {
            return validateRequestMethod(exchange, chain, internalRoute.get().getInternalMethods(),
                    internalRoute.get().isInternalMethodAllowed(requestMethod.name()));
        }
        return chain.filter(exchange);
    }

    private Mono<Void> validateRequestMethod(ServerWebExchange exchange,
                                             WebFilterChain chain,
                                             List<String> allowedMethods,
                                             boolean methodAllowed) {
        if (allowedMethods.isEmpty() || methodAllowed) {
            return chain.filter(exchange);
        }
        return writeMethodNotAllowed(exchange, allowedMethods);
    }

    private Mono<Void> writeMethodNotAllowed(ServerWebExchange exchange, List<String> allowedMethods) {
        exchange.getResponse().setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
        exchange.getResponse().getHeaders().setContentType(RESPONSE_CONTENT_TYPE);
        exchange.getResponse().getHeaders().set(HttpHeaders.ALLOW, String.join(", ", allowedMethods));
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(METHOD_NOT_ALLOWED_RESPONSE)));
    }
}
