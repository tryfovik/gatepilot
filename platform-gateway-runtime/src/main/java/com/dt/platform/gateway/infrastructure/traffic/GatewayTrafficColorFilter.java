package com.dt.platform.gateway.infrastructure.traffic;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 基于请求属性完成流量染色，并向下游透传流量颜色。
 */
public class GatewayTrafficColorFilter implements WebFilter {

    /**
     * 当前请求生效的流量颜色属性名。
     */
    public static final String TRAFFIC_COLOR_ATTRIBUTE =
            GatewayTrafficColorFilter.class.getName() + ".trafficColor";

    private static final Pattern COLOR_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}$");

    private final GatewayProperties.TrafficColorProperties trafficColorProperties;

    private final String defaultColor;

    private final List<CompiledRule> compiledRules;

    /**
     * 创建流量染色过滤器。
     *
     * @param trafficColorProperties 流量染色配置
     */
    public GatewayTrafficColorFilter(GatewayProperties.TrafficColorProperties trafficColorProperties) {
        this.trafficColorProperties = trafficColorProperties;
        this.defaultColor = normalizeColor(trafficColorProperties.getDefaultColor(), "stable");
        this.compiledRules = compileRules(trafficColorProperties.getRules());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!trafficColorProperties.isEnabled()) {
            return chain.filter(exchange);
        }
        String trafficColor = resolveTrafficColor(exchange);
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> headers.set(trafficColorProperties.getHeaderName(), trafficColor))
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
        mutatedExchange.getAttributes().put(TRAFFIC_COLOR_ATTRIBUTE, trafficColor);
        if (trafficColorProperties.isResponseHeaderEnabled()) {
            mutatedExchange.getResponse().beforeCommit(() -> {
                mutatedExchange.getResponse().getHeaders().set(trafficColorProperties.getHeaderName(), trafficColor);
                return Mono.empty();
            });
        }
        return chain.filter(mutatedExchange);
    }

    private String resolveTrafficColor(ServerWebExchange exchange) {
        if (trafficColorProperties.isTrustRequestHeader()) {
            String requestColor = normalizeColor(
                    exchange.getRequest().getHeaders().getFirst(trafficColorProperties.getHeaderName()),
                    null
            );
            if (requestColor != null) {
                return requestColor;
            }
        }
        for (CompiledRule rule : compiledRules) {
            String candidate = extractCandidate(exchange, rule.source(), rule.fieldName());
            if (candidate != null && rule.matches(candidate)) {
                return rule.color();
            }
        }
        return defaultColor;
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

    private String extractCandidate(ServerWebExchange exchange, String source, String fieldName) {
        return switch (source) {
            case "cookie" -> {
                HttpCookie cookie = exchange.getRequest().getCookies().getFirst(fieldName);
                yield cookie == null ? null : cookie.getValue();
            }
            case "query" -> exchange.getRequest().getQueryParams().getFirst(fieldName);
            default -> exchange.getRequest().getHeaders().getFirst(fieldName);
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
