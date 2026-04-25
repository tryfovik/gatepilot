package com.dt.platform.gateway.infrastructure.audit;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.governance.GatewayCircuitBreakerFilter;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinition;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import com.dt.platform.gateway.infrastructure.traffic.GatewayTrafficColorFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * 网关访问审计过滤器。
 */
public class GatewayAccessAuditFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayAccessAuditFilter.class);

    private final GatewayProperties properties;

    private final GatewayRouteDefinitionLocator routeDefinitionLocator;

    private final GatewayAccessAuditRepository auditRepository;

    /**
     * 创建访问审计过滤器。
     *
     * @param properties 网关配置
     * @param routeDefinitionLocator 路由定义定位器
     * @param auditRepository 审计仓库
     */
    public GatewayAccessAuditFilter(GatewayProperties properties,
                                    GatewayRouteDefinitionLocator routeDefinitionLocator,
                                    GatewayAccessAuditRepository auditRepository) {
        this.properties = properties;
        this.routeDefinitionLocator = routeDefinitionLocator;
        this.auditRepository = auditRepository;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.getAudit().isEnabled()) {
            return chain.filter(exchange);
        }
        long startNanos = System.nanoTime();
        return chain.filter(exchange)
                .doOnSuccess(ignored -> record(exchange, startNanos, null))
                .doOnError(exception -> record(exchange, startNanos, exception));
    }

    private void record(ServerWebExchange exchange, long startNanos, Throwable exception) {
        GatewayAccessAuditEvent event = buildEvent(exchange, startNanos, exception);
        auditRepository.save(event);
        if (properties.getAudit().isLogEnabled()) {
            log.info("gateway_access_audit traceId={} clientIp={} method={} path={} routeType={} project={} route={} "
                            + "status={} latencyMs={} trafficColor={} releaseVariant={} upstreamUri={} methodAllowed={} "
                            + "authRequired={} publicPath={} fallback={} outcome={} error={}",
                    event.traceId(),
                    event.clientIp(),
                    event.method(),
                    event.path(),
                    event.routeType(),
                    event.projectKey(),
                    event.routeKey(),
                    event.status(),
                    event.latencyMs(),
                    event.trafficColor(),
                    event.releaseVariant(),
                    event.upstreamUri(),
                    event.methodAllowed(),
                    event.authRequired(),
                    event.publicPath(),
                    event.fallback(),
                    event.outcome(),
                    event.error());
        }
    }

    private GatewayAccessAuditEvent buildEvent(ServerWebExchange exchange, long startNanos, Throwable exception) {
        String requestPath = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod() == null
                ? null
                : exchange.getRequest().getMethod().name();
        Optional<GatewayRouteDefinition> apiRoute = routeDefinitionLocator.findApiRoute(requestPath);
        Optional<GatewayRouteDefinition> internalRoute = apiRoute.isPresent()
                ? Optional.empty()
                : routeDefinitionLocator.findInternalRoute(requestPath);
        GatewayRouteDefinition definition = apiRoute.or(() -> internalRoute).orElse(null);
        String routeType = apiRoute.isPresent() ? "api" : (internalRoute.isPresent() ? "internal" : "none");
        String trafficColor = exchange.getAttribute(GatewayTrafficColorFilter.TRAFFIC_COLOR_ATTRIBUTE);
        GatewayRouteDefinition.ReleaseVariant releaseVariant = apiRoute.isPresent()
                ? findReleaseVariant(apiRoute.get(), trafficColor)
                : null;
        boolean fallback = Boolean.TRUE.equals(exchange.getAttribute(GatewayCircuitBreakerFilter.FALLBACK_ATTRIBUTE));
        Integer status = status(exchange, exception);
        boolean methodAllowed = definition == null
                || ("api".equals(routeType)
                ? definition.isApiMethodAllowed(method)
                : definition.isInternalMethodAllowed(method));
        boolean publicPath = definition != null && "api".equals(routeType) && definition.isPublicApiPath(requestPath);
        boolean authRequired = definition != null && "api".equals(routeType) && definition.isAuthRequired() && !publicPath;
        return new GatewayAccessAuditEvent(
                Instant.now(),
                traceId(exchange),
                clientIp(exchange),
                method,
                requestPath,
                routeType,
                definition == null ? null : definition.getProjectKey(),
                definition == null ? null : definition.getRouteKey(),
                status,
                Math.max(0, (System.nanoTime() - startNanos) / 1_000_000),
                trafficColor,
                releaseVariant == null ? null : releaseVariant.variantKey(),
                upstreamUri(definition, routeType, releaseVariant),
                methodAllowed,
                authRequired,
                publicPath,
                fallback,
                outcome(status, exception, fallback),
                exception == null ? null : exception.getClass().getName()
        );
    }

    private GatewayRouteDefinition.ReleaseVariant findReleaseVariant(GatewayRouteDefinition definition,
                                                                     String trafficColor) {
        if (!StringUtils.hasText(trafficColor)) {
            return null;
        }
        String normalizedTrafficColor = trafficColor.toLowerCase(Locale.ROOT);
        return definition.getReleaseVariants().stream()
                .filter(variant -> variant.matchColors().contains(normalizedTrafficColor))
                .findFirst()
                .orElse(null);
    }

    private URI upstreamUri(GatewayRouteDefinition definition,
                            String routeType,
                            GatewayRouteDefinition.ReleaseVariant releaseVariant) {
        if (definition == null) {
            return null;
        }
        if ("internal".equals(routeType)) {
            return definition.getActuatorUri();
        }
        return releaseVariant == null ? definition.getServiceUri() : releaseVariant.serviceUri();
    }

    private Integer status(ServerWebExchange exchange, Throwable exception) {
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        if (statusCode != null) {
            return statusCode.value();
        }
        return exception == null ? null : 500;
    }

    private String outcome(Integer status, Throwable exception, boolean fallback) {
        if (fallback) {
            return "fallback";
        }
        if (exception != null || (status != null && status >= 500)) {
            return "error";
        }
        if (status != null && status >= 400) {
            return "blocked";
        }
        return "success";
    }

    private String traceId(ServerWebExchange exchange) {
        String traceHeaderName = properties.getAudit().getTraceHeaderName();
        if (!StringUtils.hasText(traceHeaderName)) {
            traceHeaderName = "X-Trace-Id";
        }
        String requestTraceId = exchange.getRequest().getHeaders().getFirst(traceHeaderName);
        if (StringUtils.hasText(requestTraceId)) {
            return requestTraceId;
        }
        String responseTraceId = exchange.getResponse().getHeaders().getFirst(traceHeaderName);
        return StringUtils.hasText(responseTraceId) ? responseTraceId : exchange.getRequest().getId();
    }

    private String clientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress == null ? null : remoteAddress.getHostString();
    }
}
