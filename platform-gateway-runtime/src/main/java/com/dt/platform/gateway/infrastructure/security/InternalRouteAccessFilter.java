package com.dt.platform.gateway.infrastructure.security;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * 内部运维入口访问保护过滤器。
 *
 * <p>内部路由固定只允许网关所在服务器本机访问，不再依赖共享密钥头。</p>
 */
public class InternalRouteAccessFilter implements WebFilter {

    private static final MediaType RESPONSE_CONTENT_TYPE =
            MediaType.parseMediaType("application/json;charset=UTF-8");

    /**
     * 未授权响应体。
     */
    private static final byte[] FORBIDDEN_RESPONSE =
            "{\"status\":\"fail\",\"code\":403,\"message\":\"Forbidden\"}".getBytes(StandardCharsets.UTF_8);

    /**
     * 网关配置。
     */
    private final GatewayProperties properties;

    /**
     * 创建内部访问保护过滤器。
     *
     * @param properties 网关配置
     */
    public InternalRouteAccessFilter(GatewayProperties properties) {
        this.properties = properties;
    }

    /**
     * 校验内部入口访问规则。
     *
     * @param exchange 当前请求
     * @param chain 过滤器链
     * @return 执行结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (requiresValidation(exchange) && !isLoopbackRequest(exchange)) {
            return writeForbidden(exchange);
        }
        return chain.filter(exchange);
    }

    /**
     * 判断当前请求是否需要校验内部访问头。
     *
     * @param exchange 当前请求
     * @return 是否需要校验
     */
    private boolean requiresValidation(ServerWebExchange exchange) {
        String internalPrefix = normalizePrefix(properties.getInternalPrefix());
        if (!StringUtils.hasText(internalPrefix)) {
            return false;
        }
        String requestPath = exchange.getRequest().getPath().value();
        return requestPath.equals(internalPrefix) || requestPath.startsWith(internalPrefix + "/");
    }

    /**
     * 判断当前请求是否来自服务器本机回环地址。
     *
     * @param exchange 当前请求
     * @return 是否来自本机
     */
    private boolean isLoopbackRequest(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null) {
            return false;
        }
        InetAddress address = remoteAddress.getAddress();
        if (address != null) {
            return address.isLoopbackAddress();
        }
        String hostString = remoteAddress.getHostString();
        return "localhost".equalsIgnoreCase(hostString);
    }

    /**
     * 输出拒绝访问响应。
     *
     * @param exchange 当前请求
     * @return 响应结果
     */
    private Mono<Void> writeForbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(RESPONSE_CONTENT_TYPE);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(FORBIDDEN_RESPONSE)));
    }

    /**
     * 规范化前缀路径。
     *
     * @param prefix 原始前缀
     * @return 规范化后的前缀
     */
    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
