package com.dt.gatepilot.proxy.interfaces.web;

import com.dt.gatepilot.domain.enums.HttpMethod;
import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.domain.enums.TrafficColorSource;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.publish.PublishedConfigConstants;
import com.dt.gatepilot.proxy.domain.port.RuntimeAuthChecker;
import com.dt.gatepilot.proxy.domain.port.RuntimeAuthResult;
import com.dt.gatepilot.proxy.domain.port.RuntimeAuditSink;
import com.dt.gatepilot.proxy.domain.port.RuntimeMetricsSink;
import com.dt.gatepilot.proxy.domain.port.RuntimeRateLimiter;
import com.dt.gatepilot.proxy.domain.runtime.CircuitBreakerPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.ProxyAuditConstants;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.domain.runtime.PublishedConfigCompiler;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitAcquireResult;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.ReleaseUpstreamResolver;
import com.dt.gatepilot.proxy.domain.runtime.RetryPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessEvaluator;
import com.dt.gatepilot.proxy.domain.runtime.RouteCircuitBreaker;
import com.dt.gatepilot.proxy.domain.runtime.TrafficColorResolver;
import com.dt.gatepilot.proxy.domain.runtime.UpstreamEndpointSelector;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * proxy WebFlux 入口处理器测试。
 */
class GatePilotProxyHandlerTest {

    /**
     * 应转发请求并写入染色请求头。
     */
    @Test
    void shouldForwardRequestWithRewrittenPathAndTrafficColorHeader() {
        AtomicReference<ClientRequest> forwardedRequest = new AtomicReference<>();
        CapturingRuntimeAuditSink auditSink = new CapturingRuntimeAuditSink();
        CapturingRuntimeMetricsSink metricsSink = new CapturingRuntimeMetricsSink();
        GatePilotProxyHandler handler = handler(runtime(), forwardedRequest, auditSink, metricsSink);
        WebTestClient client = client(handler);

        client.get()
                .uri("/api/game/admin/users?preview=enabled")
                .header(HttpHeaders.HOST, "api.example.com")
                .header(ProxyAuditConstants.DEFAULT_TRACE_HEADER_NAME, "trace-001")
                .header("X-Remove-Me", "bad")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Traffic-Color", "yellow")
                .expectBody(String.class).isEqualTo("ok");

        assertThat(forwardedRequest.get().url().toString())
                .isEqualTo("http://upstream.local:8080/admin/users?preview=enabled");
        assertThat(forwardedRequest.get().headers().getFirst("X-Traffic-Color")).isEqualTo("yellow");
        assertThat(forwardedRequest.get().headers().getFirst("X-Route-Id")).isEqualTo("admin");
        assertThat(forwardedRequest.get().headers()).doesNotContainKey("X-Remove-Me");
        assertThat(auditSink.lastEvent().traceId()).isEqualTo("trace-001");
        assertThat(auditSink.lastEvent().routeId()).isEqualTo("admin");
        assertThat(auditSink.lastEvent().status()).isEqualTo(HttpStatus.OK.value());
        assertThat(auditSink.lastEvent().trafficColor()).isEqualTo("yellow");
        assertThat(auditSink.lastEvent().upstreamUri()).isEqualTo("http://upstream.local:8080/admin/users?preview=enabled");
        assertThat(auditSink.lastEvent().outcome()).isEqualTo(ProxyAuditConstants.OUTCOME_SUCCESS);
        assertThat(metricsSink.routeId()).isEqualTo("admin");
        assertThat(metricsSink.status()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * 应在转发前拒绝不支持的方法。
     */
    @Test
    void shouldRejectUnsupportedMethodBeforeForwarding() {
        AtomicReference<ClientRequest> forwardedRequest = new AtomicReference<>();
        GatePilotProxyHandler handler = handler(runtime(), forwardedRequest);
        WebTestClient client = client(handler);

        client.post()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
                .expectHeader().valueEquals(HttpHeaders.ALLOW, "GET");

        assertThat(forwardedRequest.get()).isNull();
    }

    /**
     * 应在运行态为空时返回不可用。
     */
    @Test
    void shouldReturnServiceUnavailableWhenRuntimeIsEmpty() {
        AtomicReference<ClientRequest> forwardedRequest = new AtomicReference<>();
        GatePilotProxyHandler handler = handler(new ProxyRuntimeState(), forwardedRequest);
        WebTestClient client = client(handler);

        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * 应在连续上游失败后打开熔断并返回 fallback。
     */
    @Test
    void shouldOpenCircuitBreakerAfterUpstreamFailures() {
        AtomicInteger forwardedCount = new AtomicInteger();
        GatePilotProxyHandler handler = handler(runtimeWithCircuitBreaker(), forwardedCount, HttpStatus.INTERNAL_SERVER_ERROR);
        WebTestClient client = client(handler);

        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(String.class)
                .value(body -> assertThat(body)
                        .contains("\"code\":90001")
                        .contains("服务临时不可用"));

        assertThat(forwardedCount.get()).isEqualTo(2);
    }

    /**
     * 应在上游异常时记录失败并返回 fallback。
     */
    @Test
    void shouldFallbackWhenForwardFailedWithCircuitBreaker() {
        AtomicInteger forwardedCount = new AtomicInteger();
        CapturingRuntimeAuditSink auditSink = new CapturingRuntimeAuditSink();
        GatePilotProxyHandler handler = handlerWithError(runtimeWithCircuitBreaker(), forwardedCount, auditSink);
        WebTestClient client = client(handler);

        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("服务临时不可用"));

        assertThat(forwardedCount.get()).isEqualTo(1);
        assertThat(auditSink.lastEvent().fallback()).isTrue();
        assertThat(auditSink.lastEvent().outcome()).isEqualTo(ProxyAuditConstants.OUTCOME_FALLBACK);
        assertThat(auditSink.lastEvent().reason()).isEqualTo(ProxyAuditConstants.REASON_UPSTREAM_ERROR);
        assertThat(auditSink.lastEvent().error()).isEqualTo(IllegalStateException.class.getName());
    }

    /**
     * 应在限流拒绝时不再转发上游。
     */
    @Test
    void shouldRejectBeforeForwardingWhenRateLimited() {
        AtomicInteger forwardedCount = new AtomicInteger();
        CapturingRuntimeAuditSink auditSink = new CapturingRuntimeAuditSink();
        GatePilotProxyHandler handler = handler(
                runtimeWithRateLimit(),
                forwardedCount,
                HttpStatus.OK,
                (limiterName, rule) -> RateLimitAcquireResult.REJECTED,
                auditSink
        );
        WebTestClient client = client(handler);

        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("请求过于频繁"));

        assertThat(forwardedCount.get()).isZero();
        assertThat(auditSink.lastEvent().routeId()).isEqualTo("admin");
        assertThat(auditSink.lastEvent().status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(auditSink.lastEvent().outcome()).isEqualTo(ProxyAuditConstants.OUTCOME_BLOCKED);
        assertThat(auditSink.lastEvent().reason()).isEqualTo(ProxyAuditConstants.REASON_RATE_LIMITED);
    }

    /**
     * 应在限流组件不可用时拒绝请求。
     */
    @Test
    void shouldRejectWhenRateLimiterUnavailable() {
        AtomicInteger forwardedCount = new AtomicInteger();
        GatePilotProxyHandler handler = handler(
                runtimeWithRateLimit(),
                forwardedCount,
                HttpStatus.OK,
                (limiterName, rule) -> RateLimitAcquireResult.UNAVAILABLE
        );
        WebTestClient client = client(handler);

        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("网关限流组件未就绪"));

        assertThat(forwardedCount.get()).isZero();
    }

    /**
     * 应在可重试状态码上切换端点后成功。
     */
    @Test
    void shouldRetryRetryableStatusWithNextEndpoint() {
        List<ClientRequest> forwardedRequests = new ArrayList<>();
        AtomicInteger forwardedCount = new AtomicInteger();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    forwardedRequests.add(request);
                    int count = forwardedCount.incrementAndGet();
                    HttpStatus status = count == 1 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.OK;
                    return Mono.just(ClientResponse.create(status).body("retry-ok").build());
                })
                .build();
        GatePilotProxyHandler handler = handler(runtimeWithRetry(), webClient);
        WebTestClient client = client(handler);

        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("retry-ok");

        assertThat(forwardedRequests).hasSize(2);
        assertThat(forwardedRequests.get(0).url().toString())
                .isEqualTo("http://upstream.local:8080/admin/users");
        assertThat(forwardedRequests.get(1).url().toString())
                .isEqualTo("http://upstream-b.local:8081/admin/users");
    }

    /**
     * 应在多端点上游中轮询转发。
     */
    @Test
    void shouldRoundRobinAcrossUpstreamEndpoints() {
        List<ClientRequest> forwardedRequests = new ArrayList<>();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    forwardedRequests.add(request);
                    return Mono.just(ClientResponse.create(HttpStatus.OK).body("ok").build());
                })
                .build();
        GatePilotProxyHandler handler = handler(runtimeWithMultipleEndpoints(), webClient);
        WebTestClient client = client(handler);

        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isOk();
        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isOk();

        assertThat(forwardedRequests.get(0).url().toString())
                .isEqualTo("http://upstream.local:8080/admin/users");
        assertThat(forwardedRequests.get(1).url().toString())
                .isEqualTo("http://upstream-b.local:8081/admin/users");
    }

    /**
     * 应按发布分流颜色切换上游。
     */
    @Test
    void shouldSwitchUpstreamByReleaseTrafficSplit() {
        AtomicReference<ClientRequest> forwardedRequest = new AtomicReference<>();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    forwardedRequest.set(request);
                    return Mono.just(ClientResponse.create(HttpStatus.OK).body("candidate").build());
                })
                .build();
        GatePilotProxyHandler handler = handler(runtimeWithReleaseSplit(), webClient);
        WebTestClient client = client(handler);

        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .header("X-User-Id", "u-10001")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Traffic-Color", "canary")
                .expectBody(String.class).isEqualTo("candidate");

        assertThat(forwardedRequest.get().url().toString())
                .isEqualTo("http://candidate.local:9090/admin/users");
    }

    /**
     * 应在路由认证失败时拒绝转发。
     */
    @Test
    void shouldRejectWhenRuntimeAuthCheckerDenied() {
        List<ClientRequest> forwardedRequests = new ArrayList<>();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    forwardedRequests.add(request);
                    return Mono.just(ClientResponse.create(HttpStatus.OK).body("ok").build());
                })
                .build();
        RuntimeAuthChecker deniedAuthChecker = () -> RuntimeAuthResult.denied(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.value(),
                "认证失败",
                ProxyAuditConstants.REASON_AUTHENTICATION_REQUIRED,
                null
        );
        GatePilotProxyHandler handler = handler(runtimeWithAuthRequired(), webClient, deniedAuthChecker);
        WebTestClient client = client(handler);

        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("认证失败"));

        assertThat(forwardedRequests).isEmpty();
    }

    /**
     * 应拒绝非本机来源访问内部入口。
     */
    @Test
    void shouldRejectInternalRequestFromRemoteAddress() {
        GatePilotProxyHandler handler = handler(runtime(), WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK).body("ok").build()))
                .build());
        MockServerRequest request = MockServerRequest.builder()
                .method(org.springframework.http.HttpMethod.GET)
                .uri(URI.create("http://api.example.com/internal/actuator"))
                .remoteAddress(InetSocketAddress.createUnresolved("10.0.0.8", 8080))
                .build();

        ServerResponse response = handler.handle(request).block(Duration.ofSeconds(1));

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * 创建 WebTestClient。
     *
     * @param handler proxy 入口处理器
     * @return WebTestClient
     */
    private WebTestClient client(GatePilotProxyHandler handler) {
        return WebTestClient.bindToRouterFunction(RouterFunctions.route(RequestPredicates.all(), handler::handle))
                .build();
    }

    /**
     * 创建默认成功转发处理器。
     *
     * @param runtimeState proxy 运行态
     * @param forwardedRequest 转发请求记录器
     * @return proxy 入口处理器
     */
    private GatePilotProxyHandler handler(ProxyRuntimeState runtimeState,
                                          AtomicReference<ClientRequest> forwardedRequest) {
        return handler(runtimeState, forwardedRequest, noopAuditSink(), noopMetricsSink());
    }

    /**
     * 创建默认成功转发处理器。
     *
     * @param runtimeState proxy 运行态
     * @param forwardedRequest 转发请求记录器
     * @param runtimeAuditSink 运行审计采集器
     * @param runtimeMetricsSink 运行指标采集器
     * @return proxy 入口处理器
     */
    private GatePilotProxyHandler handler(ProxyRuntimeState runtimeState,
                                          AtomicReference<ClientRequest> forwardedRequest,
                                          RuntimeAuditSink runtimeAuditSink,
                                          RuntimeMetricsSink runtimeMetricsSink) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    forwardedRequest.set(request);
                    return Mono.just(ClientResponse.create(HttpStatus.OK).body("ok").build());
                })
                .build();
        return new GatePilotProxyHandler(
                runtimeState,
                new RouteAccessEvaluator(),
                new TrafficColorResolver(),
                new CircuitBreakerPolicyResolver(),
                new RouteCircuitBreaker(),
                new RateLimitPolicyResolver(),
                allowRateLimiter(),
                new RetryPolicyResolver(),
                new ReleaseUpstreamResolver(),
                new UpstreamEndpointSelector(),
                allowAuthChecker(),
                new ProxyRuntimeAuditRecorder(runtimeAuditSink, runtimeMetricsSink),
                webClient
        );
    }

    /**
     * 创建指定客户端的处理器。
     *
     * @param runtimeState proxy 运行态
     * @param webClient WebClient
     * @return proxy 入口处理器
     */
    private GatePilotProxyHandler handler(ProxyRuntimeState runtimeState, WebClient webClient) {
        return handler(runtimeState, webClient, allowAuthChecker());
    }

    /**
     * 创建指定客户端和认证器的处理器。
     *
     * @param runtimeState proxy 运行态
     * @param webClient WebClient
     * @param runtimeAuthChecker 运行时认证器
     * @return proxy 入口处理器
     */
    private GatePilotProxyHandler handler(ProxyRuntimeState runtimeState,
                                          WebClient webClient,
                                          RuntimeAuthChecker runtimeAuthChecker) {
        return new GatePilotProxyHandler(
                runtimeState,
                new RouteAccessEvaluator(),
                new TrafficColorResolver(),
                new CircuitBreakerPolicyResolver(),
                new RouteCircuitBreaker(),
                new RateLimitPolicyResolver(),
                allowRateLimiter(),
                new RetryPolicyResolver(),
                new ReleaseUpstreamResolver(),
                new UpstreamEndpointSelector(),
                runtimeAuthChecker,
                new ProxyRuntimeAuditRecorder(noopAuditSink(), noopMetricsSink()),
                webClient
        );
    }

    /**
     * 创建固定状态转发处理器。
     *
     * @param runtimeState proxy 运行态
     * @param forwardedCount 转发计数器
     * @param status 上游状态
     * @return proxy 入口处理器
     */
    private GatePilotProxyHandler handler(ProxyRuntimeState runtimeState,
                                          AtomicInteger forwardedCount,
                                          HttpStatus status) {
        return handler(runtimeState, forwardedCount, status, allowRateLimiter());
    }

    /**
     * 创建固定状态转发处理器。
     *
     * @param runtimeState proxy 运行态
     * @param forwardedCount 转发计数器
     * @param status 上游状态
     * @param rateLimiter 运行时限流器
     * @return proxy 入口处理器
     */
    private GatePilotProxyHandler handler(ProxyRuntimeState runtimeState,
                                          AtomicInteger forwardedCount,
                                          HttpStatus status,
                                          RuntimeRateLimiter rateLimiter) {
        return handler(runtimeState, forwardedCount, status, rateLimiter, noopAuditSink());
    }

    /**
     * 创建固定状态转发处理器。
     *
     * @param runtimeState proxy 运行态
     * @param forwardedCount 转发计数器
     * @param status 上游状态
     * @param rateLimiter 运行时限流器
     * @param runtimeAuditSink 运行审计采集器
     * @return proxy 入口处理器
     */
    private GatePilotProxyHandler handler(ProxyRuntimeState runtimeState,
                                          AtomicInteger forwardedCount,
                                          HttpStatus status,
                                          RuntimeRateLimiter rateLimiter,
                                          RuntimeAuditSink runtimeAuditSink) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    forwardedCount.incrementAndGet();
                    return Mono.just(ClientResponse.create(status).body("upstream").build());
                })
                .build();
        return new GatePilotProxyHandler(
                runtimeState,
                new RouteAccessEvaluator(),
                new TrafficColorResolver(),
                new CircuitBreakerPolicyResolver(),
                new RouteCircuitBreaker(),
                new RateLimitPolicyResolver(),
                rateLimiter,
                new RetryPolicyResolver(),
                new ReleaseUpstreamResolver(),
                new UpstreamEndpointSelector(),
                allowAuthChecker(),
                new ProxyRuntimeAuditRecorder(runtimeAuditSink, noopMetricsSink()),
                webClient
        );
    }

    /**
     * 创建异常转发处理器。
     *
     * @param runtimeState proxy 运行态
     * @param forwardedCount 转发计数器
     * @return proxy 入口处理器
     */
    private GatePilotProxyHandler handlerWithError(ProxyRuntimeState runtimeState, AtomicInteger forwardedCount) {
        return handlerWithError(runtimeState, forwardedCount, noopAuditSink());
    }

    /**
     * 创建异常转发处理器。
     *
     * @param runtimeState proxy 运行态
     * @param forwardedCount 转发计数器
     * @param runtimeAuditSink 运行审计采集器
     * @return proxy 入口处理器
     */
    private GatePilotProxyHandler handlerWithError(ProxyRuntimeState runtimeState,
                                                   AtomicInteger forwardedCount,
                                                   RuntimeAuditSink runtimeAuditSink) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    forwardedCount.incrementAndGet();
                    return Mono.error(new IllegalStateException("upstream error"));
                })
                .build();
        return new GatePilotProxyHandler(
                runtimeState,
                new RouteAccessEvaluator(),
                new TrafficColorResolver(),
                new CircuitBreakerPolicyResolver(),
                new RouteCircuitBreaker(),
                new RateLimitPolicyResolver(),
                allowRateLimiter(),
                new RetryPolicyResolver(),
                new ReleaseUpstreamResolver(),
                new UpstreamEndpointSelector(),
                allowAuthChecker(),
                new ProxyRuntimeAuditRecorder(runtimeAuditSink, noopMetricsSink()),
                webClient
        );
    }

    /**
     * 创建放行限流器。
     *
     * @return 运行时限流器
     */
    private RuntimeRateLimiter allowRateLimiter() {
        return (limiterName, rule) -> RateLimitAcquireResult.ALLOWED;
    }

    /**
     * 创建放行认证器。
     *
     * @return 运行时认证器
     */
    private RuntimeAuthChecker allowAuthChecker() {
        return com.dt.gatepilot.proxy.domain.port.RuntimeAuthResult::pass;
    }

    /**
     * 创建空审计采集器。
     *
     * @return 运行审计采集器
     */
    private RuntimeAuditSink noopAuditSink() {
        // 默认测试只关心响应，不采集审计
        return event -> {
        };
    }

    /**
     * 创建空指标采集器。
     *
     * @return 运行指标采集器
     */
    private RuntimeMetricsSink noopMetricsSink() {
        // 默认测试只关心响应，不采集指标
        return (routeId, status, latencyMillis) -> {
        };
    }

    /**
     * 创建默认运行态。
     *
     * @return proxy 运行态
     */
    private ProxyRuntimeState runtime() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(config()));
        return state;
    }

    /**
     * 创建带熔断的运行态。
     *
     * @return proxy 运行态
     */
    private ProxyRuntimeState runtimeWithCircuitBreaker() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(configWithCircuitBreaker()));
        return state;
    }

    /**
     * 创建带限流的运行态。
     *
     * @return proxy 运行态
     */
    private ProxyRuntimeState runtimeWithRateLimit() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(configWithRateLimit()));
        return state;
    }

    /**
     * 创建带重试的运行态。
     *
     * @return proxy 运行态
     */
    private ProxyRuntimeState runtimeWithRetry() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(configWithRetry()));
        return state;
    }

    /**
     * 创建带多端点上游的运行态。
     *
     * @return proxy 运行态
     */
    private ProxyRuntimeState runtimeWithMultipleEndpoints() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(configWithMultipleEndpoints()));
        return state;
    }

    /**
     * 创建带认证要求的运行态。
     *
     * @return proxy 运行态
     */
    private ProxyRuntimeState runtimeWithAuthRequired() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(configWithAuthRequired()));
        return state;
    }

    /**
     * 创建带发布分流的运行态。
     *
     * @return proxy 运行态
     */
    private ProxyRuntimeState runtimeWithReleaseSplit() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(configWithReleaseSplit()));
        return state;
    }

    /**
     * 创建默认发布配置。
     *
     * @return 发布配置
     */
    private PublishedConfig config() {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion("v1");
        config.getSpec().setConfigHash("hash-v1");
        config.getSpec().getRoutes().add(route());
        config.getSpec().getUpstreams().add(upstream());
        config.getSpec().getPolicies().add(trafficPolicy());
        return config;
    }

    /**
     * 创建带熔断的发布配置。
     *
     * @return 发布配置
     */
    private PublishedConfig configWithCircuitBreaker() {
        PublishedConfig config = config();
        config.getSpec().getPolicies().clear();
        config.getSpec().getPolicies().add(trafficPolicyWithCircuitBreaker());
        return config;
    }

    /**
     * 创建带限流的发布配置。
     *
     * @return 发布配置
     */
    private PublishedConfig configWithRateLimit() {
        PublishedConfig config = config();
        config.getSpec().getPolicies().clear();
        config.getSpec().getPolicies().add(trafficPolicyWithRateLimit());
        return config;
    }

    /**
     * 创建带重试的发布配置。
     *
     * @return 发布配置
     */
    private PublishedConfig configWithRetry() {
        PublishedConfig config = configWithMultipleEndpoints();
        config.getSpec().getPolicies().clear();
        config.getSpec().getPolicies().add(trafficPolicyWithRetry());
        return config;
    }

    /**
     * 创建带多端点上游的发布配置。
     *
     * @return 发布配置
     */
    private PublishedConfig configWithMultipleEndpoints() {
        PublishedConfig config = config();
        config.getSpec().getUpstreams().clear();
        config.getSpec().getUpstreams().add(upstreamWithMultipleEndpoints());
        return config;
    }

    /**
     * 创建带认证要求的发布配置。
     *
     * @return 发布配置
     */
    private PublishedConfig configWithAuthRequired() {
        PublishedConfig config = config();
        config.getSpec().getRoutes().get(0).getPolicyNames().add("auth-main");
        PublishedConfig.PublishedPolicy auth = new PublishedConfig.PublishedPolicy();
        auth.setName("auth-main");
        auth.setType(PublishedConfigConstants.POLICY_TYPE_AUTH);
        auth.getConfig().put(PublishedConfigConstants.KEY_TYPE, "JWT");
        auth.getConfig().put(PublishedConfigConstants.KEY_ANONYMOUS_ALLOWED, false);
        config.getSpec().getPolicies().add(auth);
        return config;
    }

    /**
     * 创建带发布分流的发布配置。
     *
     * @return 发布配置
     */
    private PublishedConfig configWithReleaseSplit() {
        PublishedConfig config = config();
        config.getSpec().getRoutes().get(0).getPolicyNames().add("release-main");
        config.getSpec().getUpstreams().add(candidateUpstream());
        config.getSpec().getPolicies().add(releasePolicy());
        return config;
    }

    /**
     * 创建默认路由。
     *
     * @return 发布路由
     */
    private PublishedConfig.PublishedRoute route() {
        PublishedConfig.PublishedRoute route = new PublishedConfig.PublishedRoute();
        route.setRouteId("admin");
        route.getHosts().add("api.example.com");
        route.setPath("/api/game/admin");
        route.getMethods().add(HttpMethod.GET);
        route.setUpstreamName("admin-upstream");
        route.setStripPrefix(true);
        route.setRewritePathPrefix("/admin");
        route.getAddHeaders().put("X-Route-Id", "admin");
        route.getRemoveHeaders().add("X-Remove-Me");
        route.getPolicyNames().add("traffic-main");
        return route;
    }

    /**
     * 创建默认上游。
     *
     * @return 发布上游
     */
    private PublishedConfig.PublishedUpstream upstream() {
        PublishedConfig.PublishedUpstream upstream = new PublishedConfig.PublishedUpstream();
        upstream.setName("admin-upstream");
        upstream.setProtocol(Protocol.HTTP);
        upstream.setLoadBalance("ROUND_ROBIN");
        PublishedConfig.PublishedEndpoint endpoint = new PublishedConfig.PublishedEndpoint();
        endpoint.setHost("upstream.local");
        endpoint.setPort(8080);
        endpoint.setWeight(100);
        upstream.getEndpoints().add(endpoint);
        return upstream;
    }

    /**
     * 创建多端点上游。
     *
     * @return 发布上游
     */
    private PublishedConfig.PublishedUpstream upstreamWithMultipleEndpoints() {
        PublishedConfig.PublishedUpstream upstream = upstream();
        PublishedConfig.PublishedEndpoint endpoint = new PublishedConfig.PublishedEndpoint();
        endpoint.setHost("upstream-b.local");
        endpoint.setPort(8081);
        endpoint.setWeight(100);
        upstream.getEndpoints().add(endpoint);
        return upstream;
    }

    /**
     * 创建候选版本上游。
     *
     * @return 发布上游
     */
    private PublishedConfig.PublishedUpstream candidateUpstream() {
        PublishedConfig.PublishedUpstream upstream = new PublishedConfig.PublishedUpstream();
        upstream.setName("candidate-upstream");
        upstream.setProtocol(Protocol.HTTP);
        upstream.setLoadBalance("ROUND_ROBIN");
        PublishedConfig.PublishedEndpoint endpoint = new PublishedConfig.PublishedEndpoint();
        endpoint.setHost("candidate.local");
        endpoint.setPort(9090);
        endpoint.setWeight(100);
        upstream.getEndpoints().add(endpoint);
        return upstream;
    }

    /**
     * 创建默认流量策略。
     *
     * @return 发布策略
     */
    private PublishedConfig.PublishedPolicy trafficPolicy() {
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName("traffic-main");
        policy.setType(PublishedConfigConstants.POLICY_TYPE_TRAFFIC);
        TrafficPolicy.TrafficColorRule rule = new TrafficPolicy.TrafficColorRule();
        rule.setSource(TrafficColorSource.QUERY);
        rule.setKey("preview");
        rule.setMatch("enabled");
        rule.setColor("yellow");
        policy.getConfig().put(PublishedConfigConstants.KEY_COLOR_RULES, List.of(rule));
        return policy;
    }

    /**
     * 创建带熔断的流量策略。
     *
     * @return 发布策略
     */
    private PublishedConfig.PublishedPolicy trafficPolicyWithCircuitBreaker() {
        PublishedConfig.PublishedPolicy policy = trafficPolicy();
        TrafficPolicy.CircuitBreakerPolicy circuitBreaker = new TrafficPolicy.CircuitBreakerPolicy();
        circuitBreaker.setEnabled(true);
        circuitBreaker.setSlidingWindowSize(2);
        circuitBreaker.setMinimumNumberOfCalls(2);
        circuitBreaker.setFailureRateThreshold(50);
        circuitBreaker.setWaitDurationInOpenState(Duration.ofSeconds(30));
        circuitBreaker.setPermittedNumberOfCallsInHalfOpenState(1);
        circuitBreaker.setStatusCodes(List.of(500));
        circuitBreaker.setFallbackStatus(503);
        circuitBreaker.setFallbackCode(90001);
        circuitBreaker.setFallbackMessage("服务临时不可用");
        policy.getConfig().put(PublishedConfigConstants.KEY_CIRCUIT_BREAKER, circuitBreaker);
        return policy;
    }

    /**
     * 创建带限流的流量策略。
     *
     * @return 发布策略
     */
    private PublishedConfig.PublishedPolicy trafficPolicyWithRateLimit() {
        PublishedConfig.PublishedPolicy policy = trafficPolicy();
        TrafficPolicy.RateLimitPolicy rateLimit = new TrafficPolicy.RateLimitPolicy();
        rateLimit.setEnabled(true);
        rateLimit.setRequestsPerSecond(1);
        policy.getConfig().put(PublishedConfigConstants.KEY_RATE_LIMIT, rateLimit);
        return policy;
    }

    /**
     * 创建带重试的流量策略。
     *
     * @return 发布策略
     */
    private PublishedConfig.PublishedPolicy trafficPolicyWithRetry() {
        PublishedConfig.PublishedPolicy policy = trafficPolicy();
        TrafficPolicy.RetryPolicy retry = new TrafficPolicy.RetryPolicy();
        retry.setEnabled(true);
        retry.setMaxAttempts(2);
        retry.setStatuses(List.of(HttpStatus.SERVICE_UNAVAILABLE.value()));
        retry.setFirstBackoff(Duration.ZERO);
        policy.getConfig().put(PublishedConfigConstants.KEY_RETRY, retry);
        return policy;
    }

    /**
     * 创建发布分流策略。
     *
     * @return 发布策略
     */
    private PublishedConfig.PublishedPolicy releasePolicy() {
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName("release-main");
        policy.setType(PublishedConfigConstants.POLICY_TYPE_RELEASE);
        Map<String, Object> upstreamRef = new LinkedHashMap<>();
        upstreamRef.put(PublishedConfigConstants.KEY_NAME, "candidate-upstream");
        Map<String, Object> split = new LinkedHashMap<>();
        split.put(PublishedConfigConstants.KEY_TARGET, "canary");
        split.put(PublishedConfigConstants.KEY_COLOR, "canary");
        split.put(PublishedConfigConstants.KEY_WEIGHT, 100);
        split.put(PublishedConfigConstants.KEY_UPSTREAM_REF, upstreamRef);
        policy.getConfig().put(PublishedConfigConstants.KEY_TRAFFIC_SPLITS, List.of(split));
        return policy;
    }

    /**
     * 测试用运行审计采集器。
     */
    private static class CapturingRuntimeAuditSink implements RuntimeAuditSink {

        /**
         * 最近一次审计事件。
         */
        private final AtomicReference<RuntimeAuditSink.RuntimeAuditEvent> lastEvent = new AtomicReference<>();

        /**
         * 记录运行审计事件。
         *
         * @param event 审计事件
         */
        @Override
        public void emit(RuntimeAuditSink.RuntimeAuditEvent event) {
            // 测试只保留最后一次事件
            lastEvent.set(event);
        }

        /**
         * 获取最近一次审计事件。
         *
         * @return 最近一次审计事件
         */
        private RuntimeAuditSink.RuntimeAuditEvent lastEvent() {
            return lastEvent.get();
        }
    }

    /**
     * 测试用运行指标采集器。
     */
    private static class CapturingRuntimeMetricsSink implements RuntimeMetricsSink {

        /**
         * 最近一次路由标识。
         */
        private final AtomicReference<String> routeId = new AtomicReference<>();

        /**
         * 最近一次状态码。
         */
        private final AtomicInteger status = new AtomicInteger();

        /**
         * 记录路由请求指标。
         *
         * @param routeId 路由标识
         * @param status 响应状态码
         * @param latencyMillis 延迟
         */
        @Override
        public void recordRouteRequest(String routeId, int status, long latencyMillis) {
            // 测试只关心路由和状态
            this.routeId.set(routeId);
            this.status.set(status);
        }

        /**
         * 获取最近一次路由标识。
         *
         * @return 路由标识
         */
        private String routeId() {
            return routeId.get();
        }

        /**
         * 获取最近一次状态码。
         *
         * @return 状态码
         */
        private int status() {
            return status.get();
        }
    }
}
