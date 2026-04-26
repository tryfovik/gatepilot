package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.apiserver.application.dto.RouteCatalogResponse;
import com.dt.gatepilot.apiserver.application.dto.RouteDiagnosticsRequest;
import com.dt.gatepilot.apiserver.application.dto.RouteDiagnosticsResponse;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourcePaths;
import com.dt.gatepilot.domain.enums.AuthType;
import com.dt.gatepilot.domain.enums.HttpMethod;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.publish.PublishedConfigConstants;
import com.getboot.exception.api.code.CommonErrorCode;
import com.getboot.exception.api.exception.BusinessException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.CRC32;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * GatePilot 诊断查询服务。
 */
@Service
public class GatePilotDiagnosticsService {

    private final GatePilotResourceService resourceService;

    /**
     * 创建诊断查询服务。
     *
     * @param resourceService 资源服务
     */
    public GatePilotDiagnosticsService(GatePilotResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 查询路由目录。
     *
     * @param namespace 命名空间
     * @param projectName 项目名称
     * @param version 发布版本
     * @param configShard 配置分片
     * @return 路由目录
     */
    public RouteCatalogResponse catalog(String namespace, String projectName, String version, String configShard) {
        PublishedConfig config = selectPublishedConfig(namespace, projectName, version, configShard);
        RouteCatalogResponse result = new RouteCatalogResponse();
        fillConfigSummary(result, config);
        Map<String, PublishedConfig.PublishedUpstream> upstreams = upstreamIndex(config);
        for (PublishedConfig.PublishedRoute route : config.getSpec().getRoutes()) {
            result.getRoutes().add(routeView(route, upstreams));
        }
        for (PublishedConfig.PublishedUpstream upstream : config.getSpec().getUpstreams()) {
            result.getUpstreams().add(upstreamView(upstream));
        }
        for (PublishedConfig.PublishedPolicy policy : config.getSpec().getPolicies()) {
            result.getPolicies().add(policyView(policy));
        }
        for (PublishedConfig.NodeApplyResult nodeApplyResult : config.getStatus().getNodeApplyResults()) {
            result.getNodeApplyResults().add(nodeApplyView(nodeApplyResult));
        }
        result.setRouteCount(result.getRoutes().size());
        result.setUpstreamCount(result.getUpstreams().size());
        result.setPolicyCount(result.getPolicies().size());
        result.setTargetNodeCount(config.getSpec().getTargetNodeRefs().size());
        return result;
    }

    /**
     * 诊断一次模拟请求。
     *
     * @param request 诊断请求
     * @return 诊断结果
     */
    public RouteDiagnosticsResponse diagnose(RouteDiagnosticsRequest request) {
        NormalizedRequest normalizedRequest = normalizeRequest(request);
        PublishedConfig config = selectPublishedConfig(
                normalizedRequest.namespace(),
                request == null ? null : request.getProjectName(),
                request == null ? null : request.getVersion(),
                request == null ? null : request.getConfigShard()
        );
        RouteDiagnosticsResponse result = new RouteDiagnosticsResponse();
        fillConfigSummary(result, config);
        result.setMethod(normalizedRequest.method());
        result.setHost(normalizedRequest.host());
        result.setPath(normalizedRequest.path());
        Optional<PublishedConfig.PublishedRoute> matchedRoute = matchRoute(config, normalizedRequest);
        if (matchedRoute.isEmpty()) {
            // 没命中时仍返回版本信息，方便确认诊断基于哪份配置
            result.setMatched(false);
            result.getWarnings().add(GatePilotDiagnosticsConstants.WARNING_ROUTE_NOT_MATCHED);
            return result;
        }
        PublishedConfig.PublishedRoute route = matchedRoute.get();
        Map<String, PublishedConfig.PublishedPolicy> policies = policyIndex(config);
        Map<String, PublishedConfig.PublishedUpstream> upstreams = upstreamIndex(config);
        TrafficDecision trafficDecision = resolveTrafficColor(route, policies, normalizedRequest);
        String upstreamName = resolveUpstreamName(route, policies, trafficDecision.color());
        PublishedConfig.PublishedUpstream upstream = upstreams.get(upstreamName);
        result.setMatched(true);
        result.setRoute(diagnosticsRouteView(route));
        result.setAccess(accessView(route, policies, normalizedRequest, result.getWarnings()));
        result.setTraffic(trafficView(trafficDecision, route, policies));
        result.setUpstream(diagnosticsUpstreamView(upstreamName, upstream, result.getWarnings()));
        result.setGovernance(governanceView(route, policies));
        return result;
    }

    private void fillConfigSummary(RouteCatalogResponse result, PublishedConfig config) {
        PublishedConfig.PublishedConfigSpec spec = config.getSpec();
        ResourceReference projectRef = spec.getProjectRef();
        // 页面顶部只展示发布配置的稳定摘要
        result.setNamespace(config.getMetadata().getNamespace());
        result.setProjectName(projectRef == null ? null : projectRef.getName());
        result.setVersion(spec.getVersion());
        result.setConfigShard(spec.getConfigShard());
        result.setConfigHash(spec.getConfigHash());
        result.setGeneratedAt(spec.getGeneratedAt());
    }

    private void fillConfigSummary(RouteDiagnosticsResponse result, PublishedConfig config) {
        PublishedConfig.PublishedConfigSpec spec = config.getSpec();
        ResourceReference projectRef = spec.getProjectRef();
        // 诊断结果保留配置摘要，方便和发布记录对齐
        result.setNamespace(config.getMetadata().getNamespace());
        result.setProjectName(projectRef == null ? null : projectRef.getName());
        result.setVersion(spec.getVersion());
        result.setConfigShard(spec.getConfigShard());
        result.setConfigHash(spec.getConfigHash());
        result.setGeneratedAt(spec.getGeneratedAt());
    }

    private RouteCatalogResponse.RouteView routeView(PublishedConfig.PublishedRoute route,
                                                   Map<String, PublishedConfig.PublishedUpstream> upstreams) {
        RouteCatalogResponse.RouteView view = new RouteCatalogResponse.RouteView();
        // 路由目录以 PublishedConfig 为准，不反查草稿资源
        view.setRouteId(route.getRouteId());
        view.setName(resourceName(route.getSourceRef(), route.getRouteId()));
        view.setHosts(safeList(route.getHosts()));
        view.setPath(route.getPath());
        view.setMethods(safeList(route.getMethods()).stream().map(Enum::name).toList());
        view.setUpstreamName(route.getUpstreamName());
        view.setUpstreamAvailable(upstreams.containsKey(route.getUpstreamName()));
        view.setPolicyNames(safeList(route.getPolicyNames()));
        view.setStripPrefix(route.getStripPrefix());
        view.setRewritePathPrefix(route.getRewritePathPrefix());
        return view;
    }

    private RouteCatalogResponse.UpstreamView upstreamView(PublishedConfig.PublishedUpstream upstream) {
        RouteCatalogResponse.UpstreamView view = new RouteCatalogResponse.UpstreamView();
        // 上游目录展示端点和健康检查配置，不读取 proxy 本地健康表
        view.setName(upstream.getName());
        view.setProtocol(upstream.getProtocol() == null ? null : upstream.getProtocol().name());
        view.setLoadBalance(upstream.getLoadBalance());
        view.setEndpointCount(upstream.getEndpoints().size());
        view.setHealthCheckEnabled(upstream.getHealthCheck() == null ? null : upstream.getHealthCheck().getEnabled());
        view.setEndpoints(upstream.getEndpoints().stream().map(this::endpointView).toList());
        return view;
    }

    private RouteCatalogResponse.EndpointView endpointView(PublishedConfig.PublishedEndpoint endpoint) {
        RouteCatalogResponse.EndpointView view = new RouteCatalogResponse.EndpointView();
        view.setHost(endpoint.getHost());
        view.setPort(endpoint.getPort());
        view.setWeight(endpoint.getWeight());
        return view;
    }

    private RouteCatalogResponse.PolicyView policyView(PublishedConfig.PublishedPolicy policy) {
        RouteCatalogResponse.PolicyView view = new RouteCatalogResponse.PolicyView();
        view.setName(policy.getName());
        view.setType(policy.getType());
        return view;
    }

    private RouteCatalogResponse.NodeApplyView nodeApplyView(PublishedConfig.NodeApplyResult nodeApplyResult) {
        RouteCatalogResponse.NodeApplyView view = new RouteCatalogResponse.NodeApplyView();
        view.setNodeId(nodeApplyResult.getNodeId());
        view.setZone(nodeApplyResult.getZone());
        view.setState(nodeApplyResult.getState() == null ? null : nodeApplyResult.getState().name());
        view.setAppliedVersion(nodeApplyResult.getAppliedVersion());
        view.setReason(nodeApplyResult.getReason());
        view.setMessage(nodeApplyResult.getMessage());
        return view;
    }

    private RouteDiagnosticsResponse.RouteView diagnosticsRouteView(PublishedConfig.PublishedRoute route) {
        RouteDiagnosticsResponse.RouteView view = new RouteDiagnosticsResponse.RouteView();
        view.setRouteId(route.getRouteId());
        view.setName(resourceName(route.getSourceRef(), route.getRouteId()));
        view.setHosts(safeList(route.getHosts()));
        view.setPath(route.getPath());
        view.setUpstreamName(route.getUpstreamName());
        view.setPolicyNames(safeList(route.getPolicyNames()));
        return view;
    }

    private RouteDiagnosticsResponse.AccessView accessView(PublishedConfig.PublishedRoute route,
                                                         Map<String, PublishedConfig.PublishedPolicy> policies,
                                                         NormalizedRequest request,
                                                         List<String> warnings) {
        RouteDiagnosticsResponse.AccessView view = new RouteDiagnosticsResponse.AccessView();
        List<String> allowedMethods = allowedMethods(route);
        boolean methodAllowed = methodAllowed(route, request.method());
        AuthDecision authDecision = authDecision(route, policies);
        view.setAllowedMethods(allowedMethods);
        view.setMethodAllowed(methodAllowed);
        view.setAnonymousAllowed(authDecision.anonymousAllowed());
        view.setAuthenticationRequired(methodAllowed && authDecision.required());
        view.setAuthPolicyNames(authDecision.policyNames());
        if (!methodAllowed) {
            warnings.add(GatePilotDiagnosticsConstants.WARNING_METHOD_NOT_ALLOWED);
        }
        if (view.isAuthenticationRequired()) {
            warnings.add(GatePilotDiagnosticsConstants.WARNING_AUTH_REQUIRED);
        }
        return view;
    }

    private RouteDiagnosticsResponse.TrafficView trafficView(TrafficDecision trafficDecision,
                                                           PublishedConfig.PublishedRoute route,
                                                           Map<String, PublishedConfig.PublishedPolicy> policies) {
        RouteDiagnosticsResponse.TrafficView view = new RouteDiagnosticsResponse.TrafficView();
        view.setColor(trafficDecision.color());
        view.setSource(trafficDecision.source());
        view.setHeaderName(colorHeaderName(route, policies));
        view.setReleaseTarget(releaseTarget(route, policies, trafficDecision.color()));
        return view;
    }

    private RouteDiagnosticsResponse.UpstreamView diagnosticsUpstreamView(String upstreamName,
                                                                        PublishedConfig.PublishedUpstream upstream,
                                                                        List<String> warnings) {
        RouteDiagnosticsResponse.UpstreamView view = new RouteDiagnosticsResponse.UpstreamView();
        view.setName(upstreamName);
        if (upstream == null) {
            // 上游缺失是配置发布后的排障重点
            view.setAvailable(false);
            warnings.add(GatePilotDiagnosticsConstants.WARNING_UPSTREAM_MISSING);
            return view;
        }
        view.setAvailable(true);
        view.setProtocol(upstream.getProtocol() == null ? null : upstream.getProtocol().name());
        view.setLoadBalance(upstream.getLoadBalance());
        view.setEndpointCount(upstream.getEndpoints().size());
        view.setHealthCheckEnabled(upstream.getHealthCheck() == null ? null : upstream.getHealthCheck().getEnabled());
        if (upstream.getEndpoints().isEmpty()) {
            warnings.add(GatePilotDiagnosticsConstants.WARNING_UPSTREAM_ENDPOINT_EMPTY);
        }
        return view;
    }

    private RouteDiagnosticsResponse.GovernanceView governanceView(PublishedConfig.PublishedRoute route,
                                                                 Map<String, PublishedConfig.PublishedPolicy> policies) {
        RouteDiagnosticsResponse.GovernanceView view = new RouteDiagnosticsResponse.GovernanceView();
        for (PublishedConfig.PublishedPolicy policy : policiesByType(route, policies,
                PublishedConfigConstants.POLICY_TYPE_TRAFFIC)) {
            // 多个流量策略时诊断展示第一份显式配置
            applyRetry(view.getRetry(), policy.getConfig().get(PublishedConfigConstants.KEY_RETRY));
            applyRateLimit(view.getRateLimit(), policy.getConfig().get(PublishedConfigConstants.KEY_RATE_LIMIT));
            applyCircuitBreaker(view.getCircuitBreaker(),
                    policy.getConfig().get(PublishedConfigConstants.KEY_CIRCUIT_BREAKER));
        }
        return view;
    }

    private void applyRetry(RouteDiagnosticsResponse.RetryView view, Object value) {
        if (value instanceof TrafficPolicy.RetryPolicy retry) {
            view.setEnabled(Boolean.TRUE.equals(retry.getEnabled()));
            view.setMaxAttempts(retry.getMaxAttempts());
            view.setStatuses(safeList(retry.getStatuses()));
            return;
        }
        if (value instanceof Map<?, ?> map) {
            view.setEnabled(Boolean.TRUE.equals(booleanValue(map.get(PublishedConfigConstants.KEY_ENABLED))));
            view.setMaxAttempts(integerValue(map.get(PublishedConfigConstants.KEY_MAX_ATTEMPTS)));
            view.setStatuses(integerList(map.get(PublishedConfigConstants.KEY_STATUSES)));
        }
    }

    private void applyRateLimit(RouteDiagnosticsResponse.RateLimitView view, Object value) {
        if (value instanceof TrafficPolicy.RateLimitPolicy rateLimit) {
            view.setEnabled(Boolean.TRUE.equals(rateLimit.getEnabled()));
            view.setRequestsPerSecond(rateLimit.getRequestsPerSecond());
            view.setBurstCapacity(rateLimit.getBurstCapacity());
            view.setParamRuleCount(rateLimit.getParamRules().size());
            return;
        }
        if (value instanceof Map<?, ?> map) {
            view.setEnabled(Boolean.TRUE.equals(booleanValue(map.get(PublishedConfigConstants.KEY_ENABLED))));
            view.setRequestsPerSecond(integerValue(map.get(PublishedConfigConstants.KEY_REQUESTS_PER_SECOND)));
            view.setBurstCapacity(integerValue(map.get(PublishedConfigConstants.KEY_BURST_CAPACITY)));
            view.setParamRuleCount(iterableSize(map.get(PublishedConfigConstants.KEY_PARAM_RULES)));
        }
    }

    private void applyCircuitBreaker(RouteDiagnosticsResponse.CircuitBreakerView view, Object value) {
        if (value instanceof TrafficPolicy.CircuitBreakerPolicy circuitBreaker) {
            view.setEnabled(Boolean.TRUE.equals(circuitBreaker.getEnabled()));
            view.setSlidingWindowSize(circuitBreaker.getSlidingWindowSize());
            view.setFailureRateThreshold(circuitBreaker.getFailureRateThreshold());
            view.setFallbackStatus(circuitBreaker.getFallbackStatus());
            view.setFallbackMessage(circuitBreaker.getFallbackMessage());
            return;
        }
        if (value instanceof Map<?, ?> map) {
            view.setEnabled(Boolean.TRUE.equals(booleanValue(map.get(PublishedConfigConstants.KEY_ENABLED))));
            view.setSlidingWindowSize(integerValue(map.get(PublishedConfigConstants.KEY_SLIDING_WINDOW_SIZE)));
            view.setFailureRateThreshold(integerValue(map.get(PublishedConfigConstants.KEY_FAILURE_RATE_THRESHOLD)));
            view.setFallbackStatus(integerValue(map.get(PublishedConfigConstants.KEY_FALLBACK_STATUS)));
            view.setFallbackMessage(stringValue(map, PublishedConfigConstants.KEY_FALLBACK_MESSAGE));
        }
    }

    private AuthDecision authDecision(PublishedConfig.PublishedRoute route,
                                      Map<String, PublishedConfig.PublishedPolicy> policies) {
        boolean anonymousAllowed = false;
        boolean required = false;
        List<String> policyNames = new ArrayList<>();
        for (PublishedConfig.PublishedPolicy policy : policiesByType(route, policies,
                PublishedConfigConstants.POLICY_TYPE_AUTH)) {
            policyNames.add(policy.getName());
            if (Boolean.TRUE.equals(booleanValue(policy.getConfig().get(
                    PublishedConfigConstants.KEY_ANONYMOUS_ALLOWED)))) {
                anonymousAllowed = true;
                continue;
            }
            String authType = Objects.toString(policy.getConfig().get(PublishedConfigConstants.KEY_TYPE), "");
            if (!AuthType.NONE.name().equalsIgnoreCase(authType)) {
                required = true;
            }
        }
        return new AuthDecision(required, anonymousAllowed, List.copyOf(policyNames));
    }

    private TrafficDecision resolveTrafficColor(PublishedConfig.PublishedRoute route,
                                                Map<String, PublishedConfig.PublishedPolicy> policies,
                                                NormalizedRequest request) {
        String headerColor = resolveTrustedHeaderColor(route, policies, request);
        if (headerColor != null) {
            return new TrafficDecision(headerColor, GatePilotDiagnosticsConstants.COLOR_SOURCE_HEADER);
        }
        String ruleColor = resolveRuleColor(route, policies, request);
        if (ruleColor != null) {
            return new TrafficDecision(ruleColor, GatePilotDiagnosticsConstants.COLOR_SOURCE_RULE);
        }
        String weightedColor = resolveWeightedColor(route, policies, request);
        if (weightedColor != null) {
            return new TrafficDecision(weightedColor, GatePilotDiagnosticsConstants.COLOR_SOURCE_WEIGHT);
        }
        return new TrafficDecision(defaultColor(route, policies), GatePilotDiagnosticsConstants.COLOR_SOURCE_DEFAULT);
    }

    private String resolveTrustedHeaderColor(PublishedConfig.PublishedRoute route,
                                             Map<String, PublishedConfig.PublishedPolicy> policies,
                                             NormalizedRequest request) {
        if (!trustRequestHeader(route, policies)) {
            return null;
        }
        return normalizeColor(firstValue(request.headers(), colorHeaderName(route, policies), true), null);
    }

    private String resolveRuleColor(PublishedConfig.PublishedRoute route,
                                    Map<String, PublishedConfig.PublishedPolicy> policies,
                                    NormalizedRequest request) {
        for (PublishedConfig.PublishedPolicy policy : policiesByType(route, policies,
                PublishedConfigConstants.POLICY_TYPE_TRAFFIC)) {
            for (TrafficColorRuleView rule : trafficColorRules(policy)) {
                String candidate = colorCandidate(rule.source(), rule.fieldName(), request);
                if (candidate != null && rule.matches(candidate)) {
                    return rule.color();
                }
            }
        }
        return null;
    }

    private String resolveWeightedColor(PublishedConfig.PublishedRoute route,
                                        Map<String, PublishedConfig.PublishedPolicy> policies,
                                        NormalizedRequest request) {
        for (PublishedConfig.PublishedPolicy policy : policiesByType(route, policies,
                PublishedConfigConstants.POLICY_TYPE_RELEASE)) {
            List<TrafficSplitView> splits = trafficSplits(policy);
            if (splits.isEmpty()) {
                continue;
            }
            int bucket = calculateBucket(buildWeightedHashKey(policy, route, request));
            int cumulativeWeight = 0;
            for (TrafficSplitView split : splits) {
                cumulativeWeight = Math.min(GatePilotDiagnosticsConstants.WEIGHT_BUCKET_SIZE,
                        cumulativeWeight + Optional.ofNullable(split.weight()).orElse(0));
                if (bucket < cumulativeWeight) {
                    return normalizeColor(firstText(split.color(), split.target()), null);
                }
            }
        }
        return null;
    }

    private String resolveUpstreamName(PublishedConfig.PublishedRoute route,
                                       Map<String, PublishedConfig.PublishedPolicy> policies,
                                       String trafficColor) {
        String normalizedColor = normalizeLiteral(trafficColor, null);
        if (normalizedColor == null) {
            return route.getUpstreamName();
        }
        for (PublishedConfig.PublishedPolicy policy : policiesByType(route, policies,
                PublishedConfigConstants.POLICY_TYPE_RELEASE)) {
            for (TrafficSplitView split : trafficSplits(policy)) {
                if (split.matches(normalizedColor) && StringUtils.hasText(split.upstreamName())) {
                    return split.upstreamName();
                }
            }
        }
        return route.getUpstreamName();
    }

    private String releaseTarget(PublishedConfig.PublishedRoute route,
                                 Map<String, PublishedConfig.PublishedPolicy> policies,
                                 String trafficColor) {
        String normalizedColor = normalizeLiteral(trafficColor, null);
        if (normalizedColor == null) {
            return null;
        }
        for (PublishedConfig.PublishedPolicy policy : policiesByType(route, policies,
                PublishedConfigConstants.POLICY_TYPE_RELEASE)) {
            for (TrafficSplitView split : trafficSplits(policy)) {
                if (split.matches(normalizedColor)) {
                    return firstText(split.target(), split.color());
                }
            }
        }
        return null;
    }

    private List<TrafficColorRuleView> trafficColorRules(PublishedConfig.PublishedPolicy policy) {
        Object rules = policy.getConfig().get(PublishedConfigConstants.KEY_COLOR_RULES);
        if (!(rules instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<TrafficColorRuleView> parsedRules = new ArrayList<>();
        for (Object item : iterable) {
            TrafficColorRuleView rule = trafficColorRule(item);
            if (rule != null) {
                parsedRules.add(rule);
            }
        }
        return parsedRules;
    }

    private TrafficColorRuleView trafficColorRule(Object value) {
        if (value instanceof TrafficPolicy.TrafficColorRule rule) {
            return trafficColorRule(
                    enumName(rule.getSource()),
                    rule.getKey(),
                    rule.getMatch(),
                    GatePilotDiagnosticsConstants.MATCH_EXACT,
                    rule.getColor()
            );
        }
        if (value instanceof Map<?, ?> map) {
            return trafficColorRule(
                    stringValue(map, PublishedConfigConstants.KEY_SOURCE),
                    firstText(
                            stringValue(map, PublishedConfigConstants.KEY_KEY),
                            stringValue(map, PublishedConfigConstants.KEY_FIELD_NAME),
                            stringValue(map, PublishedConfigConstants.KEY_NAME)
                    ),
                    firstText(
                            stringValue(map, PublishedConfigConstants.KEY_MATCH),
                            stringValue(map, PublishedConfigConstants.KEY_PATTERN),
                            stringValue(map, PublishedConfigConstants.KEY_VALUE)
                    ),
                    Optional.ofNullable(stringValue(map, PublishedConfigConstants.KEY_MATCH_STRATEGY))
                            .orElse(GatePilotDiagnosticsConstants.MATCH_EXACT),
                    stringValue(map, PublishedConfigConstants.KEY_COLOR)
            );
        }
        return null;
    }

    private TrafficColorRuleView trafficColorRule(String source,
                                                  String fieldName,
                                                  String pattern,
                                                  String matchStrategy,
                                                  String color) {
        String normalizedSource = normalizeLiteral(source, GatePilotDiagnosticsConstants.SOURCE_HEADER);
        String normalizedFieldName = normalizeText(fieldName);
        String normalizedPattern = normalizeText(pattern);
        String normalizedMatchStrategy = normalizeLiteral(matchStrategy, GatePilotDiagnosticsConstants.MATCH_EXACT);
        String normalizedColor = normalizeColor(color, null);
        if (ipSource(normalizedSource) && normalizedFieldName == null) {
            normalizedFieldName = GatePilotDiagnosticsConstants.FIELD_REMOTE_ADDRESS;
        }
        if (normalizedFieldName == null || normalizedPattern == null || normalizedColor == null) {
            return null;
        }
        Pattern regex = compileRegex(normalizedMatchStrategy, normalizedPattern);
        return new TrafficColorRuleView(
                normalizedSource,
                normalizedFieldName,
                normalizedPattern,
                normalizedMatchStrategy,
                normalizedColor,
                regex
        );
    }

    private Pattern compileRegex(String matchStrategy, String pattern) {
        if (!GatePilotDiagnosticsConstants.MATCH_REGEX.equals(matchStrategy)) {
            return null;
        }
        try {
            // 正则非法时跳过该规则，避免诊断接口整体失败
            return Pattern.compile(pattern);
        } catch (PatternSyntaxException exception) {
            return null;
        }
    }

    private List<TrafficSplitView> trafficSplits(PublishedConfig.PublishedPolicy policy) {
        Object splits = policy.getConfig().get(PublishedConfigConstants.KEY_TRAFFIC_SPLITS);
        if (!(splits instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<TrafficSplitView> parsedSplits = new ArrayList<>();
        for (Object item : iterable) {
            TrafficSplitView split = trafficSplit(item);
            if (split != null) {
                parsedSplits.add(split);
            }
        }
        return parsedSplits;
    }

    private TrafficSplitView trafficSplit(Object value) {
        if (value instanceof ReleasePolicy.TrafficSplit split) {
            return trafficSplit(
                    split.getTarget(),
                    split.getColor(),
                    split.getWeight(),
                    upstreamName(split.getUpstreamRef())
            );
        }
        if (value instanceof Map<?, ?> map) {
            return trafficSplit(
                    stringValue(map, PublishedConfigConstants.KEY_TARGET),
                    stringValue(map, PublishedConfigConstants.KEY_COLOR),
                    integerValue(map.get(PublishedConfigConstants.KEY_WEIGHT)),
                    upstreamName(map.get(PublishedConfigConstants.KEY_UPSTREAM_REF))
            );
        }
        return null;
    }

    private TrafficSplitView trafficSplit(String target, String color, Integer weight, String upstreamName) {
        String normalizedTarget = normalizeLiteral(target, null);
        String normalizedColor = normalizeLiteral(color, null);
        if (normalizedTarget == null && normalizedColor == null) {
            return null;
        }
        return new TrafficSplitView(normalizedTarget, normalizedColor, weight, upstreamName);
    }

    private PublishedConfig selectPublishedConfig(String namespace,
                                                  String projectName,
                                                  String version,
                                                  String configShard) {
        List<PublishedConfig> configs = publishedConfigs(effectiveNamespace(namespace));
        return configs.stream()
                .filter(config -> projectMatches(config, projectName))
                .filter(config -> versionMatches(config, version))
                .filter(config -> configShardMatches(config, configShard))
                .max(Comparator.comparing(this::sequence)
                        .thenComparing(this::generatedAt)
                        .thenComparing(this::updatedAt)
                        .thenComparing(config -> Objects.toString(config.getSpec().getVersion(), "")))
                .orElseThrow(() -> BusinessException.of(CommonErrorCode.NOT_FOUND.code(),
                        GatePilotDiagnosticsConstants.MESSAGE_PUBLISHED_CONFIG_NOT_FOUND));
    }

    private List<PublishedConfig> publishedConfigs(String namespace) {
        List<PublishedConfig> configs = new ArrayList<>();
        String cursor = null;
        do {
            CursorPage<Object> page = resourceService.list(
                    GatePilotResourcePaths.PUBLISHED_CONFIGS,
                    namespace,
                    cursor,
                    GatePilotDiagnosticsConstants.PAGE_LIMIT
            );
            for (Object item : page.getItems()) {
                if (item instanceof PublishedConfig config) {
                    configs.add(config);
                }
            }
            cursor = page.getNextCursor();
        } while (StringUtils.hasText(cursor) && configs.size() < GatePilotDiagnosticsConstants.MAX_SCAN_ITEMS);
        return configs;
    }

    private Optional<PublishedConfig.PublishedRoute> matchRoute(PublishedConfig config, NormalizedRequest request) {
        return config.getSpec().getRoutes().stream()
                .filter(route -> hostMatches(route, request.host()))
                .filter(route -> pathMatches(route, request.path()))
                .max(Comparator.comparingInt(route -> normalizeRoutePath(route.getPath()).length()));
    }

    private boolean hostMatches(PublishedConfig.PublishedRoute route, String host) {
        List<String> hosts = route.getHosts();
        if (hosts == null || hosts.isEmpty()) {
            return true;
        }
        String normalizedHost = normalizeHost(host);
        if (normalizedHost == null) {
            return false;
        }
        return hosts.stream().map(this::normalizeHost).anyMatch(normalizedHost::equals);
    }

    private boolean pathMatches(PublishedConfig.PublishedRoute route, String requestPath) {
        String routePath = normalizeRoutePath(route.getPath());
        String path = normalizeRoutePath(requestPath);
        if (GatePilotDiagnosticsConstants.ROOT_PATH.equals(routePath)) {
            return true;
        }
        return path.equals(routePath) || path.startsWith(routePath + GatePilotDiagnosticsConstants.PATH_SEPARATOR);
    }

    private NormalizedRequest normalizeRequest(RouteDiagnosticsRequest request) {
        String method = StringUtils.hasText(request == null ? null : request.getMethod())
                ? request.getMethod().trim().toUpperCase(Locale.ROOT)
                : GatePilotDiagnosticsConstants.DEFAULT_METHOD;
        ParsedPath parsedPath = parsePath(request == null ? null : request.getPath());
        Map<String, List<String>> query = new LinkedHashMap<>(parsedPath.query());
        mergeValues(query, request == null ? null : request.getQuery());
        Map<String, List<String>> headers = sanitizeMultiValueMap(request == null ? null : request.getHeaders());
        String host = firstText(request == null ? null : request.getHost(),
                parsedPath.host(),
                firstValue(headers, GatePilotDiagnosticsConstants.HOST_HEADER, true));
        return new NormalizedRequest(
                effectiveNamespace(request == null ? null : request.getNamespace()),
                method,
                normalizeHost(host),
                parsedPath.path(),
                parsedPath.rawQuery(),
                headers,
                query,
                sanitizeMultiValueMap(request == null ? null : request.getCookies()),
                request == null ? null : request.getRemoteAddress()
        );
    }

    private ParsedPath parsePath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return new ParsedPath(GatePilotDiagnosticsConstants.ROOT_PATH, null, null, Map.of());
        }
        String candidate = rawPath.trim();
        try {
            URI uri = URI.create(candidate);
            if (StringUtils.hasText(uri.getScheme())) {
                return new ParsedPath(
                        normalizeRoutePath(uri.getRawPath()),
                        uri.getRawQuery(),
                        uri.getHost(),
                        parseQuery(uri.getRawQuery())
                );
            }
        } catch (IllegalArgumentException exception) {
            // 非 URI 按普通 path 继续解析
        }
        int queryIndex = candidate.indexOf(GatePilotDiagnosticsConstants.QUERY_SEPARATOR);
        String path = queryIndex < 0 ? candidate : candidate.substring(0, queryIndex);
        String rawQuery = queryIndex < 0 ? null : candidate.substring(queryIndex + 1);
        return new ParsedPath(normalizeRoutePath(path), rawQuery, null, parseQuery(rawQuery));
    }

    private Map<String, List<String>> parseQuery(String rawQuery) {
        if (!StringUtils.hasText(rawQuery)) {
            return Map.of();
        }
        return new LinkedHashMap<>(UriComponentsBuilder.fromUriString(
                GatePilotDiagnosticsConstants.ROOT_PATH + GatePilotDiagnosticsConstants.QUERY_SEPARATOR + rawQuery)
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

    private Map<String, PublishedConfig.PublishedUpstream> upstreamIndex(PublishedConfig config) {
        Map<String, PublishedConfig.PublishedUpstream> upstreams = new LinkedHashMap<>();
        for (PublishedConfig.PublishedUpstream upstream : config.getSpec().getUpstreams()) {
            upstreams.put(upstream.getName(), upstream);
        }
        return upstreams;
    }

    private Map<String, PublishedConfig.PublishedPolicy> policyIndex(PublishedConfig config) {
        Map<String, PublishedConfig.PublishedPolicy> policies = new LinkedHashMap<>();
        for (PublishedConfig.PublishedPolicy policy : config.getSpec().getPolicies()) {
            policies.put(policy.getName(), policy);
        }
        return policies;
    }

    private List<PublishedConfig.PublishedPolicy> policiesByType(PublishedConfig.PublishedRoute route,
                                                                 Map<String, PublishedConfig.PublishedPolicy> policies,
                                                                 String type) {
        List<PublishedConfig.PublishedPolicy> result = new ArrayList<>();
        for (String policyName : safeList(route.getPolicyNames())) {
            PublishedConfig.PublishedPolicy policy = policies.get(policyName);
            if (policy != null && type.equalsIgnoreCase(Objects.toString(policy.getType(), ""))) {
                result.add(policy);
            }
        }
        return result;
    }

    private boolean methodAllowed(PublishedConfig.PublishedRoute route, String method) {
        List<HttpMethod> methods = safeList(route.getMethods());
        if (methods.isEmpty()) {
            return true;
        }
        if (methods.stream().anyMatch(value -> HttpMethod.ANY == value)) {
            return true;
        }
        return methods.stream().map(Enum::name).anyMatch(method::equalsIgnoreCase);
    }

    private List<String> allowedMethods(PublishedConfig.PublishedRoute route) {
        List<HttpMethod> methods = safeList(route.getMethods());
        if (methods.stream().anyMatch(value -> HttpMethod.ANY == value)) {
            return List.of();
        }
        return methods.stream().map(Enum::name).toList();
    }

    private boolean trustRequestHeader(PublishedConfig.PublishedRoute route,
                                       Map<String, PublishedConfig.PublishedPolicy> policies) {
        return policiesByType(route, policies, PublishedConfigConstants.POLICY_TYPE_TRAFFIC).stream()
                .map(policy -> booleanValue(policy.getConfig().get(PublishedConfigConstants.KEY_TRUST_REQUEST_HEADER)))
                .anyMatch(Boolean.TRUE::equals);
    }

    private String colorHeaderName(PublishedConfig.PublishedRoute route,
                                   Map<String, PublishedConfig.PublishedPolicy> policies) {
        return policiesByType(route, policies, PublishedConfigConstants.POLICY_TYPE_TRAFFIC).stream()
                .map(policy -> normalizeText(stringValue(policy.getConfig(), PublishedConfigConstants.KEY_HEADER_NAME)))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(GatePilotDiagnosticsConstants.DEFAULT_COLOR_HEADER);
    }

    private String defaultColor(PublishedConfig.PublishedRoute route,
                                Map<String, PublishedConfig.PublishedPolicy> policies) {
        return policiesByType(route, policies, PublishedConfigConstants.POLICY_TYPE_TRAFFIC).stream()
                .map(policy -> normalizeColor(stringValue(policy.getConfig(),
                        PublishedConfigConstants.KEY_DEFAULT_COLOR), null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(GatePilotDiagnosticsConstants.DEFAULT_COLOR);
    }

    private String colorCandidate(String source, String fieldName, NormalizedRequest request) {
        if (ipSource(source)) {
            return request.remoteAddress();
        }
        return switch (source) {
            case GatePilotDiagnosticsConstants.SOURCE_COOKIE -> firstValue(request.cookies(), fieldName, false);
            case GatePilotDiagnosticsConstants.SOURCE_QUERY -> firstValue(request.query(), fieldName, false);
            default -> firstValue(request.headers(), fieldName, true);
        };
    }

    private boolean ipSource(String source) {
        return GatePilotDiagnosticsConstants.SOURCE_IP.equals(source)
                || GatePilotDiagnosticsConstants.SOURCE_CLIENT_IP.equals(source)
                || GatePilotDiagnosticsConstants.SOURCE_CLIENT_IP_UNDERSCORE.equals(source)
                || GatePilotDiagnosticsConstants.SOURCE_REMOTE_ADDRESS.equals(source)
                || GatePilotDiagnosticsConstants.SOURCE_REMOTE_ADDRESS_UNDERSCORE.equals(source)
                || GatePilotDiagnosticsConstants.SOURCE_REMOTE_ADDRESS_COMPACT.equals(source);
    }

    private String buildWeightedHashKey(PublishedConfig.PublishedPolicy policy,
                                        PublishedConfig.PublishedRoute route,
                                        NormalizedRequest request) {
        String identity = firstHeaderValue(request, weightHashHeaders(policy));
        if (identity == null) {
            identity = normalizeText(request.remoteAddress());
        }
        if (identity == null) {
            identity = normalizeText(request.rawQuery()) == null
                    ? request.path()
                    : request.path() + GatePilotDiagnosticsConstants.QUERY_SEPARATOR + request.rawQuery();
        }
        return route.getRouteId() + GatePilotDiagnosticsConstants.HASH_KEY_SEPARATOR + identity;
    }

    private List<String> weightHashHeaders(PublishedConfig.PublishedPolicy policy) {
        Object headers = policy.getConfig().get(PublishedConfigConstants.KEY_WEIGHT_HASH_HEADERS);
        if (!(headers instanceof Iterable<?> iterable)) {
            return GatePilotDiagnosticsConstants.DEFAULT_WEIGHT_HASH_HEADERS;
        }
        List<String> parsedHeaders = new ArrayList<>();
        for (Object item : iterable) {
            String header = normalizeText(Objects.toString(item, null));
            if (header != null) {
                parsedHeaders.add(header);
            }
        }
        return parsedHeaders.isEmpty()
                ? GatePilotDiagnosticsConstants.DEFAULT_WEIGHT_HASH_HEADERS
                : List.copyOf(parsedHeaders);
    }

    private String firstHeaderValue(NormalizedRequest request, List<String> headerNames) {
        for (String headerName : headerNames) {
            String value = normalizeText(firstValue(request.headers(), headerName, true));
            if (value != null) {
                return headerName + GatePilotDiagnosticsConstants.HEADER_VALUE_SEPARATOR + value;
            }
        }
        return null;
    }

    private int calculateBucket(String value) {
        CRC32 crc32 = new CRC32();
        crc32.update(Objects.toString(value, "").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return (int) (crc32.getValue() % GatePilotDiagnosticsConstants.WEIGHT_BUCKET_SIZE);
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

    private boolean projectMatches(PublishedConfig config, String projectName) {
        if (!StringUtils.hasText(projectName)) {
            return true;
        }
        ResourceReference projectRef = config.getSpec().getProjectRef();
        return projectRef != null && projectName.equals(projectRef.getName());
    }

    private boolean versionMatches(PublishedConfig config, String version) {
        return !StringUtils.hasText(version) || version.equals(config.getSpec().getVersion());
    }

    private boolean configShardMatches(PublishedConfig config, String configShard) {
        return !StringUtils.hasText(configShard) || configShard.equals(config.getSpec().getConfigShard());
    }

    private Long sequence(PublishedConfig config) {
        return Optional.ofNullable(config.getSpec().getSequence()).orElse(0L);
    }

    private Instant generatedAt(PublishedConfig config) {
        return Optional.ofNullable(config.getSpec().getGeneratedAt()).orElse(Instant.EPOCH);
    }

    private Instant updatedAt(PublishedConfig config) {
        return Optional.ofNullable(config.getMetadata().getUpdatedAt()).orElse(Instant.EPOCH);
    }

    private String effectiveNamespace(String namespace) {
        return StringUtils.hasText(namespace) ? namespace : GatePilotDiagnosticsConstants.DEFAULT_NAMESPACE;
    }

    private String resourceName(ResourceReference reference, String fallback) {
        return reference == null || !StringUtils.hasText(reference.getName()) ? fallback : reference.getName();
    }

    private String upstreamName(ResourceReference reference) {
        return reference == null ? null : reference.getName();
    }

    private String upstreamName(Object value) {
        if (value instanceof ResourceReference reference) {
            return upstreamName(reference);
        }
        if (value instanceof Map<?, ?> map) {
            return stringValue(map, PublishedConfigConstants.KEY_NAME);
        }
        return null;
    }

    private String normalizeRoutePath(String path) {
        if (!StringUtils.hasText(path)) {
            return GatePilotDiagnosticsConstants.ROOT_PATH;
        }
        String normalized = path.trim();
        normalized = normalized.startsWith(GatePilotDiagnosticsConstants.PATH_SEPARATOR)
                ? normalized
                : GatePilotDiagnosticsConstants.PATH_SEPARATOR + normalized;
        int queryIndex = normalized.indexOf(GatePilotDiagnosticsConstants.QUERY_SEPARATOR);
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        while (normalized.length() > 1 && normalized.endsWith(GatePilotDiagnosticsConstants.PATH_SEPARATOR)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeHost(String host) {
        if (!StringUtils.hasText(host)) {
            return null;
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        int portIndex = normalized.indexOf(GatePilotDiagnosticsConstants.HOST_PORT_SEPARATOR);
        if (portIndex > 0) {
            normalized = normalized.substring(0, portIndex);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeLiteral(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : defaultValue;
    }

    private String normalizeColor(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return GatePilotDiagnosticsConstants.COLOR_NAME_PATTERN.matcher(normalized).matches()
                ? normalized
                : defaultValue;
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String firstText(String... values) {
        for (String value : values) {
            String normalized = normalizeText(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String stringValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : Objects.toString(value, null);
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value == null ? null : Boolean.valueOf(value.toString());
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<Integer> integerList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Integer> values = new ArrayList<>();
        for (Object item : iterable) {
            Integer parsed = integerValue(item);
            if (parsed != null) {
                values.add(parsed);
            }
        }
        return List.copyOf(values);
    }

    private int iterableSize(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return 0;
        }
        int size = 0;
        for (Object ignored : iterable) {
            size++;
        }
        return size;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private record NormalizedRequest(String namespace,
                                     String method,
                                     String host,
                                     String path,
                                     String rawQuery,
                                     Map<String, List<String>> headers,
                                     Map<String, List<String>> query,
                                     Map<String, List<String>> cookies,
                                     String remoteAddress) {
    }

    private record ParsedPath(String path,
                              String rawQuery,
                              String host,
                              Map<String, List<String>> query) {
    }

    private record AuthDecision(boolean required,
                                boolean anonymousAllowed,
                                List<String> policyNames) {
    }

    private record TrafficDecision(String color,
                                   String source) {
    }

    private record TrafficColorRuleView(String source,
                                        String fieldName,
                                        String pattern,
                                        String matchStrategy,
                                        String color,
                                        Pattern regex) {

        private boolean matches(String candidate) {
            return switch (matchStrategy) {
                case GatePilotDiagnosticsConstants.MATCH_PREFIX -> candidate.startsWith(pattern);
                case GatePilotDiagnosticsConstants.MATCH_CONTAINS -> candidate.contains(pattern);
                case GatePilotDiagnosticsConstants.MATCH_REGEX -> regex != null && regex.matcher(candidate).matches();
                default -> candidate.equals(pattern);
            };
        }
    }

    private record TrafficSplitView(String target,
                                    String color,
                                    Integer weight,
                                    String upstreamName) {

        private boolean matches(String trafficColor) {
            return trafficColor.equals(target) || trafficColor.equals(color);
        }
    }
}
