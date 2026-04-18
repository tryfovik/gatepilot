package com.dt.platform.gateway.infrastructure.validation;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 网关配置启动校验器。
 */
public class GatewayPropertiesValidator {

    private static final Set<String> FLOW_CONTROL_BEHAVIORS = Set.of(
            "default",
            "warm-up",
            "rate-limiter",
            "warm-up-rate-limiter"
    );

    private static final Set<String> PARAM_PARSE_STRATEGIES = Set.of(
            "client-ip",
            "host",
            "header",
            "url-param",
            "cookie"
    );

    private static final Set<String> PARAM_MATCH_STRATEGIES = Set.of(
            "exact",
            "prefix",
            "regex",
            "contains"
    );

    private static final Set<String> TRAFFIC_COLOR_SOURCES = Set.of(
            "header",
            "cookie",
            "query"
    );

    private static final Pattern TRAFFIC_COLOR_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}$");

    private static final Set<String> SUPPORTED_HTTP_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name(),
            HttpMethod.TRACE.name()
    );

    private static final List<String> DEFAULT_RETRY_METHODS = List.of(HttpMethod.GET.name());

    private static final List<String> DEFAULT_RETRY_SERIES = List.of("server-error");

    private static final List<String> DEFAULT_RETRY_EXCEPTIONS = List.of("io", "timeout");

    private static final Set<String> SUPPORTED_RETRY_SERIES = Set.of(
            "informational",
            "successful",
            "redirection",
            "client-error",
            "server-error"
    );

    private static final Set<String> SUPPORTED_RETRY_EXCEPTIONS = Set.of(
            "io",
            "timeout"
    );

    /**
     * 创建网关配置校验器。
     *
     * @param properties 网关配置
     */
    public GatewayPropertiesValidator(GatewayProperties properties) {
        validatePrefixes(properties);
        validateCors(properties);
        validateContextHeaders(properties);
        validateTrafficColor(properties);
        validateRouteMethods(properties);
        validateRouteGovernance(properties);
    }

    private void validatePrefixes(GatewayProperties properties) {
        String apiPrefix = normalizePrefix(properties.getApiPrefix());
        String internalPrefix = normalizePrefix(properties.getInternalPrefix());
        if (StringUtils.hasText(apiPrefix) && apiPrefix.equals(internalPrefix)) {
            throw new IllegalArgumentException("gateway api prefix and internal prefix must not be identical");
        }
    }

    private void validateCors(GatewayProperties properties) {
        GatewayProperties.CorsProperties cors = properties.getCors();
        if (!cors.isEnabled()) {
            return;
        }
        if (isBlankCollection(cors.getAllowedOriginPatterns())) {
            throw new IllegalArgumentException("gateway cors allowed origin patterns must not be empty");
        }
        if (isBlankCollection(cors.getAllowedMethods())) {
            throw new IllegalArgumentException("gateway cors allowed methods must not be empty");
        }
        if (isBlankCollection(cors.getAllowedHeaders())) {
            throw new IllegalArgumentException("gateway cors allowed headers must not be empty");
        }
    }

    private void validateContextHeaders(GatewayProperties properties) {
        GatewayProperties.ContextHeadersProperties contextHeaders = properties.getContextHeaders();
        if (!contextHeaders.isEnabled()) {
            return;
        }
        requireText(contextHeaders.getProjectHeaderName(), "gateway project header name must not be blank");
        requireText(contextHeaders.getRouteHeaderName(), "gateway route header name must not be blank");
        if (contextHeaders.getProjectHeaderName().equalsIgnoreCase(contextHeaders.getRouteHeaderName())) {
            throw new IllegalArgumentException("gateway project header name and route header name must be different");
        }
    }

    private void validateTrafficColor(GatewayProperties properties) {
        GatewayProperties.TrafficColorProperties trafficColor = properties.getTrafficColor();
        if (!trafficColor.isEnabled()) {
            return;
        }
        requireText(trafficColor.getHeaderName(), "gateway traffic color header name must not be blank");
        requireTrafficColor(trafficColor.getDefaultColor(), "gateway traffic color default color is invalid");
        for (GatewayProperties.TrafficColorRuleProperties rule : trafficColor.getRules()) {
            if (rule == null || !rule.isEnabled()) {
                continue;
            }
            String source = normalizeLiteral(rule.getSource());
            if (!TRAFFIC_COLOR_SOURCES.contains(source)) {
                throw new IllegalArgumentException("gateway traffic color source is unsupported: " + rule.getSource());
            }
            requireText(rule.getFieldName(), "gateway traffic color field name must not be blank");
            requireText(rule.getPattern(), "gateway traffic color pattern must not be blank");
            String matchStrategy = normalizeLiteral(rule.getMatchStrategy());
            if (!PARAM_MATCH_STRATEGIES.contains(matchStrategy)) {
                throw new IllegalArgumentException("gateway traffic color match strategy is unsupported: " + rule.getMatchStrategy());
            }
            requireTrafficColor(rule.getColor(), "gateway traffic color rule target color is invalid");
        }
    }

    private void validateRouteGovernance(GatewayProperties properties) {
        for (var projectEntry : properties.getProjects().entrySet()) {
            String projectKey = projectEntry.getKey();
            GatewayProperties.ProjectProperties project = projectEntry.getValue();
            if (!project.isEnabled()) {
                continue;
            }
            for (var routeEntry : project.getRoutes().entrySet()) {
                String routeKey = routeEntry.getKey();
                GatewayProperties.RouteProperties route = routeEntry.getValue();
                if (!route.isEnabled()) {
                    continue;
                }
                validateRetryPolicy(projectKey, routeKey, route);
                validateFlowControl(projectKey, routeKey, route);
                validateReleaseVariants(projectKey, routeKey, route, properties.getTrafficColor());
            }
        }
    }

    private void validateRouteMethods(GatewayProperties properties) {
        for (var projectEntry : properties.getProjects().entrySet()) {
            String projectKey = projectEntry.getKey();
            GatewayProperties.ProjectProperties project = projectEntry.getValue();
            if (!project.isEnabled()) {
                continue;
            }
            for (var routeEntry : project.getRoutes().entrySet()) {
                String routeKey = routeEntry.getKey();
                GatewayProperties.RouteProperties route = routeEntry.getValue();
                if (!route.isEnabled()) {
                    continue;
                }
                validateMethods(projectKey, routeKey, "api", route.getApiMethods(), route.isApiEnabled());
                validateMethods(projectKey, routeKey, "internal", route.getInternalMethods(), route.isActuatorEnabled());
                validateRequestSize(projectKey, routeKey, "api", route.getApiMaxRequestSize(), route.isApiEnabled());
                validateRequestSize(projectKey, routeKey, "internal", route.getInternalMaxRequestSize(), route.isActuatorEnabled());
            }
        }
    }

    private void validateFlowControl(String projectKey,
                                     String routeKey,
                                     GatewayProperties.RouteProperties route) {
        GatewayProperties.FlowControlProperties flowControl = route.getGovernance().getFlowControl();
        if (!flowControl.isEnabled()) {
            return;
        }
        if (!route.isApiEnabled()) {
            throw new IllegalArgumentException("gateway flow control requires api route to be enabled: "
                    + projectKey + "/" + routeKey);
        }
        if (flowControl.getCount() == null || flowControl.getCount() <= 0) {
            throw new IllegalArgumentException("gateway flow control count must be positive: "
                    + projectKey + "/" + routeKey);
        }
        if (flowControl.getIntervalSec() <= 0) {
            throw new IllegalArgumentException("gateway flow control interval must be positive: "
                    + projectKey + "/" + routeKey);
        }
        if (flowControl.getBurst() < 0) {
            throw new IllegalArgumentException("gateway flow control burst must not be negative: "
                    + projectKey + "/" + routeKey);
        }
        if (flowControl.getMaxQueueingTimeoutMs() < 0) {
            throw new IllegalArgumentException("gateway flow control max queueing timeout must not be negative: "
                    + projectKey + "/" + routeKey);
        }
        String controlBehavior = normalizeLiteral(flowControl.getControlBehavior());
        if (!FLOW_CONTROL_BEHAVIORS.contains(controlBehavior)) {
            throw new IllegalArgumentException("gateway flow control behavior is unsupported: "
                    + projectKey + "/" + routeKey + " -> " + flowControl.getControlBehavior());
        }
        validateFlowParam(projectKey, routeKey, flowControl.getParam());
    }

    private void validateMethods(String projectKey,
                                 String routeKey,
                                 String routeType,
                                 List<String> methods,
                                 boolean routeEnabled) {
        if (methods == null || methods.isEmpty()) {
            return;
        }
        if (!routeEnabled) {
            throw new IllegalArgumentException("gateway " + routeType + " methods require route to be enabled: "
                    + projectKey + "/" + routeKey);
        }
        for (String method : methods) {
            String normalizedMethod = normalizeHttpMethod(method);
            if (!SUPPORTED_HTTP_METHODS.contains(normalizedMethod)) {
                throw new IllegalArgumentException("gateway " + routeType + " method is unsupported: "
                        + projectKey + "/" + routeKey + " -> " + method);
            }
        }
    }

    private void validateRequestSize(String projectKey,
                                     String routeKey,
                                     String routeType,
                                     DataSize maxRequestSize,
                                     boolean routeEnabled) {
        if (maxRequestSize == null) {
            return;
        }
        if (!routeEnabled) {
            throw new IllegalArgumentException("gateway " + routeType + " max request size requires route to be enabled: "
                    + projectKey + "/" + routeKey);
        }
        if (maxRequestSize.toBytes() <= 0) {
            throw new IllegalArgumentException("gateway " + routeType + " max request size must be positive: "
                    + projectKey + "/" + routeKey);
        }
    }

    private void validateRetryPolicy(String projectKey,
                                     String routeKey,
                                     GatewayProperties.RouteProperties route) {
        GatewayProperties.RetryProperties retry = route.getGovernance().getRetry();
        if (!retry.isEnabled()) {
            return;
        }
        if (!route.isApiEnabled()) {
            throw new IllegalArgumentException("gateway retry requires api route to be enabled: "
                    + projectKey + "/" + routeKey);
        }
        if (retry.getRetries() <= 0) {
            throw new IllegalArgumentException("gateway retry count must be positive: "
                    + projectKey + "/" + routeKey);
        }
        List<String> methods = retry.getMethods() == null
                ? DEFAULT_RETRY_METHODS
                : sanitizeStringLiterals(retry.getMethods(), true);
        if (methods.isEmpty()) {
            throw new IllegalArgumentException("gateway retry methods must not be empty: "
                    + projectKey + "/" + routeKey);
        }
        for (String method : methods) {
            if (!SUPPORTED_HTTP_METHODS.contains(method)) {
                throw new IllegalArgumentException("gateway retry method is unsupported: "
                        + projectKey + "/" + routeKey + " -> " + method);
            }
        }
        List<Integer> statuses = sanitizeStatusCodes(retry.getStatuses());
        for (Integer status : statuses) {
            if (HttpStatus.resolve(status) == null) {
                throw new IllegalArgumentException("gateway retry status is unsupported: "
                        + projectKey + "/" + routeKey + " -> " + status);
            }
        }
        List<String> series = retry.getSeries() == null
                ? DEFAULT_RETRY_SERIES
                : sanitizeStringLiterals(retry.getSeries(), false);
        for (String seriesLiteral : series) {
            if (!SUPPORTED_RETRY_SERIES.contains(seriesLiteral)) {
                throw new IllegalArgumentException("gateway retry status series is unsupported: "
                        + projectKey + "/" + routeKey + " -> " + seriesLiteral);
            }
        }
        List<String> exceptions = retry.getExceptions() == null
                ? DEFAULT_RETRY_EXCEPTIONS
                : sanitizeStringLiterals(retry.getExceptions(), false);
        for (String exceptionLiteral : exceptions) {
            if (!SUPPORTED_RETRY_EXCEPTIONS.contains(exceptionLiteral)) {
                throw new IllegalArgumentException("gateway retry exception is unsupported: "
                        + projectKey + "/" + routeKey + " -> " + exceptionLiteral);
            }
        }
        if (statuses.isEmpty() && series.isEmpty() && exceptions.isEmpty()) {
            throw new IllegalArgumentException("gateway retry series, statuses and exceptions must not all be empty: "
                    + projectKey + "/" + routeKey);
        }
        validateRetryBackoff(projectKey, routeKey, retry.getBackoff());
    }

    private void validateRetryBackoff(String projectKey,
                                      String routeKey,
                                      GatewayProperties.RetryBackoffProperties backoff) {
        if (backoff == null) {
            return;
        }
        if (backoff.getFirstBackoff() == null || backoff.getFirstBackoff().isZero() || backoff.getFirstBackoff().isNegative()) {
            throw new IllegalArgumentException("gateway retry first backoff must be positive: "
                    + projectKey + "/" + routeKey);
        }
        if (backoff.getMaxBackoff() != null
                && (backoff.getMaxBackoff().isZero() || backoff.getMaxBackoff().isNegative())) {
            throw new IllegalArgumentException("gateway retry max backoff must be positive: "
                    + projectKey + "/" + routeKey);
        }
        if (backoff.getMaxBackoff() != null && backoff.getMaxBackoff().compareTo(backoff.getFirstBackoff()) < 0) {
            throw new IllegalArgumentException("gateway retry max backoff must not be less than first backoff: "
                    + projectKey + "/" + routeKey);
        }
        if (backoff.getFactor() <= 0) {
            throw new IllegalArgumentException("gateway retry backoff factor must be positive: "
                    + projectKey + "/" + routeKey);
        }
    }

    private void validateFlowParam(String projectKey,
                                   String routeKey,
                                   GatewayProperties.FlowControlParamProperties param) {
        if (!param.isEnabled()) {
            return;
        }
        String parseStrategy = normalizeLiteral(param.getParseStrategy());
        if (!PARAM_PARSE_STRATEGIES.contains(parseStrategy)) {
            throw new IllegalArgumentException("gateway flow control param parse strategy is unsupported: "
                    + projectKey + "/" + routeKey + " -> " + param.getParseStrategy());
        }
        if (requiresFieldName(parseStrategy)) {
            requireText(param.getFieldName(), "gateway flow control param field name must not be blank: "
                    + projectKey + "/" + routeKey);
        }
        String matchStrategy = normalizeLiteral(param.getMatchStrategy());
        if (!PARAM_MATCH_STRATEGIES.contains(matchStrategy)) {
            throw new IllegalArgumentException("gateway flow control param match strategy is unsupported: "
                    + projectKey + "/" + routeKey + " -> " + param.getMatchStrategy());
        }
    }

    private void validateReleaseVariants(String projectKey,
                                         String routeKey,
                                         GatewayProperties.RouteProperties route,
                                         GatewayProperties.TrafficColorProperties trafficColor) {
        Map<String, GatewayProperties.ReleaseVariantProperties> variants = route.getRelease().getVariants();
        if (variants.isEmpty()) {
            return;
        }
        boolean hasEnabledVariant = variants.values().stream()
                .filter(java.util.Objects::nonNull)
                .anyMatch(GatewayProperties.ReleaseVariantProperties::isEnabled);
        if (!hasEnabledVariant) {
            return;
        }
        if (!route.isApiEnabled()) {
            throw new IllegalArgumentException("gateway release variants require api route to be enabled: "
                    + projectKey + "/" + routeKey);
        }
        if (!trafficColor.isEnabled()) {
            throw new IllegalArgumentException("gateway release variants require traffic color to be enabled: "
                    + projectKey + "/" + routeKey);
        }
        Set<String> registeredColors = new LinkedHashSet<>();
        for (Map.Entry<String, GatewayProperties.ReleaseVariantProperties> entry : variants.entrySet()) {
            GatewayProperties.ReleaseVariantProperties variant = entry.getValue();
            if (variant == null || !variant.isEnabled()) {
                continue;
            }
            String variantKey = normalizeTrafficColor(entry.getKey());
            if (variantKey == null) {
                throw new IllegalArgumentException("gateway release variant key is invalid: "
                        + projectKey + "/" + routeKey + " -> " + entry.getKey());
            }
            List<String> matchColors = sanitizeTrafficColors(variant.getMatchColors());
            if (matchColors.isEmpty()) {
                matchColors = List.of(variantKey);
            }
            for (String color : matchColors) {
                if (!registeredColors.add(color)) {
                    throw new IllegalArgumentException("gateway release variant traffic color must be unique per route: "
                            + projectKey + "/" + routeKey + " -> " + color);
                }
            }
            if (variant.getConnectTimeoutMs() != null && variant.getConnectTimeoutMs() <= 0) {
                throw new IllegalArgumentException("gateway release variant connect timeout must be positive: "
                        + projectKey + "/" + routeKey + " -> " + entry.getKey());
            }
            if (variant.getResponseTimeout() != null && variant.getResponseTimeout().isNegative()) {
                throw new IllegalArgumentException("gateway release variant response timeout must not be negative: "
                        + projectKey + "/" + routeKey + " -> " + entry.getKey());
            }
        }
    }

    private boolean isBlankCollection(Iterable<String> values) {
        if (values == null) {
            return true;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return false;
            }
        }
        return true;
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean requiresFieldName(String parseStrategy) {
        return "header".equals(parseStrategy)
                || "url-param".equals(parseStrategy)
                || "cookie".equals(parseStrategy);
    }

    private String normalizeLiteral(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String normalizeHttpMethod(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTrafficColor(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return TRAFFIC_COLOR_NAME_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    private List<String> sanitizeStringLiterals(List<String> values, boolean uppercase) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(value -> uppercase ? value.toUpperCase(Locale.ROOT) : value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private List<Integer> sanitizeStatusCodes(List<Integer> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<String> sanitizeTrafficColors(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(this::normalizeTrafficColor)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    private void requireTrafficColor(String color, String message) {
        if (normalizeTrafficColor(color) == null) {
            throw new IllegalArgumentException(message + ": " + color);
        }
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
