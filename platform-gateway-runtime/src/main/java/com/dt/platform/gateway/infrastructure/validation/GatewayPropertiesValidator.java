package com.dt.platform.gateway.infrastructure.validation;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    /**
     * 创建网关配置校验器。
     *
     * @param properties 网关配置
     */
    public GatewayPropertiesValidator(GatewayProperties properties) {
        validatePrefixes(properties);
        validateCors(properties);
        validateContextHeaders(properties);
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
                validateFlowControl(projectKey, routeKey, route);
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

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
