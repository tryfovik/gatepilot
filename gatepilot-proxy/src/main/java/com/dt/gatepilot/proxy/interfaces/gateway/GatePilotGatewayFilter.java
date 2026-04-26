package com.dt.gatepilot.proxy.interfaces.gateway;

import com.dt.gatepilot.proxy.domain.port.RuntimeAuthChecker;
import com.dt.gatepilot.proxy.domain.port.RuntimeAuthResult;
import com.dt.gatepilot.proxy.domain.port.RuntimeRateLimiter;
import com.dt.gatepilot.proxy.domain.runtime.CircuitBreakerPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.CompiledCircuitBreakerPolicy;
import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRateLimitPolicy;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRateLimitRule;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRetryPolicy;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRoute;
import com.dt.gatepilot.proxy.domain.runtime.CompiledUpstream;
import com.dt.gatepilot.proxy.domain.runtime.ProxyAuditConstants;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRateLimitConstants;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRetryConstants;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitAcquireResult;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitRequest;
import com.dt.gatepilot.proxy.domain.runtime.ReleaseUpstreamResolver;
import com.dt.gatepilot.proxy.domain.runtime.RetryPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessDecision;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessEvaluator;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessRequest;
import com.dt.gatepilot.proxy.domain.runtime.RouteCircuitBreaker;
import com.dt.gatepilot.proxy.domain.runtime.TrafficColorConstants;
import com.dt.gatepilot.proxy.domain.runtime.TrafficColorRequest;
import com.dt.gatepilot.proxy.domain.runtime.TrafficColorResolver;
import com.dt.gatepilot.proxy.infrastructure.loadbalancer.GatePilotLoadBalancerServiceIds;
import com.dt.gatepilot.proxy.interfaces.web.ProxyHttpConstants;
import com.dt.gatepilot.proxy.interfaces.web.ProxyRuntimeAuditRecorder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getboot.web.api.response.ApiResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * GatePilot 数据面治理过滤器。
 */
public class GatePilotGatewayFilter implements GlobalFilter, Ordered {

    /**
     * proxy 当前运行态。
     */
    private final ProxyRuntimeState runtimeState;

    /**
     * 路由访问判断器。
     */
    private final RouteAccessEvaluator accessEvaluator;

    /**
     * 流量染色解析器。
     */
    private final TrafficColorResolver trafficColorResolver;

    /**
     * 熔断策略解析器。
     */
    private final CircuitBreakerPolicyResolver circuitBreakerPolicyResolver;

    /**
     * 路由熔断器。
     */
    private final RouteCircuitBreaker routeCircuitBreaker;

    /**
     * 限流策略解析器。
     */
    private final RateLimitPolicyResolver rateLimitPolicyResolver;

    /**
     * 运行时限流器。
     */
    private final RuntimeRateLimiter runtimeRateLimiter;

    /**
     * 重试策略解析器。
     */
    private final RetryPolicyResolver retryPolicyResolver;

    /**
     * 发布上游解析器。
     */
    private final ReleaseUpstreamResolver releaseUpstreamResolver;

    /**
     * 运行时认证校验器。
     */
    private final RuntimeAuthChecker runtimeAuthChecker;

    /**
     * 运行审计记录器。
     */
    private final ProxyRuntimeAuditRecorder auditRecorder;

    /**
     * SCG 重试过滤器工厂。
     */
    private final RetryGatewayFilterFactory retryGatewayFilterFactory;

    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建 GatePilot 数据面治理过滤器。
     *
     * @param runtimeState proxy 运行态
     * @param accessEvaluator 路由访问判断器
     * @param trafficColorResolver 流量染色解析器
     * @param circuitBreakerPolicyResolver 熔断策略解析器
     * @param routeCircuitBreaker 路由熔断器
     * @param rateLimitPolicyResolver 限流策略解析器
     * @param runtimeRateLimiter 运行时限流器
     * @param retryPolicyResolver 重试策略解析器
     * @param releaseUpstreamResolver 发布上游解析器
     * @param runtimeAuthChecker 运行时认证校验器
     * @param auditRecorder 运行审计记录器
     * @param retryGatewayFilterFactory SCG 重试过滤器工厂
     * @param objectMapper JSON 序列化器
     */
    public GatePilotGatewayFilter(ProxyRuntimeState runtimeState,
                                  RouteAccessEvaluator accessEvaluator,
                                  TrafficColorResolver trafficColorResolver,
                                  CircuitBreakerPolicyResolver circuitBreakerPolicyResolver,
                                  RouteCircuitBreaker routeCircuitBreaker,
                                  RateLimitPolicyResolver rateLimitPolicyResolver,
                                  RuntimeRateLimiter runtimeRateLimiter,
                                  RetryPolicyResolver retryPolicyResolver,
                                  ReleaseUpstreamResolver releaseUpstreamResolver,
                                  RuntimeAuthChecker runtimeAuthChecker,
                                  ProxyRuntimeAuditRecorder auditRecorder,
                                  RetryGatewayFilterFactory retryGatewayFilterFactory,
                                  ObjectMapper objectMapper) {
        this.runtimeState = runtimeState;
        this.accessEvaluator = accessEvaluator;
        this.trafficColorResolver = trafficColorResolver;
        this.circuitBreakerPolicyResolver = circuitBreakerPolicyResolver;
        this.routeCircuitBreaker = routeCircuitBreaker;
        this.rateLimitPolicyResolver = rateLimitPolicyResolver;
        this.runtimeRateLimiter = runtimeRateLimiter;
        this.retryPolicyResolver = retryPolicyResolver;
        this.releaseUpstreamResolver = releaseUpstreamResolver;
        this.runtimeAuthChecker = runtimeAuthChecker;
        this.auditRecorder = auditRecorder;
        this.retryGatewayFilterFactory = retryGatewayFilterFactory;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取过滤器顺序。
     *
     * @return 过滤器顺序
     */
    @Override
    public int getOrder() {
        return GatePilotGatewayConstants.GATEWAY_FILTER_ORDER;
    }

    /**
     * 执行数据面治理并交给 SCG 转发。
     *
     * @param exchange WebFlux 交换上下文
     * @param chain SCG 过滤器链
     * @return 完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!proxyPath(exchange)) {
            return chain.filter(exchange);
        }
        long startNanos = System.nanoTime();
        if (internalRequest(exchange) && !loopbackRequest(exchange)) {
            return statusResponse(
                    exchange,
                    null,
                    null,
                    null,
                    HttpStatus.FORBIDDEN,
                    null,
                    true,
                    false,
                    false,
                    ProxyAuditConstants.OUTCOME_BLOCKED,
                    ProxyAuditConstants.REASON_INTERNAL_ACCESS_DENIED,
                    startNanos
            );
        }
        // 没有运行态就先拒绝，避免空配置接流量
        return runtimeState.current()
                .map(runtime -> handleWithRuntime(runtime, exchange, chain, startNanos))
                .orElseGet(() -> statusResponse(
                        exchange,
                        null,
                        null,
                        null,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        null,
                        true,
                        false,
                        false,
                        ProxyAuditConstants.OUTCOME_BLOCKED,
                        ProxyAuditConstants.REASON_RUNTIME_EMPTY,
                        startNanos
                ));
    }

    private Mono<Void> handleWithRuntime(CompiledProxyRuntime runtime,
                                         ServerWebExchange exchange,
                                         GatewayFilterChain chain,
                                         long startNanos) {
        RouteAccessDecision accessDecision = accessEvaluator.evaluate(runtime, routeAccessRequest(exchange));
        if (!accessDecision.matched()) {
            return statusResponse(
                    exchange,
                    null,
                    null,
                    null,
                    HttpStatus.NOT_FOUND,
                    null,
                    true,
                    false,
                    false,
                    ProxyAuditConstants.OUTCOME_BLOCKED,
                    ProxyAuditConstants.REASON_ROUTE_NOT_MATCHED,
                    startNanos
            );
        }
        if (!accessDecision.methodAllowed()) {
            return methodNotAllowed(exchange, accessDecision, startNanos);
        }
        String trafficColor = trafficColorResolver.resolve(runtime, trafficColorRequest(exchange));
        if (accessDecision.authenticationRequired()) {
            RuntimeAuthResult authResult = runtimeAuthChecker.check();
            if (!authResult.allowed()) {
                return authFailureResponse(exchange, accessDecision.route(), trafficColor, authResult, startNanos);
            }
        }
        String upstreamName = releaseUpstreamResolver.resolve(runtime, accessDecision.route(), trafficColor);
        CompiledUpstream upstream = runtime.getUpstreamsByName().get(upstreamName);
        if (upstream == null || upstream.getEndpoints().isEmpty()) {
            // 发布产物缺上游时直接阻断，避免把请求发到占位地址
            return statusResponse(
                    exchange,
                    accessDecision.route(),
                    upstreamName,
                    null,
                    HttpStatus.BAD_GATEWAY,
                    trafficColor,
                    true,
                    accessDecision.authenticationRequired(),
                    false,
                    ProxyAuditConstants.OUTCOME_ERROR,
                    ProxyAuditConstants.REASON_UPSTREAM_MISSING,
                    startNanos
            );
        }
        Optional<CompiledRateLimitPolicy> rateLimit = rateLimitPolicyResolver.resolve(runtime, accessDecision.route());
        Optional<Mono<Void>> rateLimitRejection = rateLimit
                .flatMap(policy -> rateLimitRejection(policy, rateLimitRequest(exchange), exchange,
                        accessDecision.route(), trafficColor, startNanos));
        if (rateLimitRejection.isPresent()) {
            return rateLimitRejection.get();
        }
        Optional<CompiledCircuitBreakerPolicy> circuitBreaker = circuitBreakerPolicyResolver.resolve(
                runtime, accessDecision.route());
        if (circuitBreaker.isPresent()
                && !routeCircuitBreaker.tryAcquire(circuitBreaker.get(), System.nanoTime())) {
            return circuitBreakerFallback(exchange, accessDecision.route(), upstream.getName(), null, trafficColor,
                    circuitBreaker.get(), startNanos, ProxyAuditConstants.REASON_CIRCUIT_BREAKER_OPEN);
        }
        CompiledRetryPolicy retryPolicy = retryPolicyResolver.resolve(runtime, accessDecision.route())
                .filter(policy -> retryAllowed(exchange, policy))
                .orElse(null);
        ForwardContext context = new ForwardContext(
                accessDecision.route(),
                upstream,
                trafficColor,
                circuitBreaker.orElse(null),
                retryPolicy,
                exchange.getRequest().getURI().getRawPath(),
                exchange.getRequest().getURI().getRawQuery(),
                startNanos
        );
        return forward(exchange, chain, context)
                .doOnSuccess(ignored -> recordForwardResult(exchange, context))
                .onErrorResume(error -> recordForwardError(exchange, context, error));
    }

    private Optional<Mono<Void>> rateLimitRejection(CompiledRateLimitPolicy policy,
                                                   RateLimitRequest request,
                                                   ServerWebExchange exchange,
                                                   CompiledRoute route,
                                                   String trafficColor,
                                                   long startNanos) {
        for (CompiledRateLimitRule rule : policy.matchingRules(request)) {
            String limiterName = rule.limiterName(request);
            RateLimitAcquireResult result = runtimeRateLimiter.tryAcquire(limiterName, rule);
            if (result == RateLimitAcquireResult.REJECTED) {
                return Optional.of(rateLimitFallback(exchange, route, policy, trafficColor, startNanos));
            }
            if (result == RateLimitAcquireResult.UNAVAILABLE) {
                return Optional.of(rateLimitUnavailable(exchange, route, policy, trafficColor, startNanos));
            }
        }
        return Optional.empty();
    }

    private Mono<Void> forward(ServerWebExchange exchange,
                               GatewayFilterChain chain,
                               ForwardContext context) {
        GatewayFilterChain forwardingChain = attemptExchange -> chain.filter(prepareForwardExchange(
                attemptExchange, context));
        if (context.retryPolicy() == null || retryGatewayFilterFactory == null) {
            return forwardingChain.filter(exchange);
        }
        GatewayFilter retryFilter = retryGatewayFilterFactory.apply(retryConfig(context));
        return retryFilter.filter(exchange, forwardingChain);
    }

    private ServerWebExchange prepareForwardExchange(ServerWebExchange exchange, ForwardContext context) {
        URI targetUri = targetUri(context);
        context.setUpstreamUri(targetUri);
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, targetUri);
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .uri(targetUri)
                .headers(headers -> applyRequestHeaders(context.route(), headers, context.trafficColor()))
                .build();
        if (StringUtils.hasText(context.trafficColor())) {
            exchange.getResponse().getHeaders().set(TrafficColorConstants.DEFAULT_HEADER_NAME, context.trafficColor());
        }
        return exchange.mutate().request(request).build();
    }

    private RetryGatewayFilterFactory.RetryConfig retryConfig(ForwardContext context) {
        RetryGatewayFilterFactory.RetryConfig config = new RetryGatewayFilterFactory.RetryConfig();
        config.setRouteId(context.route().getRouteId());
        config.setRetries(context.retryPolicy().getMaxAttempts() - 1);
        config.setMethods(ProxyRetryConstants.DEFAULT_RETRY_METHODS.stream()
                .map(HttpMethod::valueOf)
                .toArray(HttpMethod[]::new));
        HttpStatus[] statuses = context.retryPolicy().getStatuses().stream()
                .map(HttpStatus::resolve)
                .filter(Objects::nonNull)
                .toArray(HttpStatus[]::new);
        if (statuses.length > 0) {
            config.setStatuses(statuses);
        }
        config.setExceptions(IOException.class, TimeoutException.class);
        Duration firstBackoff = context.retryPolicy().getFirstBackoff();
        Duration maxBackoff = context.retryPolicy().getMaxBackoff();
        if (firstBackoff != null && maxBackoff != null && !firstBackoff.isZero()
                && !firstBackoff.isNegative() && !maxBackoff.isNegative()) {
            config.setBackoff(
                    firstBackoff,
                    maxBackoff,
                    GatePilotGatewayConstants.RETRY_BACKOFF_FACTOR,
                    GatePilotGatewayConstants.RETRY_BACKOFF_BASED_ON_PREVIOUS
            );
        }
        return config;
    }

    private void recordForwardResult(ServerWebExchange exchange, ForwardContext context) {
        int status = responseStatus(exchange);
        URI upstreamUri = resolvedUpstreamUri(exchange, context);
        context.setUpstreamUri(upstreamUri);
        recordCircuitBreaker(context.circuitBreaker(), status, context.startNanos());
        auditRecorder.record(
                exchange,
                context.route(),
                context.upstream().getName(),
                upstreamUri,
                status,
                context.trafficColor(),
                true,
                false,
                false,
                outcome(status),
                ProxyAuditConstants.REASON_UPSTREAM_RESPONSE,
                null,
                context.startNanos()
        );
    }

    private Mono<Void> recordForwardError(ServerWebExchange exchange, ForwardContext context, Throwable error) {
        URI upstreamUri = resolvedUpstreamUri(exchange, context);
        context.setUpstreamUri(upstreamUri);
        if (context.circuitBreaker() == null) {
            auditRecorder.record(
                    exchange,
                    context.route(),
                    context.upstream().getName(),
                    upstreamUri,
                    ProxyAuditConstants.DEFAULT_ERROR_STATUS,
                    context.trafficColor(),
                    true,
                    false,
                    false,
                    ProxyAuditConstants.OUTCOME_ERROR,
                    ProxyAuditConstants.REASON_UPSTREAM_ERROR,
                    error,
                    context.startNanos()
            );
            return Mono.error(error);
        }
        long nowNanos = System.nanoTime();
        routeCircuitBreaker.record(
                context.circuitBreaker(),
                true,
                routeCircuitBreaker.slowCall(context.circuitBreaker(), context.startNanos(), nowNanos),
                nowNanos
        );
        return circuitBreakerFallback(
                exchange,
                context.route(),
                context.upstream().getName(),
                upstreamUri,
                context.trafficColor(),
                context.circuitBreaker(),
                context.startNanos(),
                ProxyAuditConstants.REASON_UPSTREAM_ERROR,
                error
        );
    }

    private URI resolvedUpstreamUri(ServerWebExchange exchange, ForwardContext context) {
        URI requestUrl = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        // LoadBalancer 解析后会把真实上游地址写回这个属性
        return requestUrl == null ? context.upstreamUri() : requestUrl;
    }

    private RouteAccessRequest routeAccessRequest(ServerWebExchange exchange) {
        return new RouteAccessRequest(host(exchange), exchange.getRequest().getURI().getRawPath(),
                exchange.getRequest().getMethod().name());
    }

    private TrafficColorRequest trafficColorRequest(ServerWebExchange exchange) {
        return new TrafficColorRequest(
                host(exchange),
                exchange.getRequest().getURI().getRawPath(),
                exchange.getRequest().getURI().getRawQuery(),
                headerName -> exchange.getRequest().getHeaders().getFirst(headerName),
                cookieName -> exchange.getRequest().getCookies().getFirst(cookieName) == null
                        ? null
                        : exchange.getRequest().getCookies().getFirst(cookieName).getValue(),
                queryName -> exchange.getRequest().getQueryParams().getFirst(queryName),
                remoteAddress(exchange)
        );
    }

    private RateLimitRequest rateLimitRequest(ServerWebExchange exchange) {
        return new RateLimitRequest(
                host(exchange),
                headerName -> exchange.getRequest().getHeaders().getFirst(headerName),
                cookieName -> exchange.getRequest().getCookies().getFirst(cookieName) == null
                        ? null
                        : exchange.getRequest().getCookies().getFirst(cookieName).getValue(),
                queryName -> exchange.getRequest().getQueryParams().getFirst(queryName),
                remoteAddress(exchange)
        );
    }

    private boolean retryAllowed(ServerWebExchange exchange, CompiledRetryPolicy retryPolicy) {
        if (retryPolicy == null || !retryPolicy.isEnabled() || retryPolicy.getMaxAttempts() <= 1) {
            return false;
        }
        return ProxyRetryConstants.DEFAULT_RETRY_METHODS.contains(
                Objects.toString(exchange.getRequest().getMethod().name(), "").toUpperCase(Locale.ROOT));
    }

    private URI targetUri(ForwardContext context) {
        String path = targetPath(context.originalPath(), context.route());
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme(ProxyHttpConstants.SCHEME_LOAD_BALANCER)
                .host(GatePilotLoadBalancerServiceIds.fromUpstreamName(context.upstream().getName()))
                .path(path);
        if (StringUtils.hasText(context.rawQuery())) {
            builder.query(context.rawQuery());
        }
        return builder.build(true).toUri();
    }

    private void applyRequestHeaders(CompiledRoute route, HttpHeaders headers, String trafficColor) {
        List<String> removeHeaders = headers.keySet().stream()
                .filter(header -> skipRequestHeader(route, header))
                .toList();
        removeHeaders.forEach(headers::remove);
        route.getAddHeaders().forEach(headers::set);
        if (StringUtils.hasText(trafficColor)) {
            headers.set(TrafficColorConstants.DEFAULT_HEADER_NAME, trafficColor);
        }
    }

    private boolean skipRequestHeader(CompiledRoute route, String name) {
        if (hopByHop(name)) {
            return true;
        }
        return route.getRemoveHeaders().stream()
                .filter(Objects::nonNull)
                .anyMatch(header -> header.equalsIgnoreCase(name));
    }

    private boolean hopByHop(String name) {
        return name == null || ProxyHttpConstants.HOP_BY_HOP_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }

    private String targetPath(String requestPath, CompiledRoute route) {
        String suffix = suffix(requestPath, route.getPathPrefix());
        if (StringUtils.hasText(route.getRewritePathPrefix())) {
            return joinPath(route.getRewritePathPrefix(), suffix);
        }
        if (Boolean.TRUE.equals(route.getStripPrefix())) {
            return suffix.isEmpty() ? ProxyHttpConstants.ROOT_PATH : suffix;
        }
        return requestPath;
    }

    private String suffix(String requestPath, String prefix) {
        if (requestPath == null || prefix == null || !requestPath.startsWith(prefix)) {
            return "";
        }
        String suffix = requestPath.substring(prefix.length());
        return suffix.isEmpty() ? "" : (suffix.startsWith(ProxyHttpConstants.PATH_SEPARATOR)
                ? suffix
                : ProxyHttpConstants.PATH_SEPARATOR + suffix);
    }

    private String joinPath(String prefix, String suffix) {
        String normalizedPrefix = prefix.endsWith(ProxyHttpConstants.PATH_SEPARATOR) && prefix.length() > 1
                ? prefix.substring(0, prefix.length() - 1)
                : prefix;
        if (!StringUtils.hasText(suffix) || ProxyHttpConstants.ROOT_PATH.equals(suffix)) {
            return normalizedPrefix;
        }
        return normalizedPrefix + (suffix.startsWith(ProxyHttpConstants.PATH_SEPARATOR)
                ? suffix
                : ProxyHttpConstants.PATH_SEPARATOR + suffix);
    }

    private Mono<Void> methodNotAllowed(ServerWebExchange exchange,
                                        RouteAccessDecision decision,
                                        long startNanos) {
        if (!decision.allowedMethods().isEmpty()) {
            exchange.getResponse().getHeaders().setAllow(new HashSet<>(decision.allowedMethods().stream()
                    .map(HttpMethod::valueOf)
                    .toList()));
        }
        return statusResponse(
                exchange,
                decision.route(),
                decision.route().getUpstreamName(),
                null,
                HttpStatus.METHOD_NOT_ALLOWED,
                null,
                false,
                false,
                false,
                ProxyAuditConstants.OUTCOME_BLOCKED,
                ProxyAuditConstants.REASON_METHOD_NOT_ALLOWED,
                startNanos
        );
    }

    private Mono<Void> authFailureResponse(ServerWebExchange exchange,
                                           CompiledRoute route,
                                           String trafficColor,
                                           RuntimeAuthResult authResult,
                                           long startNanos) {
        return jsonResponse(
                exchange,
                route,
                route.getUpstreamName(),
                null,
                HttpStatusCode.valueOf(authResult.status()),
                trafficColor,
                true,
                true,
                false,
                ProxyAuditConstants.OUTCOME_BLOCKED,
                authResult.reason(),
                authResult.error(),
                MediaType.APPLICATION_JSON,
                ApiResponse.fail(authResult.code(), authResult.message()),
                startNanos
        );
    }

    private Mono<Void> circuitBreakerFallback(ServerWebExchange exchange,
                                              CompiledRoute route,
                                              String upstreamName,
                                              URI upstreamUri,
                                              String trafficColor,
                                              CompiledCircuitBreakerPolicy circuitBreaker,
                                              long startNanos,
                                              String reason) {
        return circuitBreakerFallback(exchange, route, upstreamName, upstreamUri, trafficColor, circuitBreaker,
                startNanos, reason, null);
    }

    private Mono<Void> circuitBreakerFallback(ServerWebExchange exchange,
                                              CompiledRoute route,
                                              String upstreamName,
                                              URI upstreamUri,
                                              String trafficColor,
                                              CompiledCircuitBreakerPolicy circuitBreaker,
                                              long startNanos,
                                              String reason,
                                              Throwable error) {
        return jsonResponse(
                exchange,
                route,
                upstreamName,
                upstreamUri,
                HttpStatusCode.valueOf(circuitBreaker.getFallbackStatus()),
                trafficColor,
                true,
                false,
                true,
                ProxyAuditConstants.OUTCOME_FALLBACK,
                reason,
                error,
                contentType(circuitBreaker.getContentType()),
                ApiResponse.fail(circuitBreaker.getFallbackCode(), circuitBreaker.getFallbackMessage()),
                startNanos
        );
    }

    private Mono<Void> rateLimitFallback(ServerWebExchange exchange,
                                         CompiledRoute route,
                                         CompiledRateLimitPolicy rateLimit,
                                         String trafficColor,
                                         long startNanos) {
        return jsonResponse(
                exchange,
                route,
                route.getUpstreamName(),
                null,
                HttpStatusCode.valueOf(rateLimit.getRejectStatus()),
                trafficColor,
                true,
                false,
                false,
                ProxyAuditConstants.OUTCOME_BLOCKED,
                ProxyAuditConstants.REASON_RATE_LIMITED,
                null,
                contentType(rateLimit.getContentType()),
                ApiResponse.fail(rateLimit.getRejectCode(), rateLimit.getRejectMessage()),
                startNanos
        );
    }

    private Mono<Void> rateLimitUnavailable(ServerWebExchange exchange,
                                            CompiledRoute route,
                                            CompiledRateLimitPolicy rateLimit,
                                            String trafficColor,
                                            long startNanos) {
        return jsonResponse(
                exchange,
                route,
                route.getUpstreamName(),
                null,
                HttpStatusCode.valueOf(ProxyRateLimitConstants.UNAVAILABLE_STATUS),
                trafficColor,
                true,
                false,
                false,
                ProxyAuditConstants.OUTCOME_ERROR,
                ProxyAuditConstants.REASON_RATE_LIMITER_UNAVAILABLE,
                null,
                contentType(rateLimit.getContentType()),
                ApiResponse.fail(
                        ProxyRateLimitConstants.UNAVAILABLE_CODE,
                        ProxyRateLimitConstants.UNAVAILABLE_MESSAGE
                ),
                startNanos
        );
    }

    private Mono<Void> statusResponse(ServerWebExchange exchange,
                                      CompiledRoute route,
                                      String upstreamName,
                                      URI upstreamUri,
                                      HttpStatusCode statusCode,
                                      String trafficColor,
                                      boolean methodAllowed,
                                      boolean authenticationRequired,
                                      boolean fallback,
                                      String outcome,
                                      String reason,
                                      long startNanos) {
        exchange.getResponse().setStatusCode(statusCode);
        if (StringUtils.hasText(trafficColor)) {
            exchange.getResponse().getHeaders().set(TrafficColorConstants.DEFAULT_HEADER_NAME, trafficColor);
        }
        return exchange.getResponse()
                .setComplete()
                .doOnSuccess(ignored -> auditRecorder.record(
                        exchange,
                        route,
                        upstreamName,
                        upstreamUri,
                        statusCode.value(),
                        trafficColor,
                        methodAllowed,
                        authenticationRequired,
                        fallback,
                        outcome,
                        reason,
                        null,
                        startNanos
                ));
    }

    private Mono<Void> jsonResponse(ServerWebExchange exchange,
                                    CompiledRoute route,
                                    String upstreamName,
                                    URI upstreamUri,
                                    HttpStatusCode statusCode,
                                    String trafficColor,
                                    boolean methodAllowed,
                                    boolean authenticationRequired,
                                    boolean fallback,
                                    String outcome,
                                    String reason,
                                    Throwable error,
                                    MediaType contentType,
                                    Object body,
                                    long startNanos) {
        exchange.getResponse().setStatusCode(statusCode);
        exchange.getResponse().getHeaders().setContentType(contentType);
        if (StringUtils.hasText(trafficColor)) {
            exchange.getResponse().getHeaders().set(TrafficColorConstants.DEFAULT_HEADER_NAME, trafficColor);
        }
        DataBuffer dataBuffer = exchange.getResponse().bufferFactory().wrap(responseBody(body));
        return exchange.getResponse()
                .writeWith(Mono.just(dataBuffer))
                .doOnSuccess(ignored -> auditRecorder.record(
                        exchange,
                        route,
                        upstreamName,
                        upstreamUri,
                        statusCode.value(),
                        trafficColor,
                        methodAllowed,
                        authenticationRequired,
                        fallback,
                        outcome,
                        reason,
                        error,
                        startNanos
                ));
    }

    private byte[] responseBody(Object body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException exception) {
            return GatePilotGatewayConstants.JSON_SERIALIZE_ERROR_BODY.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private void recordCircuitBreaker(CompiledCircuitBreakerPolicy circuitBreaker, int statusCode, long startNanos) {
        if (circuitBreaker == null) {
            return;
        }
        long nowNanos = System.nanoTime();
        routeCircuitBreaker.record(
                circuitBreaker,
                routeCircuitBreaker.failureStatus(circuitBreaker, statusCode),
                routeCircuitBreaker.slowCall(circuitBreaker, startNanos, nowNanos),
                nowNanos
        );
    }

    private int responseStatus(ServerWebExchange exchange) {
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        return statusCode == null ? HttpStatus.OK.value() : statusCode.value();
    }

    private String outcome(int status) {
        if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            return ProxyAuditConstants.OUTCOME_ERROR;
        }
        if (status >= HttpStatus.BAD_REQUEST.value()) {
            return ProxyAuditConstants.OUTCOME_BLOCKED;
        }
        return ProxyAuditConstants.OUTCOME_SUCCESS;
    }

    private MediaType contentType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException exception) {
            return MediaType.APPLICATION_JSON;
        }
    }

    private boolean proxyPath(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getRawPath();
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

    private boolean internalRequest(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getRawPath();
        return prefixed(path, ProxyHttpConstants.INTERNAL_PROXY_PREFIX);
    }

    private boolean loopbackRequest(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null) {
            return false;
        }
        InetAddress address = remoteAddress.getAddress();
        if (address != null) {
            return address.isLoopbackAddress();
        }
        return ProxyHttpConstants.LOCALHOST.equalsIgnoreCase(remoteAddress.getHostString());
    }

    private String host(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst(HttpHeaders.HOST);
    }

    private String remoteAddress(ServerWebExchange exchange) {
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        if (address == null) {
            return null;
        }
        return address.getAddress() == null ? address.getHostString() : address.getAddress().getHostAddress();
    }

    private static class ForwardContext {

        /**
         * 已命中路由。
         */
        private final CompiledRoute route;

        /**
         * 已命中上游。
         */
        private final CompiledUpstream upstream;

        /**
         * 流量颜色。
         */
        private final String trafficColor;

        /**
         * 熔断策略。
         */
        private final CompiledCircuitBreakerPolicy circuitBreaker;

        /**
         * 重试策略。
         */
        private final CompiledRetryPolicy retryPolicy;

        /**
         * 原始请求路径。
         */
        private final String originalPath;

        /**
         * 原始请求查询串。
         */
        private final String rawQuery;

        /**
         * 请求开始时间。
         */
        private final long startNanos;

        /**
         * 最近一次上游地址。
         */
        private final AtomicReference<URI> upstreamUri = new AtomicReference<>();

        ForwardContext(CompiledRoute route,
                       CompiledUpstream upstream,
                       String trafficColor,
                       CompiledCircuitBreakerPolicy circuitBreaker,
                       CompiledRetryPolicy retryPolicy,
                       String originalPath,
                       String rawQuery,
                       long startNanos) {
            this.route = route;
            this.upstream = upstream;
            this.trafficColor = trafficColor;
            this.circuitBreaker = circuitBreaker;
            this.retryPolicy = retryPolicy;
            this.originalPath = originalPath;
            this.rawQuery = rawQuery;
            this.startNanos = startNanos;
        }

        private CompiledRoute route() {
            return route;
        }

        private CompiledUpstream upstream() {
            return upstream;
        }

        private String trafficColor() {
            return trafficColor;
        }

        private CompiledCircuitBreakerPolicy circuitBreaker() {
            return circuitBreaker;
        }

        private CompiledRetryPolicy retryPolicy() {
            return retryPolicy;
        }

        private String originalPath() {
            return originalPath;
        }

        private String rawQuery() {
            return rawQuery;
        }

        private long startNanos() {
            return startNanos;
        }

        private URI upstreamUri() {
            return upstreamUri.get();
        }

        private void setUpstreamUri(URI upstreamUri) {
            this.upstreamUri.set(upstreamUri);
        }
    }
}
