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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * 从已编译策略中解析路由限流配置。
 */
public class RateLimitPolicyResolver {

    /**
     * 解析路由限流策略。
     *
     * @param runtime 已编译运行态
     * @param route 已命中路由
     * @return 限流策略
     */
    public Optional<CompiledRateLimitPolicy> resolve(CompiledProxyRuntime runtime, CompiledRoute route) {
        if (runtime == null || route == null) {
            return Optional.empty();
        }
        if (route.isPoliciesPrecompiled()) {
            return Optional.ofNullable(route.getRateLimitPolicy());
        }
        for (CompiledPolicy policy : policiesByType(runtime, route, PublishedConfigConstants.POLICY_TYPE_TRAFFIC)) {
            // 一个路由命中多个策略时，先使用第一个有有效规则的限流策略
            Optional<CompiledRateLimitPolicy> rateLimit = compile(policy, route);
            if (rateLimit.isPresent()) {
                return rateLimit;
            }
        }
        return Optional.empty();
    }

    /**
     * 按策略类型查找路由绑定策略。
     *
     * @param runtime 已编译运行态
     * @param route 已命中路由
     * @param type 策略类型
     * @return 策略列表
     */
    private List<CompiledPolicy> policiesByType(CompiledProxyRuntime runtime, CompiledRoute route, String type) {
        List<CompiledPolicy> policies = new ArrayList<>();
        for (String policyName : route.getPolicyNames()) {
            CompiledPolicy policy = runtime.getPoliciesByName().get(policyName);
            if (policy != null && type.equalsIgnoreCase(Objects.toString(policy.getType(), ""))) {
                policies.add(policy);
            }
        }
        return policies;
    }

    /**
     * 编译单个策略。
     *
     * @param policy 已编译策略
     * @param route 已命中路由
     * @return 限流策略
     */
    private Optional<CompiledRateLimitPolicy> compile(CompiledPolicy policy, CompiledRoute route) {
        Object config = policy.getConfig().get(PublishedConfigConstants.KEY_RATE_LIMIT);
        if (config instanceof TrafficPolicy.RateLimitPolicy source) {
            return compilePolicyObject(policy, route, source);
        }
        if (config instanceof Map<?, ?> map) {
            return compilePolicyMap(policy, route, map);
        }
        return Optional.empty();
    }

    /**
     * 编译对象形态策略。
     *
     * @param policy 已编译策略
     * @param route 已命中路由
     * @param source 对象形态配置
     * @return 限流策略
     */
    private Optional<CompiledRateLimitPolicy> compilePolicyObject(CompiledPolicy policy,
                                                                  CompiledRoute route,
                                                                  TrafficPolicy.RateLimitPolicy source) {
        if (!Boolean.TRUE.equals(source.getEnabled())) {
            return Optional.empty();
        }
        CompiledRateLimitPolicy target = basePolicy(policy, route);
        addRouteRule(target, route, policy, source.getRequestsPerSecond());
        for (TrafficPolicy.ParamLimitRule rule : paramRules(source)) {
            addParamRule(target, route, policy, rule, source.getRequestsPerSecond());
        }
        return usable(target);
    }

    /**
     * 编译 Map 形态策略。
     *
     * @param policy 已编译策略
     * @param route 已命中路由
     * @param source Map 形态配置
     * @return 限流策略
     */
    private Optional<CompiledRateLimitPolicy> compilePolicyMap(CompiledPolicy policy,
                                                               CompiledRoute route,
                                                               Map<?, ?> source) {
        if (!Boolean.TRUE.equals(booleanValue(source.get(PublishedConfigConstants.KEY_ENABLED)))) {
            return Optional.empty();
        }
        CompiledRateLimitPolicy target = basePolicy(policy, route);
        Integer routeRate = integerValue(source, PublishedConfigConstants.KEY_REQUESTS_PER_SECOND);
        addRouteRule(target, route, policy, routeRate);
        for (Object value : iterableValue(source, PublishedConfigConstants.KEY_PARAM_RULES)) {
            addParamRule(target, route, policy, value, routeRate);
        }
        return usable(target);
    }

    /**
     * 创建策略基础对象。
     *
     * @param policy 已编译策略
     * @param route 已命中路由
     * @return 策略基础对象
     */
    private CompiledRateLimitPolicy basePolicy(CompiledPolicy policy, CompiledRoute route) {
        CompiledRateLimitPolicy target = new CompiledRateLimitPolicy();
        // 限流器名称按路由和策略隔离，避免不同项目互相抢额度
        target.setName(route.getRouteId() + ProxyRateLimitConstants.LIMITER_KEY_SEPARATOR + policy.getName());
        target.setEnabled(true);
        return target;
    }

    /**
     * 添加路由级限流规则。
     *
     * @param target 目标策略
     * @param route 已命中路由
     * @param policy 已编译策略
     * @param requestsPerSecond 每秒请求数
     */
    private void addRouteRule(CompiledRateLimitPolicy target,
                              CompiledRoute route,
                              CompiledPolicy policy,
                              Integer requestsPerSecond) {
        Integer rate = positive(requestsPerSecond);
        if (rate == null) {
            return;
        }
        CompiledRateLimitRule rule = new CompiledRateLimitRule();
        rule.setLimiterNamePrefix(limiterName(route, policy, ProxyRateLimitConstants.ROUTE_SCOPE));
        rule.setRequestsPerSecond(rate);
        target.getRules().add(rule);
    }

    /**
     * 添加对象形态参数级限流规则。
     *
     * @param target 目标策略
     * @param route 已命中路由
     * @param policy 已编译策略
     * @param source 参数规则
     * @param defaultRequestsPerSecond 默认每秒请求数
     */
    private void addParamRule(CompiledRateLimitPolicy target,
                              CompiledRoute route,
                              CompiledPolicy policy,
                              TrafficPolicy.ParamLimitRule source,
                              Integer defaultRequestsPerSecond) {
        if (source == null) {
            return;
        }
        Integer rate = positive(source.getRequestsPerSecond());
        if (rate == null) {
            rate = positive(defaultRequestsPerSecond);
        }
        if (rate == null || !StringUtils.hasText(source.getSource())) {
            return;
        }
        CompiledRateLimitRule rule = new CompiledRateLimitRule();
        rule.setLimiterNamePrefix(limiterName(route, policy, ProxyRateLimitConstants.PARAM_SCOPE,
                source.getSource(), source.getName(), source.getValue()));
        rule.setSource(source.getSource());
        rule.setName(source.getName());
        rule.setValue(source.getValue());
        rule.setRequestsPerSecond(rate);
        target.getRules().add(rule);
    }

    /**
     * 添加 Map 形态参数级限流规则。
     *
     * @param target 目标策略
     * @param route 已命中路由
     * @param policy 已编译策略
     * @param source 参数规则
     * @param defaultRequestsPerSecond 默认每秒请求数
     */
    private void addParamRule(CompiledRateLimitPolicy target,
                              CompiledRoute route,
                              CompiledPolicy policy,
                              Object source,
                              Integer defaultRequestsPerSecond) {
        if (source instanceof TrafficPolicy.ParamLimitRule rule) {
            addParamRule(target, route, policy, rule, defaultRequestsPerSecond);
            return;
        }
        if (!(source instanceof Map<?, ?> map)) {
            return;
        }
        Integer rate = positive(integerValue(map, PublishedConfigConstants.KEY_REQUESTS_PER_SECOND));
        if (rate == null) {
            rate = positive(defaultRequestsPerSecond);
        }
        String sourceValue = stringValue(map, PublishedConfigConstants.KEY_SOURCE);
        String name = stringValue(map, PublishedConfigConstants.KEY_NAME);
        String value = stringValue(map, PublishedConfigConstants.KEY_VALUE);
        if (rate == null || !StringUtils.hasText(sourceValue)) {
            return;
        }
        CompiledRateLimitRule rule = new CompiledRateLimitRule();
        rule.setLimiterNamePrefix(limiterName(route, policy, ProxyRateLimitConstants.PARAM_SCOPE,
                sourceValue, name, value));
        rule.setSource(sourceValue);
        rule.setName(name);
        rule.setValue(value);
        rule.setRequestsPerSecond(rate);
        target.getRules().add(rule);
    }

    /**
     * 生成限流器名称。
     *
     * @param route 已命中路由
     * @param policy 已编译策略
     * @param parts 限流器名称片段
     * @return 限流器名称
     */
    private String limiterName(CompiledRoute route, CompiledPolicy policy, String... parts) {
        List<String> values = new ArrayList<>();
        values.add(route.getRouteId());
        values.add(policy.getName());
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                values.add(part.trim());
            }
        }
        return String.join(ProxyRateLimitConstants.LIMITER_KEY_SEPARATOR, values);
    }

    /**
     * 返回可用策略。
     *
     * @param target 目标策略
     * @return 可用策略
     */
    private Optional<CompiledRateLimitPolicy> usable(CompiledRateLimitPolicy target) {
        return target.getRules().isEmpty() ? Optional.empty() : Optional.of(target);
    }

    /**
     * 读取 Iterable 配置。
     *
     * @param source 配置 Map
     * @param key 配置键
     * @return Iterable 配置
     */
    private Iterable<?> iterableValue(Map<?, ?> source, String key) {
        Object value = source.get(key);
        return value instanceof Iterable<?> iterable ? iterable : List.of();
    }

    /**
     * 读取对象形态参数规则。
     *
     * @param source 对象形态配置
     * @return 参数规则列表
     */
    private List<TrafficPolicy.ParamLimitRule> paramRules(TrafficPolicy.RateLimitPolicy source) {
        return source.getParamRules() == null ? List.of() : source.getParamRules();
    }

    /**
     * 读取字符串配置。
     *
     * @param map 配置 Map
     * @param key 配置键
     * @return 字符串配置
     */
    private String stringValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : Objects.toString(value, null);
    }

    /**
     * 读取整数配置。
     *
     * @param map 配置 Map
     * @param key 配置键
     * @return 整数配置
     */
    private Integer integerValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            // JSON map 中数字可能以字符串形式出现
            return value == null ? null : Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 转换布尔配置。
     *
     * @param value 原始值
     * @return 布尔配置
     */
    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        // JSON map 中布尔值可能以字符串形式出现
        return value == null ? null : Boolean.valueOf(value.toString());
    }

    /**
     * 兜底正整数。
     *
     * @param value 原始值
     * @return 正整数
     */
    private Integer positive(Integer value) {
        return value == null || value <= 0 ? null : value;
    }
}
