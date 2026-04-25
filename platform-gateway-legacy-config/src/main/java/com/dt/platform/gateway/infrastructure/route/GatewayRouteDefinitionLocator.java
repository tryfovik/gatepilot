package com.dt.platform.gateway.infrastructure.route;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 负责把配置属性编译成标准化路由定义。
 */
public class GatewayRouteDefinitionLocator {

    private static final List<String> DEFAULT_RETRY_METHODS = List.of("GET");

    private static final List<String> DEFAULT_RETRY_SERIES = List.of("server-error");

    private static final List<String> DEFAULT_RETRY_EXCEPTIONS = List.of("io", "timeout");

    private final List<GatewayRouteDefinition> routeDefinitions;

    private final Map<String, GatewayRouteDefinition> apiRoutesByRoot;

    private final Map<String, GatewayRouteDefinition> internalRoutesByRoot;

    /**
     * 创建路由定义定位器。
     *
     * @param properties 网关配置
     */
    public GatewayRouteDefinitionLocator(GatewayProperties properties) {
        this.routeDefinitions = List.copyOf(buildRouteDefinitions(properties));
        this.apiRoutesByRoot = buildRouteIndex(routeDefinitions, true);
        this.internalRoutesByRoot = buildRouteIndex(routeDefinitions, false);
    }

    /**
     * 返回全部标准化路由定义。
     *
     * @return 路由定义列表
     */
    public List<GatewayRouteDefinition> getRouteDefinitions() {
        return routeDefinitions;
    }

    /**
     * 解析当前请求命中的 API 路由定义。
     *
     * @param requestPath 请求路径
     * @return 命中的路由定义
     */
    public Optional<GatewayRouteDefinition> findApiRoute(String requestPath) {
        return findRoute(apiRoutesByRoot, requestPath);
    }

    /**
     * 解析当前请求命中的内部运维路由定义。
     *
     * @param requestPath 请求路径
     * @return 命中的路由定义
     */
    public Optional<GatewayRouteDefinition> findInternalRoute(String requestPath) {
        return findRoute(internalRoutesByRoot, requestPath);
    }

    /**
     * 返回启用了运维入口的路由定义。
     *
     * @return 运维路由定义
     */
    public List<GatewayRouteDefinition> getActuatorRoutes() {
        return routeDefinitions.stream()
                .filter(definition -> !definition.getInternalPathRoots().isEmpty())
                .toList();
    }

    private List<GatewayRouteDefinition> buildRouteDefinitions(GatewayProperties properties) {
        if (properties.getProjects().isEmpty()) {
            throw new IllegalArgumentException("gateway projects must not be empty");
        }
        List<GatewayRouteDefinition> definitions = new ArrayList<>();
        Map<String, String> apiRoots = new LinkedHashMap<>();
        Map<String, String> internalRoots = new LinkedHashMap<>();
        boolean hasEnabledRoute = false;
        String apiPrefix = normalizePrefix(properties.getApiPrefix());
        String internalPrefix = normalizePrefix(properties.getInternalPrefix());
        for (Map.Entry<String, GatewayProperties.ProjectProperties> projectEntry : properties.getProjects().entrySet()) {
            GatewayProperties.ProjectProperties project = projectEntry.getValue();
            if (!project.isEnabled()) {
                continue;
            }
            String projectKey = projectEntry.getKey();
            String projectSegment = normalizeSegment(StringUtils.hasText(project.getPathSegment())
                    ? project.getPathSegment()
                    : projectKey);
            if (project.getRoutes().isEmpty()) {
                throw new IllegalArgumentException("gateway project routes must not be empty: " + projectKey);
            }
            for (Map.Entry<String, GatewayProperties.RouteProperties> routeEntry : project.getRoutes().entrySet()) {
                GatewayProperties.RouteProperties route = routeEntry.getValue();
                if (!route.isEnabled() || (!route.isApiEnabled() && !route.isActuatorEnabled())) {
                    continue;
                }
                hasEnabledRoute = true;
                String routeKey = routeEntry.getKey();
                String routeSegment = normalizeSegment(StringUtils.hasText(route.getPathSegment())
                        ? route.getPathSegment()
                        : routeKey);
                validateUpstream(projectKey, routeKey, route);

                List<String> apiPathRootsForRoute = new ArrayList<>();
                List<String> internalPathRootsForRoute = new ArrayList<>();
                String canonicalRouteSegment = projectSegment + "/" + routeSegment;
                if (route.isApiEnabled()) {
                    registerPathRoot(apiRoots, apiPathRootsForRoute, apiPrefix, canonicalRouteSegment, projectKey, routeKey);
                }
                if (route.isActuatorEnabled()) {
                    registerPathRoot(internalRoots, internalPathRootsForRoute, internalPrefix, canonicalRouteSegment, projectKey, routeKey);
                }
                definitions.add(new GatewayRouteDefinition(
                        projectKey,
                        projectSegment,
                        routeKey,
                        routeSegment,
                        apiPathRootsForRoute,
                        internalPathRootsForRoute,
                        route.getServiceUri(),
                        route.getApiMethods(),
                        route.getApiMaxRequestSize(),
                        route.getServicePathPrefix(),
                        route.getActuatorUri(),
                        route.getInternalMethods(),
                        route.getInternalMaxRequestSize(),
                        buildRetryPolicy(route),
                        buildCircuitBreakerPolicy(projectKey, routeKey, route),
                        sanitizeHeaderNames(route.getRelease().getWeightHashHeaders()),
                        buildReleaseVariants(route),
                        route.getAuth().isRequired(),
                        sanitizeRelativePaths(route.getAuth().getPublicPaths()),
                        route.getConnectTimeoutMs(),
                        route.getResponseTimeout()
                ));
            }
        }
        if (!hasEnabledRoute) {
            throw new IllegalArgumentException("at least one gateway route must be enabled");
        }
        return definitions;
    }

    private Map<String, GatewayRouteDefinition> buildRouteIndex(List<GatewayRouteDefinition> definitions,
                                                                boolean apiRoute) {
        Map<String, GatewayRouteDefinition> routeIndex = new LinkedHashMap<>();
        for (GatewayRouteDefinition definition : definitions) {
            List<String> pathRoots = apiRoute ? definition.getApiPathRoots() : definition.getInternalPathRoots();
            for (String pathRoot : pathRoots) {
                routeIndex.put(pathRoot, definition);
            }
        }
        return Map.copyOf(routeIndex);
    }

    private Optional<GatewayRouteDefinition> findRoute(Map<String, GatewayRouteDefinition> routeIndex,
                                                       String requestPath) {
        String candidate = normalizeRequestPath(requestPath);
        if (!StringUtils.hasText(candidate)) {
            return Optional.empty();
        }
        while (StringUtils.hasText(candidate)) {
            GatewayRouteDefinition definition = routeIndex.get(candidate);
            if (definition != null) {
                return Optional.of(definition);
            }
            int lastSlash = candidate.lastIndexOf('/');
            if (lastSlash <= 0) {
                return Optional.empty();
            }
            candidate = candidate.substring(0, lastSlash);
        }
        return Optional.empty();
    }

    private void validateUpstream(String projectKey,
                                  String routeKey,
                                  GatewayProperties.RouteProperties route) {
        if (route.isApiEnabled()) {
            requireUri(route.getServiceUri(),
                    "gateway route service uri must not be null when api route is enabled: " + projectKey + "/" + routeKey);
        }
        if (route.isActuatorEnabled()) {
            requireUri(route.getActuatorUri(),
                    "gateway route actuator uri must not be null when actuator route is enabled: " + projectKey + "/" + routeKey);
        }
        if (route.getConnectTimeoutMs() != null && route.getConnectTimeoutMs() <= 0) {
            throw new IllegalArgumentException("gateway route connect timeout must be positive: " + projectKey + "/" + routeKey);
        }
        if (route.getResponseTimeout() != null && route.getResponseTimeout().isNegative()) {
            throw new IllegalArgumentException("gateway route response timeout must not be negative: " + projectKey + "/" + routeKey);
        }
    }

    private void registerPathRoot(Map<String, String> registeredRoots,
                                  List<String> routeRoots,
                                  String prefix,
                                  String pathSegment,
                                  String projectKey,
                                  String routeKey) {
        String normalizedRoot = prefix + "/" + normalizeSegment(pathSegment);
        registerRoot(registeredRoots, routeRoots, normalizedRoot, projectKey, routeKey);
    }

    private void registerRoot(Map<String, String> registeredRoots,
                              List<String> routeRoots,
                              String rootPath,
                              String projectKey,
                              String routeKey) {
        String routeOwner = projectKey + "/" + routeKey;
        String previousOwner = registeredRoots.putIfAbsent(rootPath, routeOwner);
        if (previousOwner != null) {
            throw new IllegalArgumentException("gateway route path must be unique among enabled routes: "
                    + rootPath + ", duplicated by " + previousOwner + " and " + routeOwner);
        }
        routeRoots.add(rootPath);
    }

    private List<String> sanitizeRelativePaths(List<String> publicPaths) {
        if (publicPaths == null) {
            return List.of();
        }
        return publicPaths.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeRelativePath)
                .distinct()
                .toList();
    }

    private GatewayRouteDefinition.RetryPolicy buildRetryPolicy(GatewayProperties.RouteProperties route) {
        GatewayProperties.RetryProperties retry = route.getGovernance().getRetry();
        if (!retry.isEnabled()) {
            return null;
        }
        return new GatewayRouteDefinition.RetryPolicy(
                retry.getRetries(),
                sanitizeLiterals(retry.getMethods(), DEFAULT_RETRY_METHODS, true),
                sanitizeIntegerLiterals(retry.getStatuses()),
                sanitizeLiterals(retry.getSeries(), DEFAULT_RETRY_SERIES, false),
                sanitizeLiterals(retry.getExceptions(), DEFAULT_RETRY_EXCEPTIONS, false),
                buildRetryBackoff(retry.getBackoff())
        );
    }

    private GatewayRouteDefinition.RetryBackoff buildRetryBackoff(GatewayProperties.RetryBackoffProperties backoff) {
        if (backoff == null) {
            return null;
        }
        return new GatewayRouteDefinition.RetryBackoff(
                backoff.getFirstBackoff(),
                backoff.getMaxBackoff(),
                backoff.getFactor(),
                backoff.isBasedOnPreviousValue()
        );
    }

    private GatewayRouteDefinition.CircuitBreakerPolicy buildCircuitBreakerPolicy(String projectKey,
                                                                                  String routeKey,
                                                                                  GatewayProperties.RouteProperties route) {
        GatewayProperties.CircuitBreakerProperties circuitBreaker = route.getGovernance().getCircuitBreaker();
        if (!circuitBreaker.isEnabled()) {
            return null;
        }
        String circuitBreakerName = StringUtils.hasText(circuitBreaker.getName())
                ? circuitBreaker.getName().trim()
                : "platform-gateway-cb-" + sanitizeRouteIdLiteral(projectKey) + "-" + sanitizeRouteIdLiteral(routeKey);
        return new GatewayRouteDefinition.CircuitBreakerPolicy(
                circuitBreakerName,
                sanitizeIntegerLiterals(circuitBreaker.getStatusCodes()),
                circuitBreaker.getFallbackUri(),
                circuitBreaker.getSlidingWindowSize(),
                circuitBreaker.getMinimumNumberOfCalls(),
                circuitBreaker.getFailureRateThreshold(),
                circuitBreaker.getWaitDurationInOpenState(),
                circuitBreaker.getPermittedNumberOfCallsInHalfOpenState(),
                circuitBreaker.getSlowCallDurationThreshold(),
                circuitBreaker.getSlowCallRateThreshold(),
                circuitBreaker.getFallbackStatus(),
                circuitBreaker.getFallbackCode(),
                circuitBreaker.getFallbackMessage(),
                circuitBreaker.getContentType()
        );
    }

    private List<GatewayRouteDefinition.ReleaseVariant> buildReleaseVariants(GatewayProperties.RouteProperties route) {
        if (route.getRelease().getVariants().isEmpty()) {
            return List.of();
        }
        List<GatewayRouteDefinition.ReleaseVariant> variants = new ArrayList<>();
        for (Map.Entry<String, GatewayProperties.ReleaseVariantProperties> entry : route.getRelease().getVariants().entrySet()) {
            GatewayProperties.ReleaseVariantProperties variant = entry.getValue();
            if (variant == null || !variant.isEnabled()) {
                continue;
            }
            URI variantServiceUri = variant.getServiceUri() == null ? route.getServiceUri() : variant.getServiceUri();
            URI variantActuatorUri = variant.getActuatorUri() == null
                    ? (variant.getServiceUri() == null ? route.getActuatorUri() : variant.getServiceUri())
                    : variant.getActuatorUri();
            variants.add(new GatewayRouteDefinition.ReleaseVariant(
                    entry.getKey(),
                    sanitizeMatchColors(entry.getKey(), variant.getMatchColors()),
                    variant.getWeight(),
                    variantServiceUri,
                    StringUtils.hasText(variant.getServicePathPrefix()) ? variant.getServicePathPrefix() : route.getServicePathPrefix(),
                    variantActuatorUri,
                    variant.getConnectTimeoutMs() == null ? route.getConnectTimeoutMs() : variant.getConnectTimeoutMs(),
                    variant.getResponseTimeout() == null ? route.getResponseTimeout() : variant.getResponseTimeout()
            ));
        }
        return List.copyOf(variants);
    }

    private List<String> sanitizeLiterals(List<String> values,
                                          List<String> defaultValues,
                                          boolean uppercase) {
        if (values == null) {
            return defaultValues;
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(value -> uppercase ? value.toUpperCase(Locale.ROOT) : value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private List<String> sanitizeMatchColors(String variantKey, List<String> colors) {
        List<String> sanitized = sanitizeLiterals(colors, List.of(), false);
        if (sanitized.isEmpty()) {
            return List.of(variantKey.trim().toLowerCase(Locale.ROOT));
        }
        return sanitized;
    }

    private List<Integer> sanitizeIntegerLiterals(List<Integer> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    private String sanitizeRouteIdLiteral(String value) {
        if (!StringUtils.hasText(value)) {
            return "route";
        }
        return value.trim().replaceAll("[^a-zA-Z0-9-]", "-");
    }

    private List<String> sanitizeHeaderNames(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String normalizeSegment(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("gateway route segment must not be blank");
        }
        String normalized = value.trim();
        normalized = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        normalized = normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
        return normalized;
    }

    private String normalizeRelativePath(String value) {
        if (!StringUtils.hasText(value)) {
            return "/";
        }
        String normalized = value.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private String normalizeRequestPath(String requestPath) {
        if (!StringUtils.hasText(requestPath)) {
            return "";
        }
        String normalized = requestPath.trim();
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private void requireUri(URI value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
