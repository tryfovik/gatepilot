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

import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfigConstants;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 限流策略解析器测试。
 */
class RateLimitPolicyResolverTest {

    /**
     * 测试路由标识。
     */
    private static final String ROUTE_ID = "admin";

    /**
     * 测试策略名称。
     */
    private static final String POLICY_NAME = "traffic-main";

    /**
     * 应支持对象形态限流配置。
     */
    @Test
    void shouldResolveObjectRateLimitPolicy() {
        TrafficPolicy.RateLimitPolicy source = new TrafficPolicy.RateLimitPolicy();
        source.setEnabled(true);
        source.setRequestsPerSecond(100);
        TrafficPolicy.ParamLimitRule paramRule = new TrafficPolicy.ParamLimitRule();
        paramRule.setSource(ProxyRateLimitConstants.SOURCE_QUERY);
        paramRule.setName("tenant");
        paramRule.setValue("vip");
        paramRule.setRequestsPerSecond(10);
        source.getParamRules().add(paramRule);

        Optional<CompiledRateLimitPolicy> resolved = new RateLimitPolicyResolver().resolve(runtime(source), route());

        assertThat(resolved).isPresent();
        assertThat(resolved.get().getName()).isEqualTo(ROUTE_ID + ":" + POLICY_NAME);
        assertThat(resolved.get().getRules()).hasSize(2);
        assertThat(resolved.get().getRules().get(0).getRequestsPerSecond()).isEqualTo(100);
        assertThat(resolved.get().getRules().get(1).getRequestsPerSecond()).isEqualTo(10);
        assertThat(resolved.get().getRules().get(1).matches(queryRequest("vip"))).isTrue();
        assertThat(resolved.get().getRules().get(1).matches(queryRequest("normal"))).isFalse();
    }

    /**
     * 应支持 Map 形态限流配置。
     */
    @Test
    void shouldResolveMapRateLimitPolicy() {
        Map<String, Object> paramRule = new LinkedHashMap<>();
        paramRule.put(PublishedConfigConstants.KEY_SOURCE, ProxyRateLimitConstants.SOURCE_HEADER);
        paramRule.put(PublishedConfigConstants.KEY_NAME, "X-Tenant");
        paramRule.put(PublishedConfigConstants.KEY_REQUESTS_PER_SECOND, "20");
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(PublishedConfigConstants.KEY_ENABLED, "true");
        source.put(PublishedConfigConstants.KEY_REQUESTS_PER_SECOND, "200");
        source.put(PublishedConfigConstants.KEY_PARAM_RULES, List.of(paramRule));

        Optional<CompiledRateLimitPolicy> resolved = new RateLimitPolicyResolver().resolve(runtime(source), route());
        RateLimitRequest request = headerRequest("vip");

        assertThat(resolved).isPresent();
        assertThat(resolved.get().matchingRules(request)).hasSize(2);
        assertThat(resolved.get().matchingRules(request).get(1).limiterName(request))
                .isEqualTo("admin:traffic-main:param:header:X-Tenant:vip");
    }

    /**
     * 应忽略未启用的限流配置。
     */
    @Test
    void shouldIgnoreDisabledRateLimitPolicy() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(PublishedConfigConstants.KEY_ENABLED, false);
        source.put(PublishedConfigConstants.KEY_REQUESTS_PER_SECOND, 100);

        Optional<CompiledRateLimitPolicy> resolved = new RateLimitPolicyResolver().resolve(runtime(source), route());

        assertThat(resolved).isEmpty();
    }

    /**
     * 应忽略没有有效规则的限流配置。
     */
    @Test
    void shouldIgnoreRateLimitPolicyWithoutValidRules() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(PublishedConfigConstants.KEY_ENABLED, true);
        source.put(PublishedConfigConstants.KEY_REQUESTS_PER_SECOND, 0);

        Optional<CompiledRateLimitPolicy> resolved = new RateLimitPolicyResolver().resolve(runtime(source), route());

        assertThat(resolved).isEmpty();
    }

    /**
     * 创建测试运行态。
     *
     * @param rateLimit 限流配置
     * @return 测试运行态
     */
    private CompiledProxyRuntime runtime(Object rateLimit) {
        CompiledPolicy policy = new CompiledPolicy();
        policy.setName(POLICY_NAME);
        policy.setType(PublishedConfigConstants.POLICY_TYPE_TRAFFIC);
        policy.getConfig().put(PublishedConfigConstants.KEY_RATE_LIMIT, rateLimit);

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

    /**
     * 创建 Query 请求。
     *
     * @param tenant 租户值
     * @return 限流请求
     */
    private RateLimitRequest queryRequest(String tenant) {
        return new RateLimitRequest(
                "api.example.com",
                name -> null,
                name -> null,
                name -> "tenant".equals(name) ? tenant : null,
                "127.0.0.1"
        );
    }

    /**
     * 创建 Header 请求。
     *
     * @param tenant 租户值
     * @return 限流请求
     */
    private RateLimitRequest headerRequest(String tenant) {
        return new RateLimitRequest(
                "api.example.com",
                name -> "X-Tenant".equals(name) ? tenant : null,
                name -> null,
                name -> null,
                "127.0.0.1"
        );
    }
}
