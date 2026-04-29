/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dt.gatepilot.proxy.domain.runtime;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 路由熔断器测试。
 */
class RouteCircuitBreakerTest {

    /**
     * 测试熔断名称。
     */
    private static final String CIRCUIT_NAME = "admin:traffic-main";

    /**
     * 应在失败率达到阈值后打开熔断。
     */
    @Test
    void shouldOpenAfterFailureRateReached() {
        RouteCircuitBreaker circuitBreaker = new RouteCircuitBreaker();
        CompiledCircuitBreakerPolicy policy = policy();
        policy.setMinimumNumberOfCalls(4);

        record(circuitBreaker, policy, 1L, false);
        record(circuitBreaker, policy, 2L, true);
        record(circuitBreaker, policy, 3L, false);
        record(circuitBreaker, policy, 4L, true);

        assertThat(circuitBreaker.tryAcquire(policy, 5L)).isFalse();
    }

    /**
     * 应在打开状态拒绝请求。
     */
    @Test
    void shouldRejectRequestWhenOpen() {
        RouteCircuitBreaker circuitBreaker = new RouteCircuitBreaker();
        CompiledCircuitBreakerPolicy policy = policy();
        open(circuitBreaker, policy);

        assertThat(circuitBreaker.tryAcquire(policy, 5L)).isFalse();
    }

    /**
     * 应在半开探测成功后关闭熔断。
     */
    @Test
    void shouldCloseAfterHalfOpenSuccess() {
        RouteCircuitBreaker circuitBreaker = new RouteCircuitBreaker();
        CompiledCircuitBreakerPolicy policy = policy();
        open(circuitBreaker, policy);
        long halfOpenNanos = policy.getWaitDurationInOpenState().toNanos() + 5L;

        assertThat(circuitBreaker.tryAcquire(policy, halfOpenNanos)).isTrue();
        circuitBreaker.record(policy, false, false, halfOpenNanos + 1L);

        assertThat(circuitBreaker.tryAcquire(policy, halfOpenNanos + 2L)).isTrue();
    }

    /**
     * 应识别失败状态码。
     */
    @Test
    void shouldDetectFailureStatusCode() {
        RouteCircuitBreaker circuitBreaker = new RouteCircuitBreaker();
        CompiledCircuitBreakerPolicy policy = policy();

        assertThat(circuitBreaker.failureStatus(policy, 500)).isTrue();
        assertThat(circuitBreaker.failureStatus(policy, 200)).isFalse();
    }

    /**
     * 应识别慢调用。
     */
    @Test
    void shouldDetectSlowCall() {
        RouteCircuitBreaker circuitBreaker = new RouteCircuitBreaker();
        CompiledCircuitBreakerPolicy policy = policy();
        policy.setSlowCallDurationThreshold(Duration.ofNanos(10));

        assertThat(circuitBreaker.slowCall(policy, 1L, 11L)).isTrue();
        assertThat(circuitBreaker.slowCall(policy, 1L, 10L)).isFalse();
    }

    /**
     * 记录一次调用结果。
     *
     * @param circuitBreaker 路由熔断器
     * @param policy 熔断策略
     * @param nowNanos 当前时间
     * @param failure 是否失败
     */
    private void record(RouteCircuitBreaker circuitBreaker,
                        CompiledCircuitBreakerPolicy policy,
                        long nowNanos,
                        boolean failure) {
        assertThat(circuitBreaker.tryAcquire(policy, nowNanos)).isTrue();
        circuitBreaker.record(policy, failure, false, nowNanos);
    }

    /**
     * 打开熔断器。
     *
     * @param circuitBreaker 路由熔断器
     * @param policy 熔断策略
     */
    private void open(RouteCircuitBreaker circuitBreaker, CompiledCircuitBreakerPolicy policy) {
        record(circuitBreaker, policy, 1L, true);
        record(circuitBreaker, policy, 2L, true);
    }

    /**
     * 创建测试熔断策略。
     *
     * @return 测试熔断策略
     */
    private CompiledCircuitBreakerPolicy policy() {
        CompiledCircuitBreakerPolicy policy = new CompiledCircuitBreakerPolicy();
        policy.setName(CIRCUIT_NAME);
        policy.setSlidingWindowSize(4);
        policy.setMinimumNumberOfCalls(2);
        policy.setFailureRateThreshold(50);
        policy.setSlowCallRateThreshold(100);
        policy.setWaitDurationInOpenState(Duration.ofNanos(100));
        policy.setPermittedNumberOfCallsInHalfOpenState(1);
        policy.setStatusCodes(List.of(500, 502, 503, 504));
        return policy;
    }
}
