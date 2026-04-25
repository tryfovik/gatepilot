package com.dt.gatepilot.proxy.interfaces.web;

import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.proxy.domain.port.RuntimeRateLimiter;
import com.dt.gatepilot.proxy.domain.runtime.CircuitBreakerPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.CompiledCircuitBreakerPolicy;
import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRateLimitPolicy;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRateLimitRule;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRoute;
import com.dt.gatepilot.proxy.domain.runtime.CompiledUpstream;
import com.dt.gatepilot.proxy.domain.runtime.ProxyAuditConstants;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRateLimitConstants;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitAcquireResult;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitRequest;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessDecision;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessEvaluator;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessRequest;
import com.dt.gatepilot.proxy.domain.runtime.RouteCircuitBreaker;
import com.dt.gatepilot.proxy.domain.runtime.TrafficColorRequest;
import com.dt.gatepilot.proxy.domain.runtime.TrafficColorConstants;
import com.dt.gatepilot.proxy.domain.runtime.TrafficColorResolver;
import com.getboot.web.api.response.ApiResponse;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * GatePilot proxy WebFlux 入口处理器。
 */
public class GatePilotProxyHandler {

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
     * 运行审计记录器。
     */
    private final ProxyRuntimeAuditRecorder auditRecorder;

    /**
     * 上游 HTTP 客户端。
     */
    private final WebClient webClient;

    /**
     * 创建 proxy WebFlux 入口处理器。
     *
     * @param runtimeState proxy 运行态
     * @param accessEvaluator 路由访问判断器
     * @param trafficColorResolver 流量染色解析器
     * @param circuitBreakerPolicyResolver 熔断策略解析器
     * @param routeCircuitBreaker 路由熔断器
     * @param rateLimitPolicyResolver 限流策略解析器
     * @param runtimeRateLimiter 运行时限流器
     * @param auditRecorder 运行审计记录器
     * @param webClient WebClient
     */
    public GatePilotProxyHandler(ProxyRuntimeState runtimeState,
                                 RouteAccessEvaluator accessEvaluator,
                                 TrafficColorResolver trafficColorResolver,
                                 CircuitBreakerPolicyResolver circuitBreakerPolicyResolver,
                                 RouteCircuitBreaker routeCircuitBreaker,
                                 RateLimitPolicyResolver rateLimitPolicyResolver,
                                 RuntimeRateLimiter runtimeRateLimiter,
                                 ProxyRuntimeAuditRecorder auditRecorder,
                                 WebClient webClient) {
        this.runtimeState = runtimeState;
        this.accessEvaluator = accessEvaluator;
        this.trafficColorResolver = trafficColorResolver;
        this.circuitBreakerPolicyResolver = circuitBreakerPolicyResolver;
        this.routeCircuitBreaker = routeCircuitBreaker;
        this.rateLimitPolicyResolver = rateLimitPolicyResolver;
        this.runtimeRateLimiter = runtimeRateLimiter;
        this.auditRecorder = auditRecorder;
        this.webClient = webClient;
    }

    /**
     * 处理一次业务转发请求。
     *
     * @param request WebFlux 请求
     * @return WebFlux 响应
     */
    public Mono<ServerResponse> handle(ServerRequest request) {
        long startNanos = System.nanoTime();
        // 没有运行态就先拒绝，避免空配置接流量
        return runtimeState.current()
                .map(runtime -> handleWithRuntime(runtime, request, startNanos))
                .orElseGet(() -> statusResponse(
                        request,
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

    /**
     * 使用当前运行态处理请求。
     *
     * @param runtime 当前运行态
     * @param request WebFlux 请求
     * @param startNanos 请求开始时间
     * @return WebFlux 响应
     */
    private Mono<ServerResponse> handleWithRuntime(CompiledProxyRuntime runtime,
                                                   ServerRequest request,
                                                   long startNanos) {
        // 热路径先做路由和访问判断，后面只处理已命中的路由
        RouteAccessDecision accessDecision = accessEvaluator.evaluate(runtime, routeAccessRequest(request));
        if (!accessDecision.matched()) {
            return statusResponse(
                    request,
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
            return methodNotAllowed(request, accessDecision, startNanos);
        }
        if (accessDecision.authenticationRequired()) {
            // getboot-auth 还没接入前，先按策略拒绝需要认证的请求
            return statusResponse(
                    request,
                    accessDecision.route(),
                    accessDecision.route().getUpstreamName(),
                    null,
                    HttpStatus.UNAUTHORIZED,
                    null,
                    false,
                    true,
                    false,
                    ProxyAuditConstants.OUTCOME_BLOCKED,
                    ProxyAuditConstants.REASON_AUTHENTICATION_REQUIRED,
                    startNanos
            );
        }
        CompiledUpstream upstream = runtime.getUpstreamsByName().get(accessDecision.route().getUpstreamName());
        if (upstream == null || upstream.getEndpoints().isEmpty()) {
            // 上游缺失说明发布产物不可用，不能继续转发
            return statusResponse(
                    request,
                    accessDecision.route(),
                    accessDecision.route().getUpstreamName(),
                    null,
                    HttpStatus.BAD_GATEWAY,
                    null,
                    true,
                    false,
                    false,
                    ProxyAuditConstants.OUTCOME_ERROR,
                    ProxyAuditConstants.REASON_UPSTREAM_MISSING,
                    startNanos
            );
        }
        // 染色结果会写入审计，放在限流和熔断前先算出来
        String trafficColor = trafficColorResolver.resolve(runtime, trafficColorRequest(request));
        Optional<CompiledRateLimitPolicy> rateLimit = rateLimitPolicyResolver.resolve(runtime, accessDecision.route());
        Optional<Mono<ServerResponse>> rateLimitRejection = rateLimit
                .flatMap(policy -> rateLimitRejection(policy, rateLimitRequest(request), request,
                        accessDecision.route(), trafficColor, startNanos));
        if (rateLimitRejection.isPresent()) {
            return rateLimitRejection.get();
        }
        Optional<CompiledCircuitBreakerPolicy> circuitBreaker = circuitBreakerPolicyResolver.resolve(
                runtime, accessDecision.route());
        if (circuitBreaker.isPresent()
                && !routeCircuitBreaker.tryAcquire(circuitBreaker.get(), System.nanoTime())) {
            return circuitBreakerFallback(request, accessDecision.route(), null, trafficColor, circuitBreaker.get(),
                    startNanos, ProxyAuditConstants.REASON_CIRCUIT_BREAKER_OPEN);
        }
        URI targetUri = targetUri(request, accessDecision.route(), upstream);
        return forward(request, accessDecision.route(), targetUri, trafficColor, circuitBreaker.orElse(null),
                startNanos);
    }

    /**
     * 创建路由访问判断请求。
     *
     * @param request WebFlux 请求
     * @return 路由访问判断请求
     */
    private RouteAccessRequest routeAccessRequest(ServerRequest request) {
        // 访问判断只需要路由命中、路径和方法
        return new RouteAccessRequest(host(request), request.uri().getRawPath(), request.methodName());
    }

    /**
     * 创建流量染色请求。
     *
     * @param request WebFlux 请求
     * @return 流量染色请求
     */
    private TrafficColorRequest trafficColorRequest(ServerRequest request) {
        // 用读取器懒取值，避免为了染色提前解析整包请求
        return new TrafficColorRequest(
                host(request),
                request.uri().getRawPath(),
                request.uri().getRawQuery(),
                headerName -> request.headers().firstHeader(headerName),
                cookieName -> request.cookies().getFirst(cookieName) == null
                        ? null
                        : request.cookies().getFirst(cookieName).getValue(),
                queryName -> request.queryParam(queryName).orElse(null),
                remoteAddress(request)
        );
    }

    /**
     * 创建限流请求。
     *
     * @param request WebFlux 请求
     * @return 限流请求
     */
    private RateLimitRequest rateLimitRequest(ServerRequest request) {
        // 限流参数只按需要读取，不提前展开请求
        return new RateLimitRequest(
                host(request),
                headerName -> request.headers().firstHeader(headerName),
                cookieName -> request.cookies().getFirst(cookieName) == null
                        ? null
                        : request.cookies().getFirst(cookieName).getValue(),
                queryName -> request.queryParam(queryName).orElse(null),
                remoteAddress(request)
        );
    }

    /**
     * 判断是否需要返回限流响应。
     *
     * @param policy 限流策略
     * @param request 限流请求
     * @param serverRequest WebFlux 请求
     * @param route 已命中路由
     * @param trafficColor 流量颜色
     * @param startNanos 请求开始时间
     * @return 限流响应
     */
    private Optional<Mono<ServerResponse>> rateLimitRejection(CompiledRateLimitPolicy policy,
                                                              RateLimitRequest request,
                                                              ServerRequest serverRequest,
                                                              CompiledRoute route,
                                                              String trafficColor,
                                                              long startNanos) {
        for (CompiledRateLimitRule rule : policy.matchingRules(request)) {
            String limiterName = rule.limiterName(request);
            RateLimitAcquireResult result = runtimeRateLimiter.tryAcquire(limiterName, rule);
            if (result == RateLimitAcquireResult.REJECTED) {
                return Optional.of(rateLimitFallback(serverRequest, route, policy, trafficColor, startNanos));
            }
            if (result == RateLimitAcquireResult.UNAVAILABLE) {
                return Optional.of(rateLimitUnavailable(serverRequest, route, policy, trafficColor, startNanos));
            }
        }
        return Optional.empty();
    }

    /**
     * 创建方法不允许响应。
     *
     * @param request WebFlux 请求
     * @param decision 路由访问判断结果
     * @param startNanos 请求开始时间
     * @return WebFlux 响应
     */
    private Mono<ServerResponse> methodNotAllowed(ServerRequest request,
                                                  RouteAccessDecision decision,
                                                  long startNanos) {
        return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers -> {
                    // 只有明确方法白名单时才回 Allow
                    if (!decision.allowedMethods().isEmpty()) {
                        headers.setAllow(new HashSet<>(decision.allowedMethods().stream()
                                .map(org.springframework.http.HttpMethod::valueOf)
                                .toList()));
                    }
                })
                .build()
                .doOnSuccess(response -> auditRecorder.record(
                        request,
                        decision.route(),
                        decision.route().getUpstreamName(),
                        null,
                        HttpStatus.METHOD_NOT_ALLOWED.value(),
                        null,
                        false,
                        false,
                        false,
                        ProxyAuditConstants.OUTCOME_BLOCKED,
                        ProxyAuditConstants.REASON_METHOD_NOT_ALLOWED,
                        null,
                        startNanos
                ));
    }

    /**
     * 转发请求到上游。
     *
     * @param request WebFlux 请求
     * @param route 已命中路由
     * @param targetUri 上游目标地址
     * @param trafficColor 流量颜色
     * @param circuitBreaker 熔断策略
     * @param startNanos 请求开始时间
     * @return WebFlux 响应
     */
    private Mono<ServerResponse> forward(ServerRequest request,
                                         CompiledRoute route,
                                         URI targetUri,
                                         String trafficColor,
                                         CompiledCircuitBreakerPolicy circuitBreaker,
                                         long startNanos) {
        // 请求体保持流式透传，避免网关把大包读进内存
        Flux<DataBuffer> body = request.bodyToFlux(DataBuffer.class);
        return webClient.method(request.method())
                .uri(targetUri)
                .headers(headers -> applyRequestHeaders(request, route, headers, trafficColor))
                .body(BodyInserters.fromDataBuffers(body))
                .exchangeToMono(response -> {
                    int status = response.statusCode().value();
                    recordCircuitBreaker(circuitBreaker, status, startNanos);
                    auditRecorder.record(
                            request,
                            route,
                            route.getUpstreamName(),
                            targetUri,
                            status,
                            trafficColor,
                            true,
                            false,
                            false,
                            outcome(status),
                            ProxyAuditConstants.REASON_UPSTREAM_RESPONSE,
                            null,
                            startNanos
                    );
                    return toServerResponse(response, trafficColor);
                })
                .onErrorResume(error -> recordForwardError(
                        request,
                        route,
                        targetUri,
                        trafficColor,
                        circuitBreaker,
                        startNanos,
                        error
                ));
    }

    /**
     * 将上游响应转换为客户端响应。
     *
     * @param response 上游响应
     * @param trafficColor 流量颜色
     * @return WebFlux 响应
     */
    private Mono<ServerResponse> toServerResponse(ClientResponse response, String trafficColor) {
        return ServerResponse.status(response.statusCode())
                .headers(headers -> {
                    // 先复制上游响应头，再补回网关计算出的染色结果
                    copyResponseHeaders(response, headers);
                    if (StringUtils.hasText(trafficColor)) {
                        headers.set(TrafficColorConstants.DEFAULT_HEADER_NAME, trafficColor);
                    }
                })
                .body(BodyInserters.fromDataBuffers(response.bodyToFlux(DataBuffer.class)));
    }

    /**
     * 写入上游请求头。
     *
     * @param request WebFlux 请求
     * @param route 已命中路由
     * @param targetHeaders 上游请求头
     * @param trafficColor 流量颜色
     */
    private void applyRequestHeaders(ServerRequest request,
                                     CompiledRoute route,
                                     HttpHeaders targetHeaders,
                                     String trafficColor) {
        // 客户端请求头先按代理规则复制，再套路由级改写
        request.headers().asHttpHeaders().forEach((name, values) -> {
            if (!skipRequestHeader(route, name)) {
                targetHeaders.addAll(name, values);
            }
        });
        route.getAddHeaders().forEach(targetHeaders::set);
        if (StringUtils.hasText(trafficColor)) {
            targetHeaders.set(TrafficColorConstants.DEFAULT_HEADER_NAME, trafficColor);
        }
    }

    /**
     * 复制上游响应头。
     *
     * @param response 上游响应
     * @param targetHeaders 客户端响应头
     */
    private void copyResponseHeaders(ClientResponse response, HttpHeaders targetHeaders) {
        // hop-by-hop 响应头不能跨代理继续传
        response.headers().asHttpHeaders().forEach((name, values) -> {
            if (!hopByHop(name)) {
                targetHeaders.addAll(name, values);
            }
        });
    }

    /**
     * 判断请求头是否跳过透传。
     *
     * @param route 已命中路由
     * @param name 请求头名称
     * @return 是否跳过
     */
    private boolean skipRequestHeader(CompiledRoute route, String name) {
        // hop-by-hop 和路由声明移除的头都不透传
        if (hopByHop(name)) {
            return true;
        }
        return route.getRemoveHeaders().stream()
                .filter(Objects::nonNull)
                .anyMatch(header -> header.equalsIgnoreCase(name));
    }

    /**
     * 判断是否为逐跳头。
     *
     * @param name 请求头名称
     * @return 是否为逐跳头
     */
    private boolean hopByHop(String name) {
        // HTTP 头名大小写不敏感，统一按小写比较
        return name == null || ProxyHttpConstants.HOP_BY_HOP_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * 生成上游目标地址。
     *
     * @param request WebFlux 请求
     * @param route 已命中路由
     * @param upstream 已命中上游
     * @return 上游目标地址
     */
    private URI targetUri(ServerRequest request, CompiledRoute route, CompiledUpstream upstream) {
        // 当前先取第一个端点，负载均衡后续独立补
        CompiledUpstream.CompiledEndpoint endpoint = upstream.getEndpoints().get(0);
        String path = targetPath(request.uri().getRawPath(), route);
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme(scheme(upstream))
                .host(endpoint.getHost())
                .path(path);
        if (endpoint.getPort() != null) {
            builder.port(endpoint.getPort());
        }
        if (StringUtils.hasText(request.uri().getRawQuery())) {
            builder.query(request.uri().getRawQuery());
        }
        return builder.build(true).toUri();
    }

    /**
     * 生成上游目标路径。
     *
     * @param requestPath 请求路径
     * @param route 已命中路由
     * @return 上游目标路径
     */
    private String targetPath(String requestPath, CompiledRoute route) {
        // rewrite 比 stripPrefix 更明确，优先使用 rewrite
        String suffix = suffix(requestPath, route.getPathPrefix());
        if (StringUtils.hasText(route.getRewritePathPrefix())) {
            return joinPath(route.getRewritePathPrefix(), suffix);
        }
        if (Boolean.TRUE.equals(route.getStripPrefix())) {
            return suffix.isEmpty() ? ProxyHttpConstants.ROOT_PATH : suffix;
        }
        return requestPath;
    }

    /**
     * 计算入口路径后缀。
     *
     * @param requestPath 请求路径
     * @param prefix 路由前缀
     * @return 路径后缀
     */
    private String suffix(String requestPath, String prefix) {
        // 理论上这里已经命中过路由，兜底仍然保持空后缀
        if (requestPath == null || prefix == null || !requestPath.startsWith(prefix)) {
            return "";
        }
        String suffix = requestPath.substring(prefix.length());
        return suffix.isEmpty() ? "" : (suffix.startsWith(ProxyHttpConstants.PATH_SEPARATOR)
                ? suffix
                : ProxyHttpConstants.PATH_SEPARATOR + suffix);
    }

    /**
     * 拼接路径前缀和后缀。
     *
     * @param prefix 路径前缀
     * @param suffix 路径后缀
     * @return 拼接后的路径
     */
    private String joinPath(String prefix, String suffix) {
        // 保证 prefix 和 suffix 中间只有一个斜杠
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

    /**
     * 获取上游请求协议。
     *
     * @param upstream 已命中上游
     * @return 上游请求协议
     */
    private String scheme(CompiledUpstream upstream) {
        // WebSocket 先按 HTTP/S 转发入口处理
        Protocol protocol = upstream.getProtocol();
        if (protocol == Protocol.HTTPS || protocol == Protocol.WSS) {
            return ProxyHttpConstants.SCHEME_HTTPS;
        }
        return ProxyHttpConstants.SCHEME_HTTP;
    }

    /**
     * 获取请求域名。
     *
     * @param request WebFlux 请求
     * @return 请求域名
     */
    private String host(ServerRequest request) {
        // Host 用于运行态路由索引
        return request.headers().firstHeader(HttpHeaders.HOST);
    }

    /**
     * 获取客户端地址。
     *
     * @param request WebFlux 请求
     * @return 客户端地址
     */
    private String remoteAddress(ServerRequest request) {
        // IP 染色和后续审计都会复用这个地址
        return request.remoteAddress()
                .map(this::remoteAddress)
                .orElse(null);
    }

    /**
     * 转换客户端地址。
     *
     * @param address 客户端地址
     * @return 客户端地址文本
     */
    private String remoteAddress(InetSocketAddress address) {
        // InetAddress 为空时保留原始 host
        return address.getAddress() == null ? address.getHostString() : address.getAddress().getHostAddress();
    }

    /**
     * 创建仅包含状态码的响应并记录审计。
     *
     * @param request WebFlux 请求
     * @param route 已命中路由
     * @param upstreamName 上游名称
     * @param upstreamUri 上游地址
     * @param statusCode 响应状态码
     * @param trafficColor 流量颜色
     * @param methodAllowed 方法是否允许
     * @param authenticationRequired 是否需要认证
     * @param fallback 是否 fallback
     * @param outcome 执行结果
     * @param reason 结果原因
     * @param startNanos 请求开始时间
     * @return WebFlux 响应
     */
    private Mono<ServerResponse> statusResponse(ServerRequest request,
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
        return ServerResponse.status(statusCode)
                .build()
                .doOnSuccess(response -> auditRecorder.record(
                        request,
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

    /**
     * 根据状态码判断执行结果。
     *
     * @param status 状态码
     * @return 执行结果
     */
    private String outcome(int status) {
        if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            return ProxyAuditConstants.OUTCOME_ERROR;
        }
        if (status >= HttpStatus.BAD_REQUEST.value()) {
            return ProxyAuditConstants.OUTCOME_BLOCKED;
        }
        return ProxyAuditConstants.OUTCOME_SUCCESS;
    }

    /**
     * 记录熔断结果。
     *
     * @param circuitBreaker 熔断策略
     * @param statusCode 上游状态码
     * @param startNanos 请求开始时间
     */
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

    /**
     * 记录转发异常。
     *
     * @param request WebFlux 请求
     * @param route 已命中路由
     * @param upstreamUri 上游地址
     * @param trafficColor 流量颜色
     * @param circuitBreaker 熔断策略
     * @param startNanos 请求开始时间
     * @param error 转发异常
     * @return WebFlux 响应
     */
    private Mono<ServerResponse> recordForwardError(ServerRequest request,
                                                    CompiledRoute route,
                                                    URI upstreamUri,
                                                    String trafficColor,
                                                    CompiledCircuitBreakerPolicy circuitBreaker,
                                                    long startNanos,
                                                    Throwable error) {
        if (circuitBreaker == null) {
            auditRecorder.record(
                    request,
                    route,
                    route.getUpstreamName(),
                    upstreamUri,
                    ProxyAuditConstants.DEFAULT_ERROR_STATUS,
                    trafficColor,
                    true,
                    false,
                    false,
                    ProxyAuditConstants.OUTCOME_ERROR,
                    ProxyAuditConstants.REASON_UPSTREAM_ERROR,
                    error,
                    startNanos
            );
            return Mono.error(error);
        }
        long nowNanos = System.nanoTime();
        routeCircuitBreaker.record(
                circuitBreaker,
                true,
                routeCircuitBreaker.slowCall(circuitBreaker, startNanos, nowNanos),
                nowNanos
        );
        return circuitBreakerFallback(request, route, upstreamUri, trafficColor, circuitBreaker, startNanos,
                ProxyAuditConstants.REASON_UPSTREAM_ERROR, error);
    }

    /**
     * 创建熔断 fallback 响应。
     *
     * @param request WebFlux 请求
     * @param route 已命中路由
     * @param upstreamUri 上游地址
     * @param trafficColor 流量颜色
     * @param circuitBreaker 熔断策略
     * @param startNanos 请求开始时间
     * @param reason fallback 原因
     * @return WebFlux 响应
     */
    private Mono<ServerResponse> circuitBreakerFallback(ServerRequest request,
                                                        CompiledRoute route,
                                                        URI upstreamUri,
                                                        String trafficColor,
                                                        CompiledCircuitBreakerPolicy circuitBreaker,
                                                        long startNanos,
                                                        String reason) {
        return circuitBreakerFallback(request, route, upstreamUri, trafficColor, circuitBreaker, startNanos, reason,
                null);
    }

    /**
     * 创建熔断 fallback 响应。
     *
     * @param request WebFlux 请求
     * @param route 已命中路由
     * @param upstreamUri 上游地址
     * @param trafficColor 流量颜色
     * @param circuitBreaker 熔断策略
     * @param startNanos 请求开始时间
     * @param reason fallback 原因
     * @param error 转发异常
     * @return WebFlux 响应
     */
    private Mono<ServerResponse> circuitBreakerFallback(ServerRequest request,
                                                        CompiledRoute route,
                                                        URI upstreamUri,
                                                        String trafficColor,
                                                        CompiledCircuitBreakerPolicy circuitBreaker,
                                                        long startNanos,
                                                        String reason,
                                                        Throwable error) {
        return ServerResponse.status(HttpStatusCode.valueOf(circuitBreaker.getFallbackStatus()))
                .contentType(contentType(circuitBreaker.getContentType()))
                .bodyValue(ApiResponse.fail(circuitBreaker.getFallbackCode(), circuitBreaker.getFallbackMessage()))
                .doOnSuccess(response -> auditRecorder.record(
                        request,
                        route,
                        route.getUpstreamName(),
                        upstreamUri,
                        circuitBreaker.getFallbackStatus(),
                        trafficColor,
                        true,
                        false,
                        true,
                        ProxyAuditConstants.OUTCOME_FALLBACK,
                        reason,
                        error,
                        startNanos
                ));
    }

    /**
     * 创建限流 fallback 响应。
     *
     * @param request WebFlux 请求
     * @param route 已命中路由
     * @param rateLimit 限流策略
     * @param trafficColor 流量颜色
     * @param startNanos 请求开始时间
     * @return WebFlux 响应
     */
    private Mono<ServerResponse> rateLimitFallback(ServerRequest request,
                                                   CompiledRoute route,
                                                   CompiledRateLimitPolicy rateLimit,
                                                   String trafficColor,
                                                   long startNanos) {
        return ServerResponse.status(HttpStatusCode.valueOf(rateLimit.getRejectStatus()))
                .contentType(contentType(rateLimit.getContentType()))
                .bodyValue(ApiResponse.fail(rateLimit.getRejectCode(), rateLimit.getRejectMessage()))
                .doOnSuccess(response -> auditRecorder.record(
                        request,
                        route,
                        route.getUpstreamName(),
                        null,
                        rateLimit.getRejectStatus(),
                        trafficColor,
                        true,
                        false,
                        false,
                        ProxyAuditConstants.OUTCOME_BLOCKED,
                        ProxyAuditConstants.REASON_RATE_LIMITED,
                        null,
                        startNanos
                ));
    }

    /**
     * 创建限流组件不可用响应。
     *
     * @param request WebFlux 请求
     * @param route 已命中路由
     * @param rateLimit 限流策略
     * @param trafficColor 流量颜色
     * @param startNanos 请求开始时间
     * @return WebFlux 响应
     */
    private Mono<ServerResponse> rateLimitUnavailable(ServerRequest request,
                                                      CompiledRoute route,
                                                      CompiledRateLimitPolicy rateLimit,
                                                      String trafficColor,
                                                      long startNanos) {
        return ServerResponse.status(HttpStatusCode.valueOf(ProxyRateLimitConstants.UNAVAILABLE_STATUS))
                .contentType(contentType(rateLimit.getContentType()))
                .bodyValue(ApiResponse.fail(
                        ProxyRateLimitConstants.UNAVAILABLE_CODE,
                        ProxyRateLimitConstants.UNAVAILABLE_MESSAGE
                ))
                .doOnSuccess(response -> auditRecorder.record(
                        request,
                        route,
                        route.getUpstreamName(),
                        null,
                        ProxyRateLimitConstants.UNAVAILABLE_STATUS,
                        trafficColor,
                        true,
                        false,
                        false,
                        ProxyAuditConstants.OUTCOME_ERROR,
                        ProxyAuditConstants.REASON_RATE_LIMITER_UNAVAILABLE,
                        null,
                        startNanos
                ));
    }

    /**
     * 解析响应类型。
     *
     * @param contentType 响应类型
     * @return 响应类型
     */
    private MediaType contentType(String contentType) {
        try {
            // 配置异常时保持 JSON，避免 fallback 自己再失败
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException exception) {
            return MediaType.APPLICATION_JSON;
        }
    }
}
