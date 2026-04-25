package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfigConstants;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 熔断策略解析器测试。
 */
class CircuitBreakerPolicyResolverTest {

    /**
     * 测试路由标识。
     */
    private static final String ROUTE_ID = "admin";

    /**
     * 测试策略名称。
     */
    private static final String POLICY_NAME = "traffic-main";

    /**
     * 应支持对象形态熔断配置。
     */
    @Test
    void shouldResolveObjectCircuitBreakerPolicy() {
        TrafficPolicy.CircuitBreakerPolicy source = new TrafficPolicy.CircuitBreakerPolicy();
        source.setEnabled(true);
        source.setSlidingWindowSize(5);
        source.setMinimumNumberOfCalls(3);
        source.setFailureRateThreshold(60);
        source.setSlowCallRateThreshold(70);
        source.setSlowCallDurationThreshold(Duration.ofSeconds(1));
        source.setWaitDurationInOpenState(Duration.ofSeconds(2));
        source.setPermittedNumberOfCallsInHalfOpenState(2);
        source.setStatusCodes(List.of(502));
        source.setFallbackStatus(429);
        source.setFallbackCode(90001);
        source.setFallbackMessage("服务繁忙");
        source.setContentType("application/json;charset=UTF-8");

        Optional<CompiledCircuitBreakerPolicy> resolved = new CircuitBreakerPolicyResolver()
                .resolve(runtime(source), route());

        assertThat(resolved).isPresent();
        assertThat(resolved.get().getName()).isEqualTo(ROUTE_ID + ":" + POLICY_NAME);
        assertThat(resolved.get().getSlidingWindowSize()).isEqualTo(5);
        assertThat(resolved.get().getMinimumNumberOfCalls()).isEqualTo(3);
        assertThat(resolved.get().getFailureRateThreshold()).isEqualTo(60);
        assertThat(resolved.get().getSlowCallRateThreshold()).isEqualTo(70);
        assertThat(resolved.get().getSlowCallDurationThreshold()).isEqualTo(Duration.ofSeconds(1));
        assertThat(resolved.get().getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(2));
        assertThat(resolved.get().getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(2);
        assertThat(resolved.get().getStatusCodes()).containsExactly(502);
        assertThat(resolved.get().getFallbackStatus()).isEqualTo(429);
        assertThat(resolved.get().getFallbackCode()).isEqualTo(90001);
        assertThat(resolved.get().getFallbackMessage()).isEqualTo("服务繁忙");
        assertThat(resolved.get().getContentType()).isEqualTo("application/json;charset=UTF-8");
    }

    /**
     * 应支持 Map 形态熔断配置。
     */
    @Test
    void shouldResolveMapCircuitBreakerPolicy() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(PublishedConfigConstants.KEY_ENABLED, "true");
        source.put(PublishedConfigConstants.KEY_SLIDING_WINDOW_SIZE, "4");
        source.put(PublishedConfigConstants.KEY_MINIMUM_NUMBER_OF_CALLS, "2");
        source.put(PublishedConfigConstants.KEY_FAILURE_RATE_THRESHOLD, "50");
        source.put(PublishedConfigConstants.KEY_SLOW_CALL_RATE_THRESHOLD, "80");
        source.put(PublishedConfigConstants.KEY_SLOW_CALL_DURATION_THRESHOLD, "1500ms");
        source.put(PublishedConfigConstants.KEY_WAIT_DURATION_IN_OPEN_STATE, "2s");
        source.put(PublishedConfigConstants.KEY_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE, "1");
        source.put(PublishedConfigConstants.KEY_STATUS_CODES, List.of("500", "bad", 599));
        source.put(PublishedConfigConstants.KEY_FALLBACK_STATUS, "503");
        source.put(PublishedConfigConstants.KEY_FALLBACK_CODE, "90002");
        source.put(PublishedConfigConstants.KEY_FALLBACK_MESSAGE, "上游暂不可用");

        Optional<CompiledCircuitBreakerPolicy> resolved = new CircuitBreakerPolicyResolver()
                .resolve(runtime(source), route());

        assertThat(resolved).isPresent();
        assertThat(resolved.get().getSlidingWindowSize()).isEqualTo(4);
        assertThat(resolved.get().getMinimumNumberOfCalls()).isEqualTo(2);
        assertThat(resolved.get().getFailureRateThreshold()).isEqualTo(50);
        assertThat(resolved.get().getSlowCallRateThreshold()).isEqualTo(80);
        assertThat(resolved.get().getSlowCallDurationThreshold()).isEqualTo(Duration.ofMillis(1500));
        assertThat(resolved.get().getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(2));
        assertThat(resolved.get().getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1);
        assertThat(resolved.get().getStatusCodes()).containsExactly(500, 599);
        assertThat(resolved.get().getFallbackStatus()).isEqualTo(503);
        assertThat(resolved.get().getFallbackCode()).isEqualTo(90002);
        assertThat(resolved.get().getFallbackMessage()).isEqualTo("上游暂不可用");
    }

    /**
     * 应对非法配置使用默认值兜底。
     */
    @Test
    void shouldUseDefaultValuesWhenConfigInvalid() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(PublishedConfigConstants.KEY_ENABLED, true);
        source.put(PublishedConfigConstants.KEY_SLIDING_WINDOW_SIZE, -1);
        source.put(PublishedConfigConstants.KEY_MINIMUM_NUMBER_OF_CALLS, 0);
        source.put(PublishedConfigConstants.KEY_FAILURE_RATE_THRESHOLD, -1);
        source.put(PublishedConfigConstants.KEY_SLOW_CALL_RATE_THRESHOLD, 0);
        source.put(PublishedConfigConstants.KEY_SLOW_CALL_DURATION_THRESHOLD, "0ms");
        source.put(PublishedConfigConstants.KEY_WAIT_DURATION_IN_OPEN_STATE, "0ms");
        source.put(PublishedConfigConstants.KEY_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE, -1);
        source.put(PublishedConfigConstants.KEY_STATUS_CODES, List.of("bad"));
        source.put(PublishedConfigConstants.KEY_FALLBACK_STATUS, 99);
        source.put(PublishedConfigConstants.KEY_FALLBACK_CODE, -1);
        source.put(PublishedConfigConstants.KEY_FALLBACK_MESSAGE, " ");

        Optional<CompiledCircuitBreakerPolicy> resolved = new CircuitBreakerPolicyResolver()
                .resolve(runtime(source), route());

        assertThat(resolved).isPresent();
        assertThat(resolved.get().getSlidingWindowSize())
                .isEqualTo(ProxyCircuitBreakerConstants.DEFAULT_SLIDING_WINDOW_SIZE);
        assertThat(resolved.get().getMinimumNumberOfCalls())
                .isEqualTo(ProxyCircuitBreakerConstants.DEFAULT_MINIMUM_NUMBER_OF_CALLS);
        assertThat(resolved.get().getFailureRateThreshold())
                .isEqualTo(ProxyCircuitBreakerConstants.DEFAULT_FAILURE_RATE_THRESHOLD);
        assertThat(resolved.get().getSlowCallRateThreshold())
                .isEqualTo(ProxyCircuitBreakerConstants.DEFAULT_SLOW_CALL_RATE_THRESHOLD);
        assertThat(resolved.get().getSlowCallDurationThreshold()).isNull();
        assertThat(resolved.get().getWaitDurationInOpenState())
                .isEqualTo(ProxyCircuitBreakerConstants.DEFAULT_WAIT_DURATION_IN_OPEN_STATE);
        assertThat(resolved.get().getPermittedNumberOfCallsInHalfOpenState())
                .isEqualTo(ProxyCircuitBreakerConstants.DEFAULT_HALF_OPEN_CALLS);
        assertThat(resolved.get().getStatusCodes())
                .containsExactlyElementsOf(ProxyCircuitBreakerConstants.DEFAULT_FAILURE_STATUS_CODES);
        assertThat(resolved.get().getFallbackStatus())
                .isEqualTo(ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_STATUS);
        assertThat(resolved.get().getFallbackCode())
                .isEqualTo(ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_CODE);
        assertThat(resolved.get().getFallbackMessage())
                .isEqualTo(ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_MESSAGE);
    }

    /**
     * 应忽略未启用的熔断配置。
     */
    @Test
    void shouldIgnoreDisabledCircuitBreakerPolicy() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(PublishedConfigConstants.KEY_ENABLED, false);

        Optional<CompiledCircuitBreakerPolicy> resolved = new CircuitBreakerPolicyResolver()
                .resolve(runtime(source), route());

        assertThat(resolved).isEmpty();
    }

    /**
     * 创建测试运行态。
     *
     * @param circuitBreaker 熔断配置
     * @return 测试运行态
     */
    private CompiledProxyRuntime runtime(Object circuitBreaker) {
        CompiledPolicy policy = new CompiledPolicy();
        policy.setName(POLICY_NAME);
        policy.setType(PublishedConfigConstants.POLICY_TYPE_TRAFFIC);
        policy.getConfig().put(PublishedConfigConstants.KEY_CIRCUIT_BREAKER, circuitBreaker);

        CompiledProxyRuntime runtime = new CompiledProxyRuntime();
        runtime.getPoliciesByName().put(POLICY_NAME, policy);
        return runtime;
    }

    /**
     * 创建测试路由。
     *
     * @return 测试路由
     */
    private CompiledRoute route() {
        CompiledRoute route = new CompiledRoute();
        route.setRouteId(ROUTE_ID);
        route.getPolicyNames().add(POLICY_NAME);
        return route;
    }
}
