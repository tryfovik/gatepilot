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

/**
 * 从已编译策略中解析路由重试配置。
 */
public class RetryPolicyResolver {

    /**
     * 解析路由重试策略。
     *
     * @param runtime 已编译运行态
     * @param route 已命中路由
     * @return 重试策略
     */
    public Optional<CompiledRetryPolicy> resolve(CompiledProxyRuntime runtime, CompiledRoute route) {
        if (runtime == null || route == null) {
            return Optional.empty();
        }
        if (route.isPoliciesPrecompiled()) {
            return Optional.ofNullable(route.getRetryPolicy());
        }
        for (CompiledPolicy policy : policiesByType(runtime, route, PublishedConfigConstants.POLICY_TYPE_TRAFFIC)) {
            // 一个路由命中多个策略时，先使用第一个启用的重试策略
            Optional<CompiledRetryPolicy> retry = compile(policy, route);
            if (retry.isPresent()) {
                return retry;
            }
        }
        return Optional.empty();
    }

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

    private Optional<CompiledRetryPolicy> compile(CompiledPolicy policy, CompiledRoute route) {
        Object config = policy.getConfig().get(PublishedConfigConstants.KEY_RETRY);
        if (config instanceof TrafficPolicy.RetryPolicy source) {
            return compilePolicyObject(policy, route, source);
        }
        if (config instanceof Map<?, ?> map) {
            return compilePolicyMap(policy, route, map);
        }
        return Optional.empty();
    }

    private Optional<CompiledRetryPolicy> compilePolicyObject(CompiledPolicy policy,
                                                              CompiledRoute route,
                                                              TrafficPolicy.RetryPolicy source) {
        if (!Boolean.TRUE.equals(source.getEnabled())) {
            return Optional.empty();
        }
        CompiledRetryPolicy target = basePolicy(policy, route);
        target.setMaxAttempts(maxAttempts(source.getMaxAttempts()));
        target.setStatuses(statuses(source.getStatuses()));
        target.setFirstBackoff(duration(source.getFirstBackoff(), ProxyRetryConstants.DEFAULT_FIRST_BACKOFF));
        target.setMaxBackoff(duration(source.getMaxBackoff(), ProxyRetryConstants.DEFAULT_MAX_BACKOFF));
        return usable(target);
    }

    private Optional<CompiledRetryPolicy> compilePolicyMap(CompiledPolicy policy,
                                                           CompiledRoute route,
                                                           Map<?, ?> source) {
        if (!Boolean.TRUE.equals(booleanValue(source.get(PublishedConfigConstants.KEY_ENABLED)))) {
            return Optional.empty();
        }
        CompiledRetryPolicy target = basePolicy(policy, route);
        target.setMaxAttempts(maxAttempts(integerValue(source, PublishedConfigConstants.KEY_MAX_ATTEMPTS)));
        target.setStatuses(statuses(iterableValue(source, PublishedConfigConstants.KEY_STATUSES)));
        target.setFirstBackoff(duration(
                durationValue(source, PublishedConfigConstants.KEY_FIRST_BACKOFF),
                ProxyRetryConstants.DEFAULT_FIRST_BACKOFF
        ));
        target.setMaxBackoff(duration(
                durationValue(source, PublishedConfigConstants.KEY_MAX_BACKOFF),
                ProxyRetryConstants.DEFAULT_MAX_BACKOFF
        ));
        return usable(target);
    }

    private CompiledRetryPolicy basePolicy(CompiledPolicy policy, CompiledRoute route) {
        CompiledRetryPolicy target = new CompiledRetryPolicy();
        // 重试按路由和策略隔离，便于审计和后续指标打点
        target.setName(route.getRouteId() + ProxyRetryConstants.RETRY_KEY_SEPARATOR + policy.getName());
        target.setEnabled(true);
        return target;
    }

    private Optional<CompiledRetryPolicy> usable(CompiledRetryPolicy target) {
        return target.getMaxAttempts() <= 1 ? Optional.empty() : Optional.of(target);
    }

    private int maxAttempts(Integer value) {
        if (value == null || value <= 1) {
            return ProxyRetryConstants.DEFAULT_MAX_ATTEMPTS;
        }
        return Math.min(value, ProxyRetryConstants.MAX_ALLOWED_ATTEMPTS);
    }

    private List<Integer> statuses(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>(ProxyRetryConstants.DEFAULT_RETRY_STATUSES);
        }
        return statusList(values);
    }

    private List<Integer> statuses(Iterable<?> values) {
        if (values == null) {
            return new ArrayList<>(ProxyRetryConstants.DEFAULT_RETRY_STATUSES);
        }
        List<Integer> statuses = new ArrayList<>();
        for (Object value : values) {
            Integer status = integerValue(value);
            if (status != null && status >= 100 && status <= 599) {
                statuses.add(status);
            }
        }
        return statuses.isEmpty() ? new ArrayList<>(ProxyRetryConstants.DEFAULT_RETRY_STATUSES) : statuses;
    }

    private List<Integer> statusList(List<Integer> values) {
        List<Integer> statuses = new ArrayList<>();
        for (Integer value : values) {
            if (value != null && value >= 100 && value <= 599) {
                statuses.add(value);
            }
        }
        return statuses.isEmpty() ? new ArrayList<>(ProxyRetryConstants.DEFAULT_RETRY_STATUSES) : statuses;
    }

    private Iterable<?> iterableValue(Map<?, ?> source, String key) {
        Object value = source.get(key);
        return value instanceof Iterable<?> iterable ? iterable : null;
    }

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

    private Duration parseDuration(String value) {
        if (value == null || value.isBlank()) {
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

    private Duration parseSimpleDuration(String value) {
        try {
            if (value.endsWith(ProxyRetryConstants.DURATION_MILLIS_SUFFIX)) {
                return Duration.ofMillis(Long.parseLong(value.substring(
                        0, value.length() - ProxyRetryConstants.DURATION_MILLIS_SUFFIX.length())));
            }
            if (value.endsWith(ProxyRetryConstants.DURATION_SECONDS_SUFFIX)) {
                return Duration.ofSeconds(Long.parseLong(value.substring(
                        0, value.length() - ProxyRetryConstants.DURATION_SECONDS_SUFFIX.length())));
            }
            return Duration.ofMillis(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Duration duration(Duration value, Duration defaultValue) {
        return value == null || value.isNegative() ? defaultValue : value;
    }

    private Integer integerValue(Map<?, ?> map, String key) {
        return integerValue(map.get(key));
    }

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

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        // JSON map 中布尔值可能以字符串形式出现
        return value == null ? null : Boolean.valueOf(value.toString());
    }
}
