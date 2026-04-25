package com.dt.platform.gateway.infrastructure.traffic;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinition;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * 统一解析请求流量颜色，供真实过滤链和诊断 API 复用。
 */
public class GatewayTrafficColorResolver {

    private static final Pattern COLOR_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}$");

    private final GatewayProperties.TrafficColorProperties trafficColorProperties;

    private final GatewayRouteDefinitionLocator routeDefinitionLocator;

    private final String defaultColor;

    private final List<CompiledRule> compiledRules;

    /**
     * 创建流量颜色解析器。
     *
     * @param trafficColorProperties 流量染色配置
     * @param routeDefinitionLocator 路由定义定位器
     */
    public GatewayTrafficColorResolver(GatewayProperties.TrafficColorProperties trafficColorProperties,
                                       GatewayRouteDefinitionLocator routeDefinitionLocator) {
        this.trafficColorProperties = trafficColorProperties;
        this.routeDefinitionLocator = routeDefinitionLocator;
        this.defaultColor = normalizeColor(trafficColorProperties.getDefaultColor(), "stable");
        this.compiledRules = compileRules(trafficColorProperties.getRules());
    }

    /**
     * 解析流量颜色。
     *
     * @param request 请求上下文
     * @return 流量颜色
     */
    public String resolve(TrafficColorRequest request) {
        if (!trafficColorProperties.isEnabled()) {
            return null;
        }
        if (trafficColorProperties.isTrustRequestHeader()) {
            String requestColor = normalizeColor(request.header(trafficColorProperties.getHeaderName()), null);
            if (requestColor != null) {
                return requestColor;
            }
        }
        for (CompiledRule rule : compiledRules) {
            String candidate = extractCandidate(request, rule.source(), rule.fieldName());
            if (candidate != null && rule.matches(candidate)) {
                return rule.color();
            }
        }
        String weightedTrafficColor = resolveWeightedTrafficColor(request);
        if (weightedTrafficColor != null) {
            return weightedTrafficColor;
        }
        return defaultColor;
    }

    private String resolveWeightedTrafficColor(TrafficColorRequest request) {
        if (routeDefinitionLocator == null) {
            return null;
        }
        Optional<GatewayRouteDefinition> routeDefinition = routeDefinitionLocator.findApiRoute(request.path());
        if (routeDefinition.isEmpty()) {
            return null;
        }
        List<GatewayRouteDefinition.ReleaseVariant> weightedVariants = routeDefinition.get().getReleaseVariants()
                .stream()
                .filter(variant -> variant.weight() > 0)
                .toList();
        if (weightedVariants.isEmpty()) {
            return null;
        }
        int bucket = calculateBucket(buildWeightedHashKey(request, routeDefinition.get()));
        int cumulativeWeight = 0;
        for (GatewayRouteDefinition.ReleaseVariant variant : weightedVariants) {
            cumulativeWeight = Math.min(100, cumulativeWeight + variant.weight());
            if (bucket < cumulativeWeight) {
                return variant.matchColors().isEmpty() ? variant.variantKey() : variant.matchColors().get(0);
            }
        }
        return null;
    }

    private String buildWeightedHashKey(TrafficColorRequest request, GatewayRouteDefinition definition) {
        String identity = firstHeaderValue(request, definition.getReleaseWeightHashHeaders());
        if (!StringUtils.hasText(identity)) {
            identity = request.remoteAddress();
        }
        if (!StringUtils.hasText(identity)) {
            identity = request.rawQuery() == null ? request.path() : request.path() + "?" + request.rawQuery();
        }
        return definition.getProjectKey() + "|" + definition.getRouteKey() + "|" + identity;
    }

    private String firstHeaderValue(TrafficColorRequest request, List<String> headerNames) {
        if (headerNames == null || headerNames.isEmpty()) {
            return null;
        }
        for (String headerName : headerNames) {
            String value = request.header(headerName);
            if (StringUtils.hasText(value)) {
                return headerName + "=" + value.trim();
            }
        }
        return null;
    }

    private int calculateBucket(String value) {
        CRC32 crc32 = new CRC32();
        crc32.update(value.getBytes(StandardCharsets.UTF_8));
        return (int) (crc32.getValue() % 100);
    }

    private List<CompiledRule> compileRules(List<GatewayProperties.TrafficColorRuleProperties> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        List<CompiledRule> compiled = new ArrayList<>();
        for (GatewayProperties.TrafficColorRuleProperties rule : rules) {
            if (rule == null || !rule.isEnabled()) {
                continue;
            }
            String source = normalizeLiteral(rule.getSource(), "header");
            String fieldName = normalizeText(rule.getFieldName());
            String pattern = normalizeText(rule.getPattern());
            String matchStrategy = normalizeLiteral(rule.getMatchStrategy(), "exact");
            String color = normalizeColor(rule.getColor(), null);
            if (fieldName == null || pattern == null || color == null) {
                continue;
            }
            compiled.add(new CompiledRule(source, fieldName, pattern, matchStrategy, color,
                    "regex".equals(matchStrategy) ? Pattern.compile(pattern) : null));
        }
        return List.copyOf(compiled);
    }

    private String extractCandidate(TrafficColorRequest request, String source, String fieldName) {
        return switch (source) {
            case "cookie" -> request.cookie(fieldName);
            case "query" -> request.query(fieldName);
            default -> request.header(fieldName);
        };
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeLiteral(String value, String defaultValue) {
        return value == null ? defaultValue : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeColor(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return COLOR_NAME_PATTERN.matcher(normalized).matches() ? normalized : defaultValue;
    }

    /**
     * 流量颜色解析所需的请求信息。
     *
     * @param path 请求路径
     * @param rawQuery 原始 query
     * @param headerReader 请求头读取器
     * @param cookieReader Cookie 读取器
     * @param queryReader Query 读取器
     * @param remoteAddress 远端地址
     */
    public record TrafficColorRequest(String path,
                                      String rawQuery,
                                      Function<String, String> headerReader,
                                      Function<String, String> cookieReader,
                                      Function<String, String> queryReader,
                                      String remoteAddress) {

        private String header(String name) {
            return headerReader == null ? null : headerReader.apply(name);
        }

        private String cookie(String name) {
            return cookieReader == null ? null : cookieReader.apply(name);
        }

        private String query(String name) {
            return queryReader == null ? null : queryReader.apply(name);
        }
    }

    private record CompiledRule(String source,
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
}
