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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * 从已编译策略中解析路由熔断配置。
 */
public class CircuitBreakerPolicyResolver {

    /**
     * 解析路由熔断策略。
     *
     * @param runtime 已编译运行态
     * @param route 已命中路由
     * @return 熔断策略
     */
    public Optional<CompiledCircuitBreakerPolicy> resolve(CompiledProxyRuntime runtime, CompiledRoute route) {
        if (runtime == null || route == null) {
            return Optional.empty();
        }
        if (route.isPoliciesPrecompiled()) {
            return Optional.ofNullable(route.getCircuitBreakerPolicy());
        }
        for (CompiledPolicy policy : policiesByType(runtime, route, PublishedConfigConstants.POLICY_TYPE_TRAFFIC)) {
            // 一个路由命中多个策略时，先使用第一个启用的熔断策略
            Optional<CompiledCircuitBreakerPolicy> circuitBreaker = compile(policy, route);
            if (circuitBreaker.isPresent()) {
                return circuitBreaker;
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
     * @return 熔断策略
     */
    private Optional<CompiledCircuitBreakerPolicy> compile(CompiledPolicy policy, CompiledRoute route) {
        Object config = policy.getConfig().get(PublishedConfigConstants.KEY_CIRCUIT_BREAKER);
        if (config instanceof TrafficPolicy.CircuitBreakerPolicy source) {
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
     * @return 熔断策略
     */
    private Optional<CompiledCircuitBreakerPolicy> compilePolicyObject(CompiledPolicy policy,
                                                                       CompiledRoute route,
                                                                       TrafficPolicy.CircuitBreakerPolicy source) {
        if (!Boolean.TRUE.equals(source.getEnabled())) {
            return Optional.empty();
        }
        CompiledCircuitBreakerPolicy target = basePolicy(policy, route);
        // 策略对象来自控制器组装，字段语义保持新模型
        target.setSlidingWindowSize(positive(source.getSlidingWindowSize(),
                ProxyCircuitBreakerConstants.DEFAULT_SLIDING_WINDOW_SIZE));
        target.setMinimumNumberOfCalls(positive(source.getMinimumNumberOfCalls(),
                ProxyCircuitBreakerConstants.DEFAULT_MINIMUM_NUMBER_OF_CALLS));
        target.setFailureRateThreshold(positive(source.getFailureRateThreshold(),
                ProxyCircuitBreakerConstants.DEFAULT_FAILURE_RATE_THRESHOLD));
        target.setSlowCallRateThreshold(positive(source.getSlowCallRateThreshold(),
                ProxyCircuitBreakerConstants.DEFAULT_SLOW_CALL_RATE_THRESHOLD));
        target.setSlowCallDurationThreshold(optionalDuration(source.getSlowCallDurationThreshold()));
        target.setWaitDurationInOpenState(duration(source.getWaitDurationInOpenState(),
                ProxyCircuitBreakerConstants.DEFAULT_WAIT_DURATION_IN_OPEN_STATE));
        target.setPermittedNumberOfCallsInHalfOpenState(positive(
                source.getPermittedNumberOfCallsInHalfOpenState(), ProxyCircuitBreakerConstants.DEFAULT_HALF_OPEN_CALLS));
        target.setStatusCodes(statusCodes(source.getStatusCodes()));
        target.setFallbackStatus(httpStatus(source.getFallbackStatus(),
                ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_STATUS));
        target.setFallbackCode(positive(source.getFallbackCode(), ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_CODE));
        target.setFallbackMessage(text(source.getFallbackMessage(),
                ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_MESSAGE));
        target.setContentType(text(source.getContentType(), ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_CONTENT_TYPE));
        return Optional.of(target);
    }

    /**
     * 编译 Map 形态策略。
     *
     * @param policy 已编译策略
     * @param route 已命中路由
     * @param source Map 形态配置
     * @return 熔断策略
     */
    private Optional<CompiledCircuitBreakerPolicy> compilePolicyMap(CompiledPolicy policy,
                                                                    CompiledRoute route,
                                                                    Map<?, ?> source) {
        if (!Boolean.TRUE.equals(booleanValue(source.get(PublishedConfigConstants.KEY_ENABLED)))) {
            return Optional.empty();
        }
        CompiledCircuitBreakerPolicy target = basePolicy(policy, route);
        // Map 兼容 PublishedConfig JSON 反序列化后的结构
        target.setSlidingWindowSize(positive(integerValue(source, PublishedConfigConstants.KEY_SLIDING_WINDOW_SIZE),
                ProxyCircuitBreakerConstants.DEFAULT_SLIDING_WINDOW_SIZE));
        target.setMinimumNumberOfCalls(positive(integerValue(source,
                        PublishedConfigConstants.KEY_MINIMUM_NUMBER_OF_CALLS),
                ProxyCircuitBreakerConstants.DEFAULT_MINIMUM_NUMBER_OF_CALLS));
        target.setFailureRateThreshold(positive(integerValue(source,
                        PublishedConfigConstants.KEY_FAILURE_RATE_THRESHOLD),
                ProxyCircuitBreakerConstants.DEFAULT_FAILURE_RATE_THRESHOLD));
        target.setSlowCallRateThreshold(positive(integerValue(source,
                        PublishedConfigConstants.KEY_SLOW_CALL_RATE_THRESHOLD),
                ProxyCircuitBreakerConstants.DEFAULT_SLOW_CALL_RATE_THRESHOLD));
        target.setSlowCallDurationThreshold(optionalDuration(durationValue(source,
                PublishedConfigConstants.KEY_SLOW_CALL_DURATION_THRESHOLD)));
        target.setWaitDurationInOpenState(duration(durationValue(source,
                        PublishedConfigConstants.KEY_WAIT_DURATION_IN_OPEN_STATE),
                ProxyCircuitBreakerConstants.DEFAULT_WAIT_DURATION_IN_OPEN_STATE));
        target.setPermittedNumberOfCallsInHalfOpenState(positive(integerValue(source,
                        PublishedConfigConstants.KEY_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE),
                ProxyCircuitBreakerConstants.DEFAULT_HALF_OPEN_CALLS));
        target.setStatusCodes(statusCodes(iterableValue(source, PublishedConfigConstants.KEY_STATUS_CODES)));
        target.setFallbackStatus(httpStatus(integerValue(source, PublishedConfigConstants.KEY_FALLBACK_STATUS),
                ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_STATUS));
        target.setFallbackCode(positive(integerValue(source, PublishedConfigConstants.KEY_FALLBACK_CODE),
                ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_CODE));
        target.setFallbackMessage(text(stringValue(source, PublishedConfigConstants.KEY_FALLBACK_MESSAGE),
                ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_MESSAGE));
        target.setContentType(text(stringValue(source, PublishedConfigConstants.KEY_CONTENT_TYPE),
                ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_CONTENT_TYPE));
        return Optional.of(target);
    }

    /**
     * 创建策略基础对象。
     *
     * @param policy 已编译策略
     * @param route 已命中路由
     * @return 策略基础对象
     */
    private CompiledCircuitBreakerPolicy basePolicy(CompiledPolicy policy, CompiledRoute route) {
        CompiledCircuitBreakerPolicy target = new CompiledCircuitBreakerPolicy();
        // 熔断状态按路由和策略隔离，避免不同路由互相影响
        target.setName(route.getRouteId() + ProxyCircuitBreakerConstants.CIRCUIT_KEY_SEPARATOR + policy.getName());
        target.setEnabled(true);
        return target;
    }

    /**
     * 解析失败状态码列表。
     *
     * @param values 原始值列表
     * @return 状态码列表
     */
    private List<Integer> statusCodes(Iterable<?> values) {
        if (values == null) {
            return new ArrayList<>(ProxyCircuitBreakerConstants.DEFAULT_FAILURE_STATUS_CODES);
        }
        List<Integer> statusCodes = new ArrayList<>();
        for (Object value : values) {
            Integer statusCode = integerValue(value);
            if (statusCode != null && statusCode >= 100 && statusCode <= 599) {
                statusCodes.add(statusCode);
            }
        }
        return statusCodes.isEmpty()
                ? new ArrayList<>(ProxyCircuitBreakerConstants.DEFAULT_FAILURE_STATUS_CODES)
                : statusCodes;
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
        return value instanceof Iterable<?> iterable ? iterable : null;
    }

    /**
     * 读取 Duration 配置。
     *
     * @param source 配置 Map
     * @param key 配置键
     * @return Duration 配置
     */
    private Duration durationValue(Map<?, ?> source, String key) {
        Object value = source.get(key);
        if (value instanceof Duration duration) {
            return duration;
        }
        if (value instanceof Number number) {
            return Duration.ofMillis(number.longValue());
        }
        return parseDuration(Objects.toString(value, null));
    }

    /**
     * 解析 Duration 字符串。
     *
     * @param value 原始值
     * @return Duration
     */
    private Duration parseDuration(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        try {
            // 优先兼容 Java Duration 的 PT 表达式
            return Duration.parse(value.trim());
        } catch (RuntimeException exception) {
            return parseSimpleDuration(normalized);
        }
    }

    /**
     * 解析简写 Duration。
     *
     * @param value 原始值
     * @return Duration
     */
    private Duration parseSimpleDuration(String value) {
        try {
            // 简写只支持 ms 和 s，避免误读复杂表达式
            if (value.endsWith(ProxyCircuitBreakerConstants.DURATION_MILLIS_SUFFIX)) {
                return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
            }
            if (value.endsWith(ProxyCircuitBreakerConstants.DURATION_SECONDS_SUFFIX)) {
                return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
            }
            return Duration.ofMillis(Long.parseLong(value));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 兜底 Duration。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 兜底后的 Duration
     */
    private Duration duration(Duration value, Duration defaultValue) {
        return value == null || value.isNegative() || value.isZero() ? defaultValue : value;
    }

    /**
     * 过滤可选 Duration。
     *
     * @param value 原始值
     * @return 可用 Duration
     */
    private Duration optionalDuration(Duration value) {
        return value == null || value.isNegative() || value.isZero() ? null : value;
    }

    /**
     * 兜底正整数。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 兜底后的正整数
     */
    private int positive(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    /**
     * 兜底 HTTP 状态码。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 兜底后的 HTTP 状态码
     */
    private int httpStatus(Integer value, int defaultValue) {
        return value == null || value < 100 || value > 599 ? defaultValue : value;
    }

    /**
     * 兜底文本。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 兜底后的文本
     */
    private String text(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
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
        return integerValue(map.get(key));
    }

    /**
     * 转换整数配置。
     *
     * @param value 原始值
     * @return 整数配置
     */
    private Integer integerValue(Object value) {
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
}
