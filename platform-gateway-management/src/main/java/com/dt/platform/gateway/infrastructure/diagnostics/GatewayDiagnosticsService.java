package com.dt.platform.gateway.infrastructure.diagnostics;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinition;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import com.dt.platform.gateway.infrastructure.traffic.GatewayTrafficColorResolver;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 网关路由诊断服务。
 */
public class GatewayDiagnosticsService {

    private final GatewayProperties properties;

    private final GatewayRouteDefinitionLocator routeDefinitionLocator;

    private final GatewayTrafficColorResolver trafficColorResolver;

    /**
     * 创建诊断服务。
     *
     * @param properties 网关配置
     * @param routeDefinitionLocator 路由定义定位器
     */
    public GatewayDiagnosticsService(GatewayProperties properties,
                                     GatewayRouteDefinitionLocator routeDefinitionLocator) {
        this.properties = properties;
        this.routeDefinitionLocator = routeDefinitionLocator;
        this.trafficColorResolver = new GatewayTrafficColorResolver(
                properties.getTrafficColor(),
                routeDefinitionLocator
        );
    }

    /**
     * 诊断一次模拟请求。
     *
     * @param request 诊断请求
     * @return 诊断结果
     */
    public GatewayDiagnosticsView diagnose(GatewayDiagnosticsRequest request) {
        NormalizedRequest normalizedRequest = normalizeRequest(request);
        Optional<GatewayRouteDefinition> apiRoute = routeDefinitionLocator.findApiRoute(normalizedRequest.path());
        if (apiRoute.isPresent()) {
            return diagnoseApiRoute(normalizedRequest, apiRoute.get());
        }
        Optional<GatewayRouteDefinition> internalRoute = routeDefinitionLocator.findInternalRoute(normalizedRequest.path());
        if (internalRoute.isPresent()) {
            return diagnoseInternalRoute(normalizedRequest, internalRoute.get());
        }
        return new GatewayDiagnosticsView(
                false,
                "none",
                normalizedRequest.method(),
                normalizedRequest.path(),
                null,
                null,
                null,
                null,
                null,
                List.of("No gateway route matched the request path")
        );
    }

    private GatewayDiagnosticsView diagnoseApiRoute(NormalizedRequest request, GatewayRouteDefinition definition) {
        String trafficColor = trafficColorResolver.resolve(new GatewayTrafficColorResolver.TrafficColorRequest(
                request.path(),
                request.rawQuery(),
                headerName -> firstValue(request.headers(), headerName, true),
                cookieName -> firstValue(request.cookies(), cookieName, false),
                queryName -> firstValue(request.query(), queryName, false),
                request.remoteAddress()
        ));
        GatewayRouteDefinition.ReleaseVariant selectedVariant = findReleaseVariant(definition, trafficColor);
        List<String> warnings = new ArrayList<>();
        boolean methodAllowed = definition.isApiMethodAllowed(request.method());
        boolean publicPath = definition.isPublicApiPath(request.path());
        if (!methodAllowed) {
            warnings.add("HTTP method is not allowed for the matched API route");
        }
        if (definition.isAuthRequired() && !publicPath) {
            warnings.add("Request requires authentication for the matched API route");
        }
        return new GatewayDiagnosticsView(
                true,
                "api",
                request.method(),
                request.path(),
                buildRouteMatch(definition, definition.findMatchedApiRoot(request.path()).orElse(null),
                        definition.resolveRelativeApiPath(request.path()).orElse(null)),
                new AccessDecisionView(
                        methodAllowed,
                        definition.getApiMethods(),
                        definition.isAuthRequired(),
                        publicPath,
                        definition.isAuthRequired() && !publicPath,
                        false
                ),
                new TrafficDecisionView(
                        properties.getTrafficColor().isEnabled(),
                        properties.getTrafficColor().getHeaderName(),
                        trafficColor,
                        selectedVariant == null ? null : buildReleaseVariantView(selectedVariant)
                ),
                buildUpstreamView(definition, selectedVariant),
                buildGovernanceView(definition, routeProperties(definition)),
                List.copyOf(warnings)
        );
    }

    private GatewayDiagnosticsView diagnoseInternalRoute(NormalizedRequest request, GatewayRouteDefinition definition) {
        boolean methodAllowed = definition.isInternalMethodAllowed(request.method());
        List<String> warnings = new ArrayList<>();
        if (!methodAllowed) {
            warnings.add("HTTP method is not allowed for the matched internal route");
        }
        warnings.add("Internal route is protected by internal access policy");
        return new GatewayDiagnosticsView(
                true,
                "internal",
                request.method(),
                request.path(),
                buildRouteMatch(definition, definition.findMatchedInternalRoot(request.path()).orElse(null), null),
                new AccessDecisionView(
                        methodAllowed,
                        definition.getInternalMethods(),
                        false,
                        false,
                        false,
                        true
                ),
                null,
                new UpstreamDecisionView(
                        null,
                        definition.getActuatorUri(),
                        null,
                        definition.getConnectTimeoutMs(),
                        toMillis(definition.getResponseTimeout())
                ),
                buildGovernanceView(definition, routeProperties(definition)),
                List.copyOf(warnings)
        );
    }

    private RouteMatchView buildRouteMatch(GatewayRouteDefinition definition, String pathRoot, String relativePath) {
        return new RouteMatchView(
                definition.getProjectKey(),
                definition.getRouteKey(),
                definition.getProjectSegment(),
                definition.getRouteSegment(),
                pathRoot,
                relativePath,
                definition.buildGovernanceApiName()
        );
    }

    private UpstreamDecisionView buildUpstreamView(GatewayRouteDefinition definition,
                                                   GatewayRouteDefinition.ReleaseVariant selectedVariant) {
        if (selectedVariant != null) {
            return new UpstreamDecisionView(
                    selectedVariant.variantKey(),
                    selectedVariant.serviceUri(),
                    selectedVariant.servicePathPrefix(),
                    selectedVariant.connectTimeoutMs(),
                    toMillis(selectedVariant.responseTimeout())
            );
        }
        return new UpstreamDecisionView(
                null,
                definition.getServiceUri(),
                definition.getServicePathPrefix(),
                definition.getConnectTimeoutMs(),
                toMillis(definition.getResponseTimeout())
        );
    }

    private GatewayRouteDefinition.ReleaseVariant findReleaseVariant(GatewayRouteDefinition definition,
                                                                     String trafficColor) {
        if (!StringUtils.hasText(trafficColor)) {
            return null;
        }
        return definition.getReleaseVariants().stream()
                .filter(variant -> variant.matchColors().contains(trafficColor.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    private GovernanceDecisionView buildGovernanceView(GatewayRouteDefinition definition,
                                                       GatewayProperties.RouteProperties routeProperties) {
        return new GovernanceDecisionView(
                buildRetryView(definition.getRetryPolicy()),
                buildFlowControlView(definition, routeProperties),
                buildCircuitBreakerView(definition.getCircuitBreakerPolicy())
        );
    }

    private RetryView buildRetryView(GatewayRouteDefinition.RetryPolicy retryPolicy) {
        if (retryPolicy == null) {
            return new RetryView(false, null, List.of(), List.of(), List.of(), List.of(), null);
        }
        GatewayRouteDefinition.RetryBackoff backoff = retryPolicy.backoff();
        return new RetryView(
                true,
                retryPolicy.retries(),
                retryPolicy.methods(),
                retryPolicy.statuses(),
                retryPolicy.series(),
                retryPolicy.exceptions(),
                backoff == null
                        ? null
                        : new RetryBackoffView(
                        toMillis(backoff.firstBackoff()),
                        toMillis(backoff.maxBackoff()),
                        backoff.factor(),
                        backoff.basedOnPreviousValue()
                )
        );
    }

    private FlowControlView buildFlowControlView(GatewayRouteDefinition definition,
                                                 GatewayProperties.RouteProperties routeProperties) {
        GatewayProperties.FlowControlProperties flowControl = routeProperties == null
                ? new GatewayProperties.FlowControlProperties()
                : routeProperties.getGovernance().getFlowControl();
        GatewayProperties.FlowControlParamProperties param = flowControl.getParam();
        return new FlowControlView(
                flowControl.isEnabled(),
                definition.buildGovernanceApiName(),
                flowControl.getCount(),
                flowControl.getIntervalSec(),
                flowControl.getBurst(),
                flowControl.getControlBehavior(),
                flowControl.getMaxQueueingTimeoutMs(),
                new ParamFlowView(
                        param.isEnabled(),
                        param.getParseStrategy(),
                        param.getFieldName(),
                        param.getPattern(),
                        param.getMatchStrategy()
                )
        );
    }

    private CircuitBreakerView buildCircuitBreakerView(GatewayRouteDefinition.CircuitBreakerPolicy policy) {
        if (policy == null) {
            return new CircuitBreakerView(false, null, List.of(), null, null, null, null, null, null, null, null, null, null, null);
        }
        return new CircuitBreakerView(
                true,
                policy.name(),
                policy.statusCodes(),
                policy.fallbackUri() == null ? null : policy.fallbackUri().toString(),
                policy.slidingWindowSize(),
                policy.minimumNumberOfCalls(),
                policy.failureRateThreshold(),
                toMillis(policy.waitDurationInOpenState()),
                policy.permittedNumberOfCallsInHalfOpenState(),
                toMillis(policy.slowCallDurationThreshold()),
                policy.slowCallRateThreshold(),
                policy.fallbackStatus(),
                policy.fallbackCode(),
                policy.fallbackMessage()
        );
    }

    private ReleaseVariantView buildReleaseVariantView(GatewayRouteDefinition.ReleaseVariant variant) {
        return new ReleaseVariantView(
                variant.variantKey(),
                variant.matchColors(),
                variant.weight(),
                variant.serviceUri(),
                variant.servicePathPrefix(),
                variant.connectTimeoutMs(),
                toMillis(variant.responseTimeout())
        );
    }

    private GatewayProperties.RouteProperties routeProperties(GatewayRouteDefinition definition) {
        GatewayProperties.ProjectProperties project = properties.getProjects().get(definition.getProjectKey());
        return project == null ? null : project.getRoutes().get(definition.getRouteKey());
    }

    private NormalizedRequest normalizeRequest(GatewayDiagnosticsRequest request) {
        String method = StringUtils.hasText(request == null ? null : request.method())
                ? request.method().trim().toUpperCase(Locale.ROOT)
                : "GET";
        ParsedPath parsedPath = parsePath(request == null ? null : request.path());
        Map<String, List<String>> query = new LinkedHashMap<>(parsedPath.query());
        mergeValues(query, request == null ? null : request.query());
        return new NormalizedRequest(
                method,
                parsedPath.path(),
                parsedPath.rawQuery(),
                sanitizeMultiValueMap(request == null ? null : request.headers()),
                query,
                sanitizeMultiValueMap(request == null ? null : request.cookies()),
                request == null ? null : request.remoteAddress()
        );
    }

    private ParsedPath parsePath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return new ParsedPath("/", null, Map.of());
        }
        String candidate = rawPath.trim();
        try {
            URI uri = URI.create(candidate);
            if (StringUtils.hasText(uri.getScheme())) {
                return new ParsedPath(
                        normalizePath(uri.getRawPath()),
                        uri.getRawQuery(),
                        parseQuery(uri.getRawQuery())
                );
            }
        }
        catch (IllegalArgumentException ignored) {
            // Fall back to plain path parsing below.
        }
        int queryIndex = candidate.indexOf('?');
        String path = queryIndex < 0 ? candidate : candidate.substring(0, queryIndex);
        String rawQuery = queryIndex < 0 ? null : candidate.substring(queryIndex + 1);
        return new ParsedPath(normalizePath(path), rawQuery, parseQuery(rawQuery));
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private Map<String, List<String>> parseQuery(String rawQuery) {
        if (!StringUtils.hasText(rawQuery)) {
            return Map.of();
        }
        return new LinkedHashMap<>(UriComponentsBuilder.fromUriString("/?" + rawQuery)
                .build(true)
                .getQueryParams());
    }

    private Map<String, List<String>> sanitizeMultiValueMap(Map<String, List<String>> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            if (!StringUtils.hasText(entry.getKey())) {
                continue;
            }
            List<String> entryValues = entry.getValue() == null
                    ? List.of()
                    : entry.getValue().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .toList();
            if (!entryValues.isEmpty()) {
                sanitized.put(entry.getKey().trim(), entryValues);
            }
        }
        return sanitized;
    }

    private void mergeValues(Map<String, List<String>> target, Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : sanitizeMultiValueMap(source).entrySet()) {
            target.merge(entry.getKey(), entry.getValue(), (left, right) -> {
                List<String> merged = new ArrayList<>(left);
                merged.addAll(right);
                return List.copyOf(merged);
            });
        }
    }

    private String firstValue(Map<String, List<String>> values, String name, boolean ignoreCase) {
        if (values == null || values.isEmpty() || !StringUtils.hasText(name)) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            boolean matched = ignoreCase ? entry.getKey().equalsIgnoreCase(name) : entry.getKey().equals(name);
            if (matched && entry.getValue() != null && !entry.getValue().isEmpty()) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }

    private Long toMillis(Duration duration) {
        return duration == null ? null : duration.toMillis();
    }

    private record NormalizedRequest(String method,
                                     String path,
                                     String rawQuery,
                                     Map<String, List<String>> headers,
                                     Map<String, List<String>> query,
                                     Map<String, List<String>> cookies,
                                     String remoteAddress) {
    }

    private record ParsedPath(String path,
                              String rawQuery,
                              Map<String, List<String>> query) {
    }

    /**
     * 诊断请求。
     *
     * @param method HTTP 方法
     * @param path 请求路径，可包含 query
     * @param headers 请求头
     * @param query Query 参数
     * @param cookies Cookie
     * @param remoteAddress 远端地址
     */
    public record GatewayDiagnosticsRequest(String method,
                                            String path,
                                            Map<String, List<String>> headers,
                                            Map<String, List<String>> query,
                                            Map<String, List<String>> cookies,
                                            String remoteAddress) {
    }

    /**
     * 诊断结果。
     */
    public record GatewayDiagnosticsView(boolean matched,
                                         String routeType,
                                         String method,
                                         String path,
                                         RouteMatchView route,
                                         AccessDecisionView access,
                                         TrafficDecisionView traffic,
                                         UpstreamDecisionView upstream,
                                         GovernanceDecisionView governance,
                                         List<String> warnings) {
    }

    public record RouteMatchView(String projectKey,
                                 String routeKey,
                                 String projectSegment,
                                 String routeSegment,
                                 String pathRoot,
                                 String relativePath,
                                 String governanceResourceName) {
    }

    public record AccessDecisionView(boolean methodAllowed,
                                     List<String> allowedMethods,
                                     boolean authRequiredByRoute,
                                     boolean publicPath,
                                     boolean authenticationRequired,
                                     boolean internalAccessProtected) {
    }

    public record TrafficDecisionView(boolean enabled,
                                      String headerName,
                                      String color,
                                      ReleaseVariantView selectedReleaseVariant) {
    }

    public record ReleaseVariantView(String variantKey,
                                     List<String> matchColors,
                                     int weight,
                                     URI serviceUri,
                                     String servicePathPrefix,
                                     Integer connectTimeoutMs,
                                     Long responseTimeoutMs) {
    }

    public record UpstreamDecisionView(String releaseVariant,
                                       URI uri,
                                       String pathPrefix,
                                       Integer connectTimeoutMs,
                                       Long responseTimeoutMs) {
    }

    public record GovernanceDecisionView(RetryView retry,
                                         FlowControlView flowControl,
                                         CircuitBreakerView circuitBreaker) {
    }

    public record RetryView(boolean enabled,
                            Integer retries,
                            List<String> methods,
                            List<Integer> statuses,
                            List<String> series,
                            List<String> exceptions,
                            RetryBackoffView backoff) {
    }

    public record RetryBackoffView(Long firstBackoffMs,
                                   Long maxBackoffMs,
                                   int factor,
                                   boolean basedOnPreviousValue) {
    }

    public record FlowControlView(boolean enabled,
                                  String resourceName,
                                  Double count,
                                  long intervalSec,
                                  int burst,
                                  String controlBehavior,
                                  int maxQueueingTimeoutMs,
                                  ParamFlowView param) {
    }

    public record ParamFlowView(boolean enabled,
                                String parseStrategy,
                                String fieldName,
                                String pattern,
                                String matchStrategy) {
    }

    public record CircuitBreakerView(boolean enabled,
                                     String name,
                                     List<Integer> statusCodes,
                                     String fallbackUri,
                                     Integer slidingWindowSize,
                                     Integer minimumNumberOfCalls,
                                     Double failureRateThreshold,
                                     Long waitDurationInOpenStateMs,
                                     Integer permittedNumberOfCallsInHalfOpenState,
                                     Long slowCallDurationThresholdMs,
                                     Double slowCallRateThreshold,
                                     Integer fallbackStatus,
                                     Integer fallbackCode,
                                     String fallbackMessage) {
    }
}
