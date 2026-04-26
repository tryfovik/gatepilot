package com.dt.gatepilot.proxy.interfaces.gateway;

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
import com.dt.gatepilot.proxy.infrastructure.loadbalancer.GatePilotLoadBalancerServiceIds;
import com.dt.gatepilot.proxy.interfaces.web.ProxyRuntimeAuditRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GatePilot SCG 治理过滤器测试。
 */
class GatePilotGatewayFilterTest {

    /**
     * 应交给 SCG chain 转发并写入染色请求头。
     */
    @Test
    void shouldPrepareScgForwardRequestWithRewrittenPathAndTrafficColorHeader() {
        CapturingRuntimeAuditSink auditSink = new CapturingRuntimeAuditSink();
        CapturingRuntimeMetricsSink metricsSink = new CapturingRuntimeMetricsSink();
        GatePilotGatewayFilter filter = filter(runtime(), allowRateLimiter(), allowAuthChecker(),
                auditSink, metricsSink);
        MockServerWebExchange exchange = exchange("http://api.example.com/api/game/admin/users?preview=enabled");
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();

        filter.filter(exchange, successChain(forwardedExchange)).block(Duration.ofSeconds(1));

        URI targetUri = forwardedExchange.get().getRequiredAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        assertThat(targetUri.toString()).isEqualTo(loadBalancedUri("admin-upstream", "/admin/users?preview=enabled"));
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Traffic-Color"))
                .isEqualTo("yellow");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Route-Id")).isEqualTo("admin");
        assertThat(forwardedExchange.get().getRequest().getHeaders()).doesNotContainKey("X-Remove-Me");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Traffic-Color")).isEqualTo("yellow");
        assertThat(auditSink.lastEvent().traceId()).isEqualTo("trace-001");
        assertThat(auditSink.lastEvent().routeId()).isEqualTo("admin");
        assertThat(auditSink.lastEvent().status()).isEqualTo(HttpStatus.OK.value());
        assertThat(auditSink.lastEvent().trafficColor()).isEqualTo("yellow");
        assertThat(auditSink.lastEvent().upstreamUri())
                .isEqualTo(loadBalancedUri("admin-upstream", "/admin/users?preview=enabled"));
        assertThat(metricsSink.routeId()).isEqualTo("admin");
        assertThat(metricsSink.status()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * 应在转发前拒绝不支持的方法。
     */
    @Test
    void shouldRejectUnsupportedMethodBeforeForwarding() {
        GatePilotGatewayFilter filter = filter(runtime());
        MockServerWebExchange exchange = exchange(
                MockServerHttpRequest.post("http://api.example.com/api/game/admin/users")
                        .header(HttpHeaders.HOST, "api.example.com")
        );
        AtomicInteger forwardedCount = new AtomicInteger();

        filter.filter(exchange, countingChain(forwardedCount, HttpStatus.OK)).block(Duration.ofSeconds(1));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ALLOW)).isEqualTo("GET");
        assertThat(forwardedCount.get()).isZero();
    }

    /**
     * 应在运行态为空时返回不可用。
     */
    @Test
    void shouldReturnServiceUnavailableWhenRuntimeIsEmpty() {
        GatePilotGatewayFilter filter = filter(new ProxyRuntimeState());
        MockServerWebExchange exchange = exchange("http://api.example.com/api/game/admin/users");

        filter.filter(exchange, countingChain(new AtomicInteger(), HttpStatus.OK)).block(Duration.ofSeconds(1));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * 应在多端点上游中准备 LoadBalancer 转发地址。
     */
    @Test
    void shouldPrepareLoadBalancedUriForMultipleEndpoints() {
        GatePilotGatewayFilter filter = filter(runtimeWithMultipleEndpoints());
        List<URI> targetUris = new ArrayList<>();

        filter.filter(exchange("http://api.example.com/api/game/admin/users"), captureUriChain(targetUris))
                .block(Duration.ofSeconds(1));
        filter.filter(exchange("http://api.example.com/api/game/admin/users"), captureUriChain(targetUris))
                .block(Duration.ofSeconds(1));

        assertThat(targetUris).extracting(URI::toString)
                .containsExactly(
                        loadBalancedUri("admin-upstream", "/admin/users"),
                        loadBalancedUri("admin-upstream", "/admin/users")
                );
    }

    /**
     * 应按发布分流颜色切换上游。
     */
    @Test
    void shouldSwitchUpstreamByReleaseTrafficSplit() {
        GatePilotGatewayFilter filter = filter(runtimeWithReleaseSplit());
        List<URI> targetUris = new ArrayList<>();
        MockServerWebExchange exchange = exchange(
                MockServerHttpRequest.get("http://api.example.com/api/game/admin/users")
                        .header(HttpHeaders.HOST, "api.example.com")
                        .header(ProxyAuditConstants.DEFAULT_TRACE_HEADER_NAME, "trace-001")
                        .header("X-User-Id", "u-10001")
        );

        filter.filter(exchange, captureUriChain(targetUris)).block(Duration.ofSeconds(1));

        assertThat(exchange.getResponse().getHeaders().getFirst("X-Traffic-Color")).isEqualTo("canary");
        assertThat(targetUris).extracting(URI::toString)
                .containsExactly(loadBalancedUri("candidate-upstream", "/admin/users"));
    }

    /**
     * 应在限流拒绝时不再调用 SCG 转发链。
     */
    @Test
    void shouldRejectBeforeScgRoutingWhenRateLimited() {
        CapturingRuntimeAuditSink auditSink = new CapturingRuntimeAuditSink();
        GatePilotGatewayFilter filter = filter(
                runtimeWithRateLimit(),
                (limiterName, rule) -> RateLimitAcquireResult.REJECTED,
                allowAuthChecker(),
                auditSink,
                noopMetricsSink()
        );
        AtomicInteger forwardedCount = new AtomicInteger();
        MockServerWebExchange exchange = exchange("http://api.example.com/api/game/admin/users");

        filter.filter(exchange, countingChain(forwardedCount, HttpStatus.OK)).block(Duration.ofSeconds(1));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(forwardedCount.get()).isZero();
        assertThat(auditSink.lastEvent().reason()).isEqualTo(ProxyAuditConstants.REASON_RATE_LIMITED);
    }

    /**
     * 应在路由认证失败时拒绝调用 SCG 转发链。
     */
    @Test
    void shouldRejectWhenRuntimeAuthCheckerDenied() {
        RuntimeAuthChecker deniedAuthChecker = () -> RuntimeAuthResult.denied(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.value(),
                "认证失败",
                ProxyAuditConstants.REASON_AUTHENTICATION_REQUIRED,
                null
        );
        GatePilotGatewayFilter filter = filter(runtimeWithAuthRequired(), allowRateLimiter(), deniedAuthChecker,
                noopAuditSink(), noopMetricsSink());
        AtomicInteger forwardedCount = new AtomicInteger();
        MockServerWebExchange exchange = exchange("http://api.example.com/api/game/admin/users");

        filter.filter(exchange, countingChain(forwardedCount, HttpStatus.OK)).block(Duration.ofSeconds(1));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwardedCount.get()).isZero();
        assertThat(exchange.getResponse().getBodyAsString().block(Duration.ofSeconds(1))).contains("认证失败");
    }

    /**
     * 应拒绝非本机来源访问内部入口。
     */
    @Test
    void shouldRejectInternalRequestFromRemoteAddress() {
        GatePilotGatewayFilter filter = filter(runtime());
        MockServerWebExchange exchange = exchange(
                MockServerHttpRequest.get("http://api.example.com/internal/actuator")
                        .header(HttpHeaders.HOST, "api.example.com")
                        .remoteAddress(InetSocketAddress.createUnresolved("10.0.0.8", 8080))
        );

        filter.filter(exchange, countingChain(new AtomicInteger(), HttpStatus.OK)).block(Duration.ofSeconds(1));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private GatewayFilterChain successChain(AtomicReference<ServerWebExchange> forwardedExchange) {
        return exchange -> {
            forwardedExchange.set(exchange);
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };
    }

    private GatewayFilterChain captureUriChain(List<URI> targetUris) {
        return exchange -> {
            targetUris.add(exchange.getRequiredAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR));
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };
    }

    private GatewayFilterChain countingChain(AtomicInteger forwardedCount, HttpStatus status) {
        return exchange -> {
            forwardedCount.incrementAndGet();
            exchange.getResponse().setStatusCode(status);
            return Mono.empty();
        };
    }

    private MockServerWebExchange exchange(String uri) {
        return exchange(MockServerHttpRequest.get(uri)
                .header(HttpHeaders.HOST, "api.example.com")
                .header(ProxyAuditConstants.DEFAULT_TRACE_HEADER_NAME, "trace-001")
                .header("X-Remove-Me", "bad"));
    }

    private MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> requestBuilder) {
        return MockServerWebExchange.from(requestBuilder);
    }

    private GatePilotGatewayFilter filter(ProxyRuntimeState runtimeState) {
        return filter(runtimeState, allowRateLimiter(), allowAuthChecker(), noopAuditSink(), noopMetricsSink());
    }

    private GatePilotGatewayFilter filter(ProxyRuntimeState runtimeState,
                                          RuntimeRateLimiter rateLimiter,
                                          RuntimeAuthChecker authChecker,
                                          RuntimeAuditSink auditSink,
                                          RuntimeMetricsSink metricsSink) {
        return new GatePilotGatewayFilter(
                runtimeState,
                new RouteAccessEvaluator(),
                new TrafficColorResolver(),
                new CircuitBreakerPolicyResolver(),
                new RouteCircuitBreaker(),
                new RateLimitPolicyResolver(),
                rateLimiter,
                new RetryPolicyResolver(),
                new ReleaseUpstreamResolver(),
                authChecker,
                new ProxyRuntimeAuditRecorder(auditSink, metricsSink),
                new RetryGatewayFilterFactory(),
                new ObjectMapper()
        );
    }

    private RuntimeRateLimiter allowRateLimiter() {
        return (limiterName, rule) -> RateLimitAcquireResult.ALLOWED;
    }

    private RuntimeAuthChecker allowAuthChecker() {
        return RuntimeAuthResult::pass;
    }

    private String loadBalancedUri(String upstreamName, String pathAndQuery) {
        return "lb://" + GatePilotLoadBalancerServiceIds.fromUpstreamName(upstreamName) + pathAndQuery;
    }

    private RuntimeAuditSink noopAuditSink() {
        return event -> {
        };
    }

    private RuntimeMetricsSink noopMetricsSink() {
        return (routeId, status, latencyMillis) -> {
        };
    }

    private ProxyRuntimeState runtime() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(config()));
        return state;
    }

    private ProxyRuntimeState runtimeWithRateLimit() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(configWithRateLimit()));
        return state;
    }

    private ProxyRuntimeState runtimeWithMultipleEndpoints() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(configWithMultipleEndpoints()));
        return state;
    }

    private ProxyRuntimeState runtimeWithAuthRequired() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(configWithAuthRequired()));
        return state;
    }

    private ProxyRuntimeState runtimeWithReleaseSplit() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(configWithReleaseSplit()));
        return state;
    }

    private PublishedConfig config() {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion("v1");
        config.getSpec().setConfigHash("hash-v1");
        config.getSpec().getRoutes().add(route());
        config.getSpec().getUpstreams().add(upstream());
        config.getSpec().getPolicies().add(trafficPolicy());
        return config;
    }

    private PublishedConfig configWithRateLimit() {
        PublishedConfig config = config();
        config.getSpec().getPolicies().clear();
        config.getSpec().getPolicies().add(trafficPolicyWithRateLimit());
        return config;
    }

    private PublishedConfig configWithMultipleEndpoints() {
        PublishedConfig config = config();
        config.getSpec().getUpstreams().clear();
        config.getSpec().getUpstreams().add(upstreamWithMultipleEndpoints());
        return config;
    }

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

    private PublishedConfig configWithReleaseSplit() {
        PublishedConfig config = config();
        config.getSpec().getRoutes().get(0).getPolicyNames().add("release-main");
        config.getSpec().getUpstreams().add(candidateUpstream());
        config.getSpec().getPolicies().add(releasePolicy());
        return config;
    }

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

    private PublishedConfig.PublishedUpstream upstreamWithMultipleEndpoints() {
        PublishedConfig.PublishedUpstream upstream = upstream();
        PublishedConfig.PublishedEndpoint endpoint = new PublishedConfig.PublishedEndpoint();
        endpoint.setHost("upstream-b.local");
        endpoint.setPort(8081);
        endpoint.setWeight(100);
        upstream.getEndpoints().add(endpoint);
        return upstream;
    }

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

    private PublishedConfig.PublishedPolicy trafficPolicyWithRateLimit() {
        PublishedConfig.PublishedPolicy policy = trafficPolicy();
        TrafficPolicy.RateLimitPolicy rateLimit = new TrafficPolicy.RateLimitPolicy();
        rateLimit.setEnabled(true);
        rateLimit.setRequestsPerSecond(1);
        policy.getConfig().put(PublishedConfigConstants.KEY_RATE_LIMIT, rateLimit);
        return policy;
    }

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
            lastEvent.set(event);
        }

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
            this.routeId.set(routeId);
            this.status.set(status);
        }

        private String routeId() {
            return routeId.get();
        }

        private int status() {
            return status.get();
        }
    }
}
