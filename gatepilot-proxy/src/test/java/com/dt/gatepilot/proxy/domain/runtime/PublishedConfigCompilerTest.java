package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.AuthType;
import com.dt.gatepilot.domain.resource.publish.PublishedConfigConstants;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PublishedConfig 编译器测试。
 */
class PublishedConfigCompilerTest {

    private static final String HOST = "api.example.com";

    private static final String ROUTE_ID = "admin";

    private static final String ROUTE_PATH = "/api";

    private static final String TRAFFIC_POLICY_NAME = "traffic-main";

    private static final String RELEASE_POLICY_NAME = "release-main";

    private static final String AUTH_POLICY_NAME = "auth-main";

    private static final String STABLE_UPSTREAM = "stable-upstream";

    private static final String CANARY_UPSTREAM = "canary-upstream";

    @Test
    void shouldMatchRouteByHostAndLongestPathPrefix() {
        PublishedConfig config = config("v1", "hash-v1");
        PublishedConfig.PublishedRoute projectRoute = route("project", "API.EXAMPLE.COM:443", "/api/game");
        projectRoute.setProjectName("game");
        config.getSpec().getRoutes().add(projectRoute);
        config.getSpec().getRoutes().add(route("admin", "api.example.com", "/api/game/admin"));
        config.getSpec().getRoutes().add(route("public", null, "/api/public"));

        CompiledProxyRuntime runtime = new PublishedConfigCompiler().compile(config);

        assertThat(runtime.match("api.example.com", "/api/game/admin/users").getRouteId()).isEqualTo("admin");
        assertThat(runtime.match("api.example.com:443", "/api/game/orders").getRouteId()).isEqualTo("project");
        assertThat(runtime.match("api.example.com:443", "/api/game/orders").getProjectName()).isEqualTo("game");
        assertThat(runtime.match("other.example.com", "/api/public/ping").getRouteId()).isEqualTo("public");
        assertThat(runtime.match("other.example.com", "/api/missing")).isNull();
    }

    @Test
    void shouldRejectDuplicateRouteMatchKey() {
        PublishedConfig config = config("v1", "hash-v1");
        config.getSpec().getRoutes().add(route("one", "api.example.com", "/api/game"));
        config.getSpec().getRoutes().add(route("two", "API.EXAMPLE.COM:443", "/api/game/"));

        assertThatThrownBy(() -> new PublishedConfigCompiler().compile(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate route match key");
    }

    @Test
    void shouldRejectUnsupportedLoadBalanceStrategy() {
        PublishedConfig config = config("v1", "hash-v1");
        PublishedConfig.PublishedUpstream upstream = new PublishedConfig.PublishedUpstream();
        upstream.setName("hash-upstream");
        upstream.setLoadBalance("CONSISTENT_HASH");
        config.getSpec().getUpstreams().add(upstream);

        assertThatThrownBy(() -> new PublishedConfigCompiler().compile(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ProxyLoadBalanceConstants.ERROR_UNSUPPORTED_STRATEGY_PREFIX);
    }

    @Test
    void shouldPrecompileRoutePoliciesWithoutDependingOnRawConfigMap() {
        PublishedConfig config = config("v1", "hash-v1");
        PublishedConfig.PublishedRoute route = route(ROUTE_ID, HOST, ROUTE_PATH);
        route.setUpstreamName(STABLE_UPSTREAM);
        route.getPolicyNames().add(TRAFFIC_POLICY_NAME);
        route.getPolicyNames().add(RELEASE_POLICY_NAME);
        route.getPolicyNames().add(AUTH_POLICY_NAME);
        config.getSpec().getRoutes().add(route);
        config.getSpec().getPolicies().add(trafficPolicy());
        config.getSpec().getPolicies().add(releasePolicy());
        config.getSpec().getPolicies().add(authPolicy());

        CompiledProxyRuntime runtime = new PublishedConfigCompiler().compile(config);
        runtime.getPoliciesByName().values().forEach(policy -> policy.getConfig().clear());
        CompiledRoute compiledRoute = runtime.match(HOST, "/api/users");

        assertThat(new TrafficColorResolver().resolve(runtime, trafficColorRequest())).isEqualTo("green");
        assertThat(new ReleaseUpstreamResolver().resolve(runtime, compiledRoute, "canary"))
                .isEqualTo(CANARY_UPSTREAM);
        assertThat(new RateLimitPolicyResolver().resolve(runtime, compiledRoute)).isPresent();
        assertThat(new CircuitBreakerPolicyResolver().resolve(runtime, compiledRoute)).isPresent();
        assertThat(new RetryPolicyResolver().resolve(runtime, compiledRoute)).hasValueSatisfying(policy ->
                assertThat(policy.getMaxAttempts()).isEqualTo(3));
        assertThat(new RouteAccessEvaluator().evaluate(runtime,
                new RouteAccessRequest(HOST, "/api/users", "GET"))
                .authenticationRequired()).isTrue();
        assertThat(new RouteAccessEvaluator().evaluate(runtime,
                new RouteAccessRequest(HOST, "/api/public/readme", "GET"))
                .authenticationRequired()).isFalse();
    }

    private PublishedConfig config(String version, String configHash) {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion(version);
        config.getSpec().setConfigHash(configHash);
        return config;
    }

    private PublishedConfig.PublishedRoute route(String routeId, String host, String path) {
        PublishedConfig.PublishedRoute route = new PublishedConfig.PublishedRoute();
        route.setRouteId(routeId);
        if (host != null) {
            route.getHosts().add(host);
        }
        route.setPath(path);
        return route;
    }

    /**
     * 创建测试流量策略。
     *
     * @return 流量策略
     */
    private PublishedConfig.PublishedPolicy trafficPolicy() {
        Map<String, Object> rateLimit = new LinkedHashMap<>();
        rateLimit.put(PublishedConfigConstants.KEY_ENABLED, true);
        rateLimit.put(PublishedConfigConstants.KEY_REQUESTS_PER_SECOND, 100);
        Map<String, Object> circuitBreaker = new LinkedHashMap<>();
        circuitBreaker.put(PublishedConfigConstants.KEY_ENABLED, true);
        Map<String, Object> retry = new LinkedHashMap<>();
        retry.put(PublishedConfigConstants.KEY_ENABLED, true);
        retry.put(PublishedConfigConstants.KEY_MAX_ATTEMPTS, 3);
        Map<String, Object> colorRule = new LinkedHashMap<>();
        colorRule.put(PublishedConfigConstants.KEY_SOURCE, TrafficColorConstants.SOURCE_HEADER);
        colorRule.put(PublishedConfigConstants.KEY_KEY, "X-Beta");
        colorRule.put(PublishedConfigConstants.KEY_MATCH, "yes");
        colorRule.put(PublishedConfigConstants.KEY_COLOR, "green");
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName(TRAFFIC_POLICY_NAME);
        policy.setType(PublishedConfigConstants.POLICY_TYPE_TRAFFIC);
        policy.getConfig().put(PublishedConfigConstants.KEY_RATE_LIMIT, rateLimit);
        policy.getConfig().put(PublishedConfigConstants.KEY_CIRCUIT_BREAKER, circuitBreaker);
        policy.getConfig().put(PublishedConfigConstants.KEY_RETRY, retry);
        policy.getConfig().put(PublishedConfigConstants.KEY_COLOR_RULES, List.of(colorRule));
        return policy;
    }

    /**
     * 创建测试发布策略。
     *
     * @return 发布策略
     */
    private PublishedConfig.PublishedPolicy releasePolicy() {
        Map<String, Object> upstreamRef = new LinkedHashMap<>();
        upstreamRef.put(PublishedConfigConstants.KEY_NAME, CANARY_UPSTREAM);
        Map<String, Object> split = new LinkedHashMap<>();
        split.put(PublishedConfigConstants.KEY_TARGET, "canary");
        split.put(PublishedConfigConstants.KEY_COLOR, "canary");
        split.put(PublishedConfigConstants.KEY_WEIGHT, 100);
        split.put(PublishedConfigConstants.KEY_UPSTREAM_REF, upstreamRef);
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName(RELEASE_POLICY_NAME);
        policy.setType(PublishedConfigConstants.POLICY_TYPE_RELEASE);
        policy.getConfig().put(PublishedConfigConstants.KEY_TRAFFIC_SPLITS, List.of(split));
        return policy;
    }

    /**
     * 创建测试认证策略。
     *
     * @return 认证策略
     */
    private PublishedConfig.PublishedPolicy authPolicy() {
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName(AUTH_POLICY_NAME);
        policy.setType(PublishedConfigConstants.POLICY_TYPE_AUTH);
        policy.getConfig().put(PublishedConfigConstants.KEY_TYPE, AuthType.JWT.name());
        policy.getConfig().put(PublishedConfigConstants.KEY_PUBLIC_PATHS, List.of("/public"));
        return policy;
    }

    /**
     * 创建测试染色请求。
     *
     * @return 染色请求
     */
    private TrafficColorRequest trafficColorRequest() {
        return new TrafficColorRequest(
                HOST,
                "/api/users",
                null,
                headerName -> "X-Beta".equals(headerName) ? "yes" : null,
                cookieName -> null,
                queryName -> null,
                "127.0.0.1"
        );
    }
}
