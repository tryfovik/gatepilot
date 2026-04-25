package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.TrafficColorSource;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
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

    private static final String DEFAULT_COLOR = "stable";

    private static final String DEFAULT_HEADER_NAME = "X-Traffic-Color";

    private static final List<String> DEFAULT_WEIGHT_HASH_HEADERS =
            List.of("X-User-Id", "X-Tenant-Id", "X-Trace-Id");

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
        String trustedHeaderColor = resolveTrustedHeaderColor(runtime, route, request);
        if (trustedHeaderColor != null) {
            return trustedHeaderColor;
        }
        String ruleColor = resolveRuleColor(runtime, route, request);
        if (ruleColor != null) {
            return ruleColor;
        }
        String weightedColor = resolveWeightedColor(runtime, route, request);
        if (weightedColor != null) {
            return weightedColor;
        }
        return defaultColor(runtime, route);
    }

    private String resolveTrustedHeaderColor(CompiledProxyRuntime runtime,
                                             CompiledRoute route,
                                             TrafficColorRequest request) {
        if (!trustRequestHeader(runtime, route)) {
            return null;
        }
        return normalizeColor(request.header(headerName(runtime, route)), null);
    }

    private String resolveRuleColor(CompiledProxyRuntime runtime,
                                    CompiledRoute route,
                                    TrafficColorRequest request) {
        for (CompiledPolicy policy : policiesByType(runtime, route, "TrafficPolicy")) {
            for (TrafficColorRule rule : trafficColorRules(policy)) {
                String candidate = extractCandidate(request, rule.source(), rule.fieldName());
                if (candidate != null && rule.matches(candidate)) {
                    return rule.color();
                }
            }
        }
        return null;
    }

    private String resolveWeightedColor(CompiledProxyRuntime runtime,
                                        CompiledRoute route,
                                        TrafficColorRequest request) {
        for (CompiledPolicy policy : policiesByType(runtime, route, "ReleasePolicy")) {
            List<TrafficSplit> splits = trafficSplits(policy);
            if (splits.isEmpty()) {
                continue;
            }
            int bucket = calculateBucket(buildWeightedHashKey(policy, route, request));
            int cumulativeWeight = 0;
            for (TrafficSplit split : splits) {
                cumulativeWeight = Math.min(100, cumulativeWeight + split.weight());
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

    private List<TrafficColorRule> trafficColorRules(CompiledPolicy policy) {
        Object rules = policy.getConfig().get("colorRules");
        if (!(rules instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<TrafficColorRule> parsedRules = new ArrayList<>();
        for (Object item : iterable) {
            TrafficColorRule rule = trafficColorRule(item);
            if (rule != null) {
                parsedRules.add(rule);
            }
        }
        return parsedRules;
    }

    private TrafficColorRule trafficColorRule(Object item) {
        if (item instanceof TrafficPolicy.TrafficColorRule rule) {
            return trafficColorRule(
                    sourceName(rule.getSource()),
                    rule.getKey(),
                    rule.getMatch(),
                    "exact",
                    rule.getColor()
            );
        }
        if (item instanceof Map<?, ?> map) {
            return trafficColorRule(
                    stringValue(map, "source"),
                    firstText(map, "key", "fieldName", "name"),
                    firstText(map, "match", "pattern", "value"),
                    Optional.ofNullable(stringValue(map, "matchStrategy")).orElse("exact"),
                    stringValue(map, "color")
            );
        }
        return null;
    }

    private TrafficColorRule trafficColorRule(String source,
                                              String fieldName,
                                              String pattern,
                                              String matchStrategy,
                                              String color) {
        String normalizedSource = normalizeLiteral(source, "header");
        String normalizedFieldName = normalizeText(fieldName);
        String normalizedPattern = normalizeText(pattern);
        String normalizedMatchStrategy = normalizeLiteral(matchStrategy, "exact");
        String normalizedColor = normalizeColor(color, null);
        if (normalizedFieldName == null || normalizedPattern == null || normalizedColor == null) {
            return null;
        }
        Pattern regexPattern = "regex".equals(normalizedMatchStrategy)
                ? Pattern.compile(normalizedPattern)
                : null;
        return new TrafficColorRule(
                normalizedSource,
                normalizedFieldName,
                normalizedPattern,
                normalizedMatchStrategy,
                normalizedColor,
                regexPattern
        );
    }

    private List<TrafficSplit> trafficSplits(CompiledPolicy policy) {
        Object splits = policy.getConfig().get("trafficSplits");
        if (!(splits instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<TrafficSplit> parsedSplits = new ArrayList<>();
        for (Object item : iterable) {
            TrafficSplit split = trafficSplit(item);
            if (split != null) {
                parsedSplits.add(split);
            }
        }
        return parsedSplits;
    }

    private TrafficSplit trafficSplit(Object item) {
        if (item instanceof ReleasePolicy.TrafficSplit split) {
            return trafficSplit(split.getTarget(), split.getColor(), split.getWeight());
        }
        if (item instanceof Map<?, ?> map) {
            return trafficSplit(stringValue(map, "target"), stringValue(map, "color"), integerValue(map, "weight"));
        }
        return null;
    }

    private TrafficSplit trafficSplit(String target, String color, Integer weight) {
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
        return new TrafficSplit(normalizedColor, Math.min(100, weight));
    }

    private String extractCandidate(TrafficColorRequest request, String source, String fieldName) {
        return switch (source) {
            case "cookie" -> request.cookie(fieldName);
            case "query" -> request.query(fieldName);
            default -> request.header(fieldName);
        };
    }

    private String buildWeightedHashKey(CompiledPolicy policy, CompiledRoute route, TrafficColorRequest request) {
        String identity = firstHeaderValue(request, weightHashHeaders(policy));
        if (identity == null) {
            identity = normalizeText(request.remoteAddress());
        }
        if (identity == null) {
            identity = normalizeText(request.rawQuery()) == null
                    ? request.path()
                    : request.path() + "?" + request.rawQuery();
        }
        return route.getRouteId() + "|" + identity;
    }

    private List<String> weightHashHeaders(CompiledPolicy policy) {
        Object headers = policy.getConfig().get("weightHashHeaders");
        if (!(headers instanceof Iterable<?> iterable)) {
            return DEFAULT_WEIGHT_HASH_HEADERS;
        }
        List<String> parsedHeaders = new ArrayList<>();
        for (Object item : iterable) {
            String header = normalizeText(Objects.toString(item, null));
            if (header != null) {
                parsedHeaders.add(header);
            }
        }
        return parsedHeaders.isEmpty() ? DEFAULT_WEIGHT_HASH_HEADERS : List.copyOf(parsedHeaders);
    }

    private String firstHeaderValue(TrafficColorRequest request, List<String> headerNames) {
        for (String headerName : headerNames) {
            String value = normalizeText(request.header(headerName));
            if (value != null) {
                return headerName + "=" + value;
            }
        }
        return null;
    }

    private int calculateBucket(String value) {
        CRC32 crc32 = new CRC32();
        crc32.update(Objects.toString(value, "").getBytes(StandardCharsets.UTF_8));
        return (int) (crc32.getValue() % 100);
    }

    private boolean trustRequestHeader(CompiledProxyRuntime runtime, CompiledRoute route) {
        return policiesByType(runtime, route, "TrafficPolicy").stream()
                .map(policy -> booleanValue(policy.getConfig().get("trustRequestHeader")))
                .anyMatch(Boolean.TRUE::equals);
    }

    private String headerName(CompiledProxyRuntime runtime, CompiledRoute route) {
        return policiesByType(runtime, route, "TrafficPolicy").stream()
                .map(policy -> normalizeText(stringValue(policy.getConfig(), "headerName")))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(DEFAULT_HEADER_NAME);
    }

    private String defaultColor(CompiledProxyRuntime runtime, CompiledRoute route) {
        return policiesByType(runtime, route, "TrafficPolicy").stream()
                .map(policy -> normalizeColor(stringValue(policy.getConfig(), "defaultColor"), null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(DEFAULT_COLOR);
    }

    private String sourceName(TrafficColorSource source) {
        return source == null ? null : source.name();
    }

    private String firstText(Map<?, ?> map, String... keys) {
        for (String key : keys) {
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
            return value == null ? null : Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value == null ? null : Boolean.valueOf(value.toString());
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeLiteral(String value, String defaultValue) {
        return value == null ? defaultValue : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeColor(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return COLOR_NAME_PATTERN.matcher(normalized).matches() ? normalized : defaultValue;
    }

    private record TrafficColorRule(String source,
                                    String fieldName,
                                    String pattern,
                                    String matchStrategy,
                                    String color,
                                    Pattern regexPattern) {

        private boolean matches(String candidate) {
            return switch (matchStrategy) {
                case "prefix" -> candidate.startsWith(pattern);
                case "contains" -> candidate.contains(pattern);
                case "regex" -> regexPattern != null && regexPattern.matcher(candidate).matches();
                default -> candidate.equals(pattern);
            };
        }
    }

    private record TrafficSplit(String color, int weight) {
    }
}
