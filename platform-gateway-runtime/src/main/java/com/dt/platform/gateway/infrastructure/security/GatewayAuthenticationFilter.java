package com.dt.platform.gateway.infrastructure.security;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinition;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import com.getboot.auth.spi.SaTokenWebFluxAuthChecker;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 基于路由策略的网关认证过滤器。
 */
public class GatewayAuthenticationFilter implements WebFilter {

    private final GatewayProperties.AuthProperties authProperties;

    private final GatewayRouteDefinitionLocator routeDefinitionLocator;

    private final SaTokenWebFluxAuthChecker authChecker;

    private final MediaType responseContentType;

    private final byte[] unauthorizedResponseBody;

    private final byte[] forbiddenResponseBody;

    /**
     * 创建认证过滤器。
     *
     * @param authProperties 网关认证配置
     * @param routeDefinitionLocator 路由定义定位器
     * @param authChecker Sa-Token 认证校验器
     */
    public GatewayAuthenticationFilter(GatewayProperties.AuthProperties authProperties,
                                       GatewayRouteDefinitionLocator routeDefinitionLocator,
                                       SaTokenWebFluxAuthChecker authChecker) {
        this.authProperties = authProperties;
        this.routeDefinitionLocator = routeDefinitionLocator;
        this.authChecker = authChecker;
        this.responseContentType = MediaType.parseMediaType(authProperties.getContentType());
        this.unauthorizedResponseBody = buildFailureBody(
                authProperties.getUnauthorizedCode(),
                authProperties.getUnauthorizedMessage()
        );
        this.forbiddenResponseBody = buildFailureBody(
                authProperties.getForbiddenCode(),
                authProperties.getForbiddenMessage()
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!authProperties.isEnabled() || shouldSkipOptions(exchange)) {
            return chain.filter(exchange);
        }
        String requestPath = exchange.getRequest().getPath().value();
        Optional<GatewayRouteDefinition> routeDefinition = routeDefinitionLocator.findApiRoute(requestPath);
        if (routeDefinition.isEmpty()) {
            return chain.filter(exchange);
        }
        GatewayRouteDefinition definition = routeDefinition.get();
        if (!definition.isAuthRequired() || definition.isPublicApiPath(requestPath)) {
            return chain.filter(exchange);
        }
        try {
            authChecker.check();
            return chain.filter(exchange);
        }
        catch (NotLoginException exception) {
            return writeFailure(exchange, authProperties.getUnauthorizedStatus(), unauthorizedResponseBody);
        }
        catch (NotPermissionException exception) {
            return writeFailure(exchange, authProperties.getForbiddenStatus(), forbiddenResponseBody);
        }
    }

    private boolean shouldSkipOptions(ServerWebExchange exchange) {
        return authProperties.isSkipOptionsRequest()
                && "OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name());
    }

    private Mono<Void> writeFailure(ServerWebExchange exchange,
                                    int httpStatus,
                                    byte[] responseBody) {
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(httpStatus));
        exchange.getResponse().getHeaders().setContentType(responseContentType);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(responseBody)));
    }

    private byte[] buildFailureBody(int businessCode, String message) {
        String body = "{\"status\":\"fail\",\"code\":" + businessCode + ",\"message\":\"" + escapeJson(message) + "\"}";
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (current < 0x20) {
                        builder.append(String.format("\\u%04x", (int) current));
                    }
                    else {
                        builder.append(current);
                    }
                }
            }
        }
        return builder.toString();
    }
}
