package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.TrafficColorSource;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfigConstants;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * 基于已发布运行态解析流量颜色。
 */
public class TrafficColorResolver {

    private static final Pattern COLOR_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}$");

    /**
     * 解析流量颜色。
     *
     * @param runtime 已编译运行态
     * @param request 请求上下文
     * @return 流量颜色，无法解析时返回 null
     */
    public String resolve(CompiledProxyRuntime runtime, TrafficColorRequest request) {
        if (runtime == null || request == null) {
            return null;
        }
        CompiledRoute route = runtime.match(request.host(), request.path());
        if (route == null) {
            return null;
        }
        CompiledTrafficColorPolicy policy = trafficColorPolicy(runtime, route);
        String trustedHeaderColor = resolveTrustedHeaderColor(policy, request);
        if (trustedHeaderColor != null) {
            return trustedHeaderColor;
        }
        String ruleColor = resolveRuleColor(policy, request);
        if (ruleColor != null) {
            return ruleColor;
        }
        String weightedColor = resolveWeightedColor(policy, route, request);
        if (weightedColor != null) {
            return weightedColor;
        }
        return policy.getDefaultColor();
    }

    /**
     * 预编译流量染色策略。
     *
     * @param runtime 已编译运行态
     * @param route 已命中路由
     * @return 预编译流量染色策略
     */
    public CompiledTrafficColorPolicy compile(CompiledProxyRuntime runtime, CompiledRoute route) {
        CompiledTrafficColorPolicy target = new CompiledTrafficColorPolicy();
        if (runtime == null || route == null) {
            return target;
        }
        boolean headerNameConfigured = false;
        boolean defaultColorConfigured = false;
        for (CompiledPolicy policy : policiesByType(runtime, route, PublishedConfigConstants.POLICY_TYPE_TRAFFIC)) {
            if (Boolean.TRUE.equals(booleanValue(policy.getConfig().get(
                    PublishedConfigConstants.KEY_TRUST_REQUEST_HEADER)))) {
                target.setTrustRequestHeader(true);
            }
            if (!headerNameConfigured) {
                String headerName = normalizeText(stringValue(policy.getConfig(),
                        PublishedConfigConstants.KEY_HEADER_NAME));
                if (headerName != null) {
                    target.setHeaderName(headerName);
                    headerNameConfigured = true;
                }
            }
            if (!defaultColorConfigured) {
                String defaultColor = normalizeColor(stringValue(policy.getConfig(),
                        PublishedConfigConstants.KEY_DEFAULT_COLOR), null);
                if (defaultColor != null) {
                    target.setDefaultColor(defaultColor);
                    defaultColorConfigured = true;
                }
            }
            target.getRules().addAll(trafficColorRules(policy));
        }
        for (CompiledPolicy policy : policiesByType(runtime, route, PublishedConfigConstants.POLICY_TYPE_RELEASE)) {
            List<CompiledTrafficSplit> splits = trafficSplits(policy);
            if (!splits.isEmpty()) {
                target.getWeightedPolicies().add(new CompiledWeightedTrafficPolicy(
                        policy.getName(),
                        weightHashHeaders(policy),
                        splits
                ));
            }
        }
        return target;
    }

    private CompiledTrafficColorPolicy trafficColorPolicy(CompiledProxyRuntime runtime, CompiledRoute route) {
        if (route.isPoliciesPrecompiled()) {
            return route.getTrafficColorPolicy() == null
                    ? new CompiledTrafficColorPolicy()
                    : route.getTrafficColorPolicy();
        }
        return route.getTrafficColorPolicy() == null ? compile(runtime, route) : route.getTrafficColorPolicy();
    }

    private String resolveTrustedHeaderColor(CompiledTrafficColorPolicy policy,
                                             TrafficColorRequest request) {
        if (!policy.isTrustRequestHeader()) {
            return null;
        }
        return normalizeColor(request.header(policy.getHeaderName()), null);
    }

    private String resolveRuleColor(CompiledTrafficColorPolicy policy,
                                    TrafficColorRequest request) {
        for (CompiledTrafficColorRule rule : policy.getRules()) {
            String candidate = extractCandidate(request, rule.source(), rule.fieldName());
            if (candidate != null && rule.matches(candidate)) {
                return rule.color();
            }
        }
        return null;
    }

    private String resolveWeightedColor(CompiledTrafficColorPolicy policy,
                                        CompiledRoute route,
                                        TrafficColorRequest request) {
        for (CompiledWeightedTrafficPolicy weightedPolicy : policy.getWeightedPolicies()) {
            int bucket = calculateBucket(buildWeightedHashKey(weightedPolicy, route, request));
            int cumulativeWeight = 0;
            for (CompiledTrafficSplit split : weightedPolicy.splits()) {
                cumulativeWeight = Math.min(TrafficColorConstants.WEIGHT_BUCKET_SIZE,
                        cumulativeWeight + split.weight());
                if (bucket < cumulativeWeight) {
                    return split.color();
                }
            }
        }
        return null;
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

    private List<CompiledTrafficColorRule> trafficColorRules(CompiledPolicy policy) {
        Object rules = policy.getConfig().get(PublishedConfigConstants.KEY_COLOR_RULES);
        if (!(rules instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<CompiledTrafficColorRule> parsedRules = new ArrayList<>();
        for (Object item : iterable) {
            // 单条规则解析失败时跳过，不影响其他规则
            CompiledTrafficColorRule rule = trafficColorRule(item);
            if (rule != null) {
                parsedRules.add(rule);
            }
        }
        return parsedRules;
    }

    private CompiledTrafficColorRule trafficColorRule(Object item) {
        if (item instanceof TrafficPolicy.TrafficColorRule rule) {
            return trafficColorRule(
                    sourceName(rule.getSource()),
                    rule.getKey(),
                    rule.getMatch(),
                    TrafficColorConstants.MATCH_EXACT,
                    rule.getColor()
            );
        }
        if (item instanceof Map<?, ?> map) {
            return trafficColorRule(
                    stringValue(map, PublishedConfigConstants.KEY_SOURCE),
                    firstText(map, PublishedConfigConstants.KEY_KEY, PublishedConfigConstants.KEY_FIELD_NAME,
                            PublishedConfigConstants.KEY_NAME),
                    firstText(map, PublishedConfigConstants.KEY_MATCH, PublishedConfigConstants.KEY_PATTERN,
                            PublishedConfigConstants.KEY_VALUE),
                    Optional.ofNullable(stringValue(map, PublishedConfigConstants.KEY_MATCH_STRATEGY))
                            .orElse(TrafficColorConstants.MATCH_EXACT),
                    stringValue(map, PublishedConfigConstants.KEY_COLOR)
            );
        }
        return null;
    }

    private CompiledTrafficColorRule trafficColorRule(String source,
                                                      String fieldName,
                                                      String pattern,
                                                      String matchStrategy,
                                                      String color) {
        String normalizedSource = normalizeLiteral(source, TrafficColorConstants.SOURCE_HEADER);
        String normalizedFieldName = normalizeText(fieldName);
        String normalizedPattern = normalizeText(pattern);
        String normalizedMatchStrategy = normalizeLiteral(matchStrategy, TrafficColorConstants.MATCH_EXACT);
        String normalizedColor = normalizeColor(color, null);
        if (ipSource(normalizedSource) && normalizedFieldName == null) {
            normalizedFieldName = TrafficColorConstants.FIELD_REMOTE_ADDRESS;
        }
        if (normalizedFieldName == null || normalizedPattern == null || normalizedColor == null) {
            return null;
        }
        Pattern regexPattern = TrafficColorConstants.MATCH_REGEX.equals(normalizedMatchStrategy)
                ? Pattern.compile(normalizedPattern)
                : null;
        return new CompiledTrafficColorRule(
                normalizedSource,
                normalizedFieldName,
                normalizedPattern,
                normalizedMatchStrategy,
                normalizedColor,
                regexPattern
        );
    }

    private List<CompiledTrafficSplit> trafficSplits(CompiledPolicy policy) {
        Object splits = policy.getConfig().get(PublishedConfigConstants.KEY_TRAFFIC_SPLITS);
        if (!(splits instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<CompiledTrafficSplit> parsedSplits = new ArrayList<>();
        for (Object item : iterable) {
            // 权重配置非法时跳过当前分组
            CompiledTrafficSplit split = trafficSplit(item);
            if (split != null) {
                parsedSplits.add(split);
            }
        }
        return parsedSplits;
    }

    private CompiledTrafficSplit trafficSplit(Object item) {
        if (item instanceof ReleasePolicy.TrafficSplit split) {
            return trafficSplit(split.getTarget(), split.getColor(), split.getWeight());
        }
        if (item instanceof Map<?, ?> map) {
            return trafficSplit(stringValue(map, PublishedConfigConstants.KEY_TARGET),
                    stringValue(map, PublishedConfigConstants.KEY_COLOR),
                    integerValue(map, PublishedConfigConstants.KEY_WEIGHT));
        }
        return null;
    }

    private CompiledTrafficSplit trafficSplit(String target, String color, Integer weight) {
        if (weight == null || weight <= 0) {
            return null;
        }
        String normalizedColor = normalizeColor(color, null);
        if (normalizedColor == null) {
            normalizedColor = normalizeColor(target, null);
        }
        if (normalizedColor == null) {
            return null;
        }
        return new CompiledTrafficSplit(normalizedColor, Math.min(TrafficColorConstants.WEIGHT_BUCKET_SIZE, weight));
    }

    private String extractCandidate(TrafficColorRequest request, String source, String fieldName) {
        if (ipSource(source)) {
            return request.remoteAddress();
        }
        return switch (source) {
            case TrafficColorConstants.SOURCE_COOKIE -> request.cookie(fieldName);
            case TrafficColorConstants.SOURCE_QUERY -> request.query(fieldName);
            default -> request.header(fieldName);
        };
    }

    private boolean ipSource(String source) {
        return TrafficColorConstants.SOURCE_IP.equals(source)
                || TrafficColorConstants.SOURCE_CLIENT_IP.equals(source)
                || TrafficColorConstants.SOURCE_CLIENT_IP_UNDERSCORE.equals(source)
                || TrafficColorConstants.SOURCE_REMOTE_ADDRESS.equals(source)
                || TrafficColorConstants.SOURCE_REMOTE_ADDRESS_UNDERSCORE.equals(source)
                || TrafficColorConstants.SOURCE_REMOTE_ADDRESS_COMPACT.equals(source);
    }

    private String buildWeightedHashKey(CompiledWeightedTrafficPolicy policy,
                                        CompiledRoute route,
                                        TrafficColorRequest request) {
        String identity = firstHeaderValue(request, policy.weightHashHeaders());
        if (identity == null) {
            identity = normalizeText(request.remoteAddress());
        }
        if (identity == null) {
            identity = normalizeText(request.rawQuery()) == null
                    ? request.path()
                    : request.path() + TrafficColorConstants.QUERY_SEPARATOR + request.rawQuery();
        }
        return route.getRouteId() + TrafficColorConstants.HASH_KEY_SEPARATOR + identity;
    }

    private List<String> weightHashHeaders(CompiledPolicy policy) {
        Object headers = policy.getConfig().get(PublishedConfigConstants.KEY_WEIGHT_HASH_HEADERS);
        if (!(headers instanceof Iterable<?> iterable)) {
            return TrafficColorConstants.DEFAULT_WEIGHT_HASH_HEADERS;
        }
        List<String> parsedHeaders = new ArrayList<>();
        for (Object item : iterable) {
            String header = normalizeText(Objects.toString(item, null));
            if (header != null) {
                parsedHeaders.add(header);
            }
        }
        return parsedHeaders.isEmpty() ? TrafficColorConstants.DEFAULT_WEIGHT_HASH_HEADERS : List.copyOf(parsedHeaders);
    }

    private String firstHeaderValue(TrafficColorRequest request, List<String> headerNames) {
        for (String headerName : headerNames) {
            String value = normalizeText(request.header(headerName));
            if (value != null) {
                return headerName + TrafficColorConstants.HEADER_VALUE_SEPARATOR + value;
            }
        }
        return null;
    }

    private int calculateBucket(String value) {
        CRC32 crc32 = new CRC32();
        crc32.update(Objects.toString(value, "").getBytes(StandardCharsets.UTF_8));
        return (int) (crc32.getValue() % TrafficColorConstants.WEIGHT_BUCKET_SIZE);
    }

    private String sourceName(TrafficColorSource source) {
        return source == null ? null : source.name();
    }

    private String firstText(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            // firstText 按兼容顺序取第一个非空值
            String value = stringValue(map, key);
            if (normalizeText(value) != null) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : Objects.toString(value, null);
    }

    private Integer integerValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            // 配置里权重可能从 JSON 反序列化成字符串
            return value == null ? null : Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        // 字符串布尔值来自 JSON map 场景
        return value == null ? null : Boolean.valueOf(value.toString());
    }

    private String normalizeText(String value) {
        // 空白字符串统一当作未配置
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeLiteral(String value, String defaultValue) {
        // 字面量统一小写，减少配置大小写差异
        return value == null ? defaultValue : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeColor(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        // 颜色名只允许安全字符，避免透传异常 Header
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return COLOR_NAME_PATTERN.matcher(normalized).matches() ? normalized : defaultValue;
    }

}
