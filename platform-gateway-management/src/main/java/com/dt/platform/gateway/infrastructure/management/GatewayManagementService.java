package com.dt.platform.gateway.infrastructure.management;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinition;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import com.dt.platform.gateway.infrastructure.validation.GatewayPropertiesValidator;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 网关管理面配置治理服务。
 */
public class GatewayManagementService {

    private final GatewayConfigStore configStore;

    private final GatewayConfigSnapshotRepository configSnapshotRepository;

    /**
     * 创建配置治理服务。
     *
     * @param configStore 管理端配置存储
     */
    public GatewayManagementService(GatewayProperties properties,
                                    GatewayRouteDefinitionLocator routeDefinitionLocator) {
        this(new GatewayConfigStore(properties), new GatewayConfigSnapshotRepository());
    }

    /**
     * 创建配置治理服务。
     *
     * @param properties 当前生效配置
     * @param routeDefinitionLocator 当前生效路由定义定位器
     * @param configSnapshotRepository 配置快照仓库
     */
    public GatewayManagementService(GatewayConfigStore configStore,
                                    GatewayConfigSnapshotRepository configSnapshotRepository) {
        this.configStore = configStore;
        this.configSnapshotRepository = configSnapshotRepository;
        this.configSnapshotRepository.saveActiveSnapshot(
                buildSummary(configStore.activeProperties(), configStore.activeRouteDefinitionLocator()),
                configStore.activeProperties()
        );
    }

    /**
     * 导出当前生效配置和编译摘要。
     *
     * @return 当前配置视图
     */
    public GatewayConfigExportView exportConfig() {
        GatewayProperties properties = configStore.activeProperties();
        GatewayRouteDefinitionLocator routeDefinitionLocator = configStore.activeRouteDefinitionLocator();
        return new GatewayConfigExportView(
                Instant.now(),
                buildSummary(properties, routeDefinitionLocator),
                properties
        );
    }

    /**
     * dry-run 校验候选配置。
     *
     * @param candidate 候选配置
     * @return 校验结果
     */
    public GatewayConfigValidationView validate(GatewayProperties candidate) {
        CompileResult compileResult = compile(candidate);
        return new GatewayConfigValidationView(
                compileResult.valid(),
                compileResult.errors(),
                compileResult.warnings(),
                compileResult.summary()
        );
    }

    /**
     * 对比当前配置和候选配置。
     *
     * @param candidate 候选配置
     * @return 配置差异
     */
    public GatewayConfigDiffView diff(GatewayProperties candidate) {
        GatewayProperties properties = configStore.activeProperties();
        GatewayRouteDefinitionLocator routeDefinitionLocator = configStore.activeRouteDefinitionLocator();
        CompileResult candidateResult = compile(candidate);
        GatewayConfigSummary currentSummary = buildSummary(properties, routeDefinitionLocator);
        if (!candidateResult.valid()) {
            return new GatewayConfigDiffView(
                    false,
                    candidateResult.errors(),
                    currentSummary,
                    candidateResult.summary(),
                    List.of()
            );
        }
        return new GatewayConfigDiffView(
                true,
                List.of(),
                currentSummary,
                candidateResult.summary(),
                compareConfig(properties, routeDefinitionLocator, candidate, candidateResult.locator())
        );
    }

    /**
     * 保存候选配置快照。
     *
     * @param request 快照请求
     * @return 配置快照
     */
    public GatewayConfigSnapshotRepository.GatewayConfigSnapshotRecord createCandidateSnapshot(
            GatewayConfigSnapshotRequest request) {
        GatewayProperties candidate = request == null ? null : request.properties();
        GatewayProperties properties = configStore.activeProperties();
        GatewayRouteDefinitionLocator routeDefinitionLocator = configStore.activeRouteDefinitionLocator();
        CompileResult compileResult = compile(candidate);
        List<GatewayConfigChangeView> changes = compileResult.valid()
                ? compareConfig(properties, routeDefinitionLocator, candidate, compileResult.locator())
                : List.of();
        List<String> warnings = new ArrayList<>(compileResult.warnings());
        if (compileResult.valid() && changes.isEmpty()) {
            warnings.add("Candidate config has no changes compared with current config");
        }
        return configSnapshotRepository.saveCandidateSnapshot(
                compileResult.valid() ? "VALID" : "INVALID",
                request == null ? null : request.operator(),
                request == null ? null : request.reason(),
                compileResult.summary(),
                compileResult.errors(),
                warnings,
                changes,
                candidate
        );
    }

    /**
     * 查询配置版本快照。
     *
     * @param status 状态过滤
     * @param limit 返回条数
     * @return 配置快照
     */
    public List<GatewayConfigSnapshotRepository.GatewayConfigSnapshotRecord> listConfigVersions(String status,
                                                                                                Integer limit) {
        return configSnapshotRepository.listSnapshots(status, limit);
    }

    /**
     * 查询单个配置版本快照。
     *
     * @param versionId 版本号
     * @return 配置快照
     */
    public GatewayConfigSnapshotRepository.GatewayConfigSnapshotRecord getConfigVersion(String versionId) {
        return configSnapshotRepository.findSnapshot(versionId)
                .orElseThrow(() -> new IllegalArgumentException("gateway config version not found: " + versionId));
    }

    /**
     * 查询配置发布事件记录。
     *
     * @param action 动作过滤
     * @param status 状态过滤
     * @param limit 返回条数
     * @return 发布事件记录
     */
    public List<GatewayConfigSnapshotRepository.GatewayConfigReleaseRecord> listReleaseRecords(String action,
                                                                                               String status,
                                                                                               Integer limit) {
        return configSnapshotRepository.listReleaseRecords(action, status, limit);
    }

    private CompileResult compile(GatewayProperties candidate) {
        if (candidate == null) {
            return new CompileResult(false, List.of("gateway candidate config must not be null"), List.of(), null, null);
        }
        try {
            new GatewayPropertiesValidator(candidate);
            GatewayRouteDefinitionLocator candidateLocator = new GatewayRouteDefinitionLocator(candidate);
            GatewayConfigSummary summary = buildSummary(candidate, candidateLocator);
            return new CompileResult(true, List.of(), buildWarnings(candidate, summary), candidateLocator, summary);
        } catch (RuntimeException exception) {
            return new CompileResult(false, List.of(errorMessage(exception)), List.of(), null, null);
        }
    }

    private String errorMessage(RuntimeException exception) {
        return StringUtils.hasText(exception.getMessage())
                ? exception.getMessage()
                : exception.getClass().getName();
    }

    private List<String> buildWarnings(GatewayProperties candidate, GatewayConfigSummary summary) {
        List<String> warnings = new ArrayList<>();
        if (!candidate.getAudit().isEnabled()) {
            warnings.add("Access audit is disabled, management console cannot query recent request events");
        }
        if (candidate.getAudit().isEnabled() && candidate.getAudit().getRecentCapacity() > 100_000) {
            warnings.add("Access audit recent capacity is very large, verify memory impact before publishing");
        }
        if (summary.releaseVariantCount() > 0 && !candidate.getTrafficColor().isEnabled()) {
            warnings.add("Release variants exist but traffic color is disabled");
        }
        if (!StringUtils.hasText(candidate.getInternalPrefix())) {
            warnings.add("Internal prefix is blank, management endpoints may be hard to protect");
        }
        return List.copyOf(warnings);
    }

    private GatewayConfigSummary buildSummary(GatewayProperties source,
                                              GatewayRouteDefinitionLocator locator) {
        int apiRouteCount = 0;
        int internalRouteCount = 0;
        int releaseVariantCount = 0;
        int retryRouteCount = 0;
        int flowControlRouteCount = 0;
        int circuitBreakerRouteCount = 0;
        for (GatewayRouteDefinition definition : locator.getRouteDefinitions()) {
            if (!definition.getApiPathRoots().isEmpty()) {
                apiRouteCount++;
            }
            if (!definition.getInternalPathRoots().isEmpty()) {
                internalRouteCount++;
            }
            releaseVariantCount += definition.getReleaseVariants().size();
            if (definition.getRetryPolicy() != null) {
                retryRouteCount++;
            }
            if (isFlowControlEnabled(source, definition)) {
                flowControlRouteCount++;
            }
            if (definition.getCircuitBreakerPolicy() != null) {
                circuitBreakerRouteCount++;
            }
        }
        return new GatewayConfigSummary(
                source.getProjects().size(),
                locator.getRouteDefinitions().size(),
                apiRouteCount,
                internalRouteCount,
                releaseVariantCount,
                retryRouteCount,
                flowControlRouteCount,
                circuitBreakerRouteCount
        );
    }

    private boolean isFlowControlEnabled(GatewayProperties source, GatewayRouteDefinition definition) {
        GatewayProperties.RouteProperties routeProperties = routeProperties(source, definition);
        return routeProperties != null
                && routeProperties.getGovernance() != null
                && routeProperties.getGovernance().getFlowControl() != null
                && routeProperties.getGovernance().getFlowControl().isEnabled();
    }

    private List<GatewayConfigChangeView> compareConfig(GatewayProperties properties,
                                                        GatewayRouteDefinitionLocator routeDefinitionLocator,
                                                        GatewayProperties candidate,
                                                        GatewayRouteDefinitionLocator candidateLocator) {
        List<GatewayConfigChangeView> changes = new ArrayList<>();
        compareGlobal(changes, buildGlobalSnapshot(properties), buildGlobalSnapshot(candidate));
        compareRoutes(changes, buildRouteSnapshots(properties, routeDefinitionLocator),
                buildRouteSnapshots(candidate, candidateLocator));
        return List.copyOf(changes);
    }

    private void compareGlobal(List<GatewayConfigChangeView> changes,
                               GlobalSnapshot before,
                               GlobalSnapshot after) {
        compareField(changes, "global", null, null, "apiPrefix", before.apiPrefix(), after.apiPrefix());
        compareField(changes, "global", null, null, "internalPrefix", before.internalPrefix(), after.internalPrefix());
        compareField(changes, "global", null, null, "authEnabled", before.authEnabled(), after.authEnabled());
        compareField(changes, "global", null, null, "corsEnabled", before.corsEnabled(), after.corsEnabled());
        compareField(changes, "global", null, null, "contextHeaders", before.contextHeaders(), after.contextHeaders());
        compareField(changes, "global", null, null, "trafficColor", before.trafficColor(), after.trafficColor());
        compareField(changes, "global", null, null, "audit", before.audit(), after.audit());
        compareField(changes, "global", null, null, "health", before.health(), after.health());
    }

    private void compareRoutes(List<GatewayConfigChangeView> changes,
                               Map<String, RouteSnapshot> beforeRoutes,
                               Map<String, RouteSnapshot> afterRoutes) {
        Set<String> routeKeys = new LinkedHashSet<>();
        routeKeys.addAll(beforeRoutes.keySet());
        routeKeys.addAll(afterRoutes.keySet());
        for (String routeKey : routeKeys) {
            RouteSnapshot before = beforeRoutes.get(routeKey);
            RouteSnapshot after = afterRoutes.get(routeKey);
            String projectKey = before == null ? after.projectKey() : before.projectKey();
            String actualRouteKey = before == null ? after.routeKey() : before.routeKey();
            if (before == null) {
                changes.add(new GatewayConfigChangeView("route", projectKey, actualRouteKey, "route", null, after));
                continue;
            }
            if (after == null) {
                changes.add(new GatewayConfigChangeView("route", projectKey, actualRouteKey, "route", before, null));
                continue;
            }
            compareRoute(changes, before, after);
        }
    }

    private void compareRoute(List<GatewayConfigChangeView> changes,
                              RouteSnapshot before,
                              RouteSnapshot after) {
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "projectSegment", before.projectSegment(), after.projectSegment());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "routeSegment", before.routeSegment(), after.routeSegment());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "apiPathRoots", before.apiPathRoots(), after.apiPathRoots());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "internalPathRoots", before.internalPathRoots(), after.internalPathRoots());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "serviceUri", before.serviceUri(), after.serviceUri());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "servicePathPrefix", before.servicePathPrefix(), after.servicePathPrefix());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "actuatorUri", before.actuatorUri(), after.actuatorUri());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "apiMethods", before.apiMethods(), after.apiMethods());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "internalMethods", before.internalMethods(), after.internalMethods());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "apiMaxRequestSizeBytes", before.apiMaxRequestSizeBytes(), after.apiMaxRequestSizeBytes());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "internalMaxRequestSizeBytes", before.internalMaxRequestSizeBytes(), after.internalMaxRequestSizeBytes());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "authRequired", before.authRequired(), after.authRequired());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "publicApiPaths", before.publicApiPaths(), after.publicApiPaths());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "connectTimeoutMs", before.connectTimeoutMs(), after.connectTimeoutMs());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "responseTimeoutMs", before.responseTimeoutMs(), after.responseTimeoutMs());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "retry", before.retry(), after.retry());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "flowControl", before.flowControl(), after.flowControl());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "circuitBreaker", before.circuitBreaker(), after.circuitBreaker());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "releaseWeightHashHeaders", before.releaseWeightHashHeaders(), after.releaseWeightHashHeaders());
        compareField(changes, "route", before.projectKey(), before.routeKey(),
                "releaseVariants", before.releaseVariants(), after.releaseVariants());
    }

    private void compareField(List<GatewayConfigChangeView> changes,
                              String category,
                              String projectKey,
                              String routeKey,
                              String field,
                              Object before,
                              Object after) {
        if (!Objects.equals(before, after)) {
            changes.add(new GatewayConfigChangeView(category, projectKey, routeKey, field, before, after));
        }
    }

    private GlobalSnapshot buildGlobalSnapshot(GatewayProperties source) {
        return new GlobalSnapshot(
                source.getApiPrefix(),
                source.getInternalPrefix(),
                source.getAuth().isEnabled(),
                source.getCors().isEnabled(),
                new ContextHeadersSnapshot(
                        source.getContextHeaders().isEnabled(),
                        source.getContextHeaders().getProjectHeaderName(),
                        source.getContextHeaders().getRouteHeaderName()
                ),
                new TrafficColorSnapshot(
                        source.getTrafficColor().isEnabled(),
                        source.getTrafficColor().getHeaderName(),
                        source.getTrafficColor().isResponseHeaderEnabled(),
                        source.getTrafficColor().isTrustRequestHeader(),
                        source.getTrafficColor().getDefaultColor()
                ),
                new AuditSnapshot(
                        source.getAudit().isEnabled(),
                        source.getAudit().isLogEnabled(),
                        source.getAudit().getRecentCapacity(),
                        source.getAudit().getTraceHeaderName()
                ),
                new HealthSnapshot(
                        source.getHealth().getPath(),
                        toMillis(source.getHealth().getTimeout())
                )
        );
    }

    private Map<String, RouteSnapshot> buildRouteSnapshots(GatewayProperties source,
                                                           GatewayRouteDefinitionLocator locator) {
        Map<String, RouteSnapshot> snapshots = new LinkedHashMap<>();
        for (GatewayRouteDefinition definition : locator.getRouteDefinitions()) {
            GatewayProperties.RouteProperties routeProperties = routeProperties(source, definition);
            snapshots.put(routeSnapshotKey(definition), buildRouteSnapshot(definition, routeProperties));
        }
        return snapshots;
    }

    private RouteSnapshot buildRouteSnapshot(GatewayRouteDefinition definition,
                                             GatewayProperties.RouteProperties routeProperties) {
        return new RouteSnapshot(
                definition.getProjectKey(),
                definition.getRouteKey(),
                definition.getProjectSegment(),
                definition.getRouteSegment(),
                definition.getApiPathRoots(),
                definition.getInternalPathRoots(),
                definition.getServiceUri(),
                definition.getServicePathPrefix(),
                definition.getActuatorUri(),
                definition.getApiMethods(),
                definition.getInternalMethods(),
                definition.getApiMaxRequestSize() == null ? null : definition.getApiMaxRequestSize().toBytes(),
                definition.getInternalMaxRequestSize() == null ? null : definition.getInternalMaxRequestSize().toBytes(),
                definition.isAuthRequired(),
                definition.getPublicApiPaths(),
                definition.getConnectTimeoutMs(),
                toMillis(definition.getResponseTimeout()),
                buildRetrySnapshot(definition.getRetryPolicy()),
                buildFlowControlSnapshot(routeProperties),
                buildCircuitBreakerSnapshot(definition.getCircuitBreakerPolicy()),
                definition.getReleaseWeightHashHeaders(),
                buildReleaseVariantSnapshots(definition.getReleaseVariants())
        );
    }

    private RetrySnapshot buildRetrySnapshot(GatewayRouteDefinition.RetryPolicy retryPolicy) {
        if (retryPolicy == null) {
            return new RetrySnapshot(false, null, List.of(), List.of(), List.of(), List.of(), null);
        }
        return new RetrySnapshot(
                true,
                retryPolicy.retries(),
                retryPolicy.methods(),
                retryPolicy.statuses(),
                retryPolicy.series(),
                retryPolicy.exceptions(),
                retryPolicy.backoff() == null
                        ? null
                        : new RetryBackoffSnapshot(
                        toMillis(retryPolicy.backoff().firstBackoff()),
                        toMillis(retryPolicy.backoff().maxBackoff()),
                        retryPolicy.backoff().factor(),
                        retryPolicy.backoff().basedOnPreviousValue()
                )
        );
    }

    private FlowControlSnapshot buildFlowControlSnapshot(GatewayProperties.RouteProperties routeProperties) {
        if (routeProperties == null
                || routeProperties.getGovernance() == null
                || routeProperties.getGovernance().getFlowControl() == null) {
            return new FlowControlSnapshot(false, null, null, null, null, null, null);
        }
        GatewayProperties.FlowControlProperties flowControl = routeProperties.getGovernance().getFlowControl();
        GatewayProperties.FlowControlParamProperties param = flowControl.getParam();
        return new FlowControlSnapshot(
                flowControl.isEnabled(),
                flowControl.getCount(),
                flowControl.getIntervalSec(),
                flowControl.getBurst(),
                flowControl.getControlBehavior(),
                flowControl.getMaxQueueingTimeoutMs(),
                param == null
                        ? null
                        : new FlowControlParamSnapshot(
                        param.isEnabled(),
                        param.getParseStrategy(),
                        param.getFieldName(),
                        param.getPattern(),
                        param.getMatchStrategy()
                )
        );
    }

    private CircuitBreakerSnapshot buildCircuitBreakerSnapshot(GatewayRouteDefinition.CircuitBreakerPolicy policy) {
        if (policy == null) {
            return new CircuitBreakerSnapshot(false, null, List.of(), null, null, null, null, null, null, null, null, null, null, null);
        }
        return new CircuitBreakerSnapshot(
                true,
                policy.name(),
                policy.statusCodes(),
                policy.fallbackUri(),
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

    private List<ReleaseVariantSnapshot> buildReleaseVariantSnapshots(List<GatewayRouteDefinition.ReleaseVariant> variants) {
        return variants.stream()
                .map(variant -> new ReleaseVariantSnapshot(
                        variant.variantKey(),
                        variant.matchColors(),
                        variant.weight(),
                        variant.serviceUri(),
                        variant.servicePathPrefix(),
                        variant.actuatorUri(),
                        variant.connectTimeoutMs(),
                        toMillis(variant.responseTimeout())
                ))
                .toList();
    }

    private GatewayProperties.RouteProperties routeProperties(GatewayProperties source,
                                                             GatewayRouteDefinition definition) {
        GatewayProperties.ProjectProperties project = source.getProjects().get(definition.getProjectKey());
        return project == null ? null : project.getRoutes().get(definition.getRouteKey());
    }

    private String routeSnapshotKey(GatewayRouteDefinition definition) {
        return definition.getProjectKey() + "/" + definition.getRouteKey();
    }

    private Long toMillis(Duration duration) {
        return duration == null ? null : duration.toMillis();
    }

    /**
     * 当前配置导出视图。
     */
    public record GatewayConfigExportView(Instant exportedAt,
                                          GatewayConfigSummary summary,
                                          GatewayProperties properties) {
    }

    /**
     * 候选配置 dry-run 校验结果。
     */
    public record GatewayConfigValidationView(boolean valid,
                                              List<String> errors,
                                              List<String> warnings,
                                              GatewayConfigSummary summary) {
    }

    /**
     * 候选配置与当前配置 diff 结果。
     */
    public record GatewayConfigDiffView(boolean valid,
                                        List<String> errors,
                                        GatewayConfigSummary currentSummary,
                                        GatewayConfigSummary candidateSummary,
                                        List<GatewayConfigChangeView> changes) {
    }

    /**
     * 创建候选配置快照请求。
     */
    public record GatewayConfigSnapshotRequest(String operator,
                                               String reason,
                                               GatewayProperties properties) {
    }

    /**
     * 单条配置变化。
     */
    public record GatewayConfigChangeView(String category,
                                          String projectKey,
                                          String routeKey,
                                          String field,
                                          Object before,
                                          Object after) {
    }

    /**
     * 编译后的配置摘要。
     */
    public record GatewayConfigSummary(int projectCount,
                                       int routeCount,
                                       int apiRouteCount,
                                       int internalRouteCount,
                                       int releaseVariantCount,
                                       int retryRouteCount,
                                       int flowControlRouteCount,
                                       int circuitBreakerRouteCount) {
    }

    private record CompileResult(boolean valid,
                                 List<String> errors,
                                 List<String> warnings,
                                 GatewayRouteDefinitionLocator locator,
                                 GatewayConfigSummary summary) {
    }

    public record GlobalSnapshot(String apiPrefix,
                                 String internalPrefix,
                                 boolean authEnabled,
                                 boolean corsEnabled,
                                 ContextHeadersSnapshot contextHeaders,
                                 TrafficColorSnapshot trafficColor,
                                 AuditSnapshot audit,
                                 HealthSnapshot health) {
    }

    public record ContextHeadersSnapshot(boolean enabled,
                                         String projectHeaderName,
                                         String routeHeaderName) {
    }

    public record TrafficColorSnapshot(boolean enabled,
                                       String headerName,
                                       boolean responseHeaderEnabled,
                                       boolean trustRequestHeader,
                                       String defaultColor) {
    }

    public record AuditSnapshot(boolean enabled,
                                boolean logEnabled,
                                int recentCapacity,
                                String traceHeaderName) {
    }

    public record HealthSnapshot(String path,
                                 Long timeoutMs) {
    }

    public record RouteSnapshot(String projectKey,
                                String routeKey,
                                String projectSegment,
                                String routeSegment,
                                List<String> apiPathRoots,
                                List<String> internalPathRoots,
                                URI serviceUri,
                                String servicePathPrefix,
                                URI actuatorUri,
                                List<String> apiMethods,
                                List<String> internalMethods,
                                Long apiMaxRequestSizeBytes,
                                Long internalMaxRequestSizeBytes,
                                boolean authRequired,
                                List<String> publicApiPaths,
                                Integer connectTimeoutMs,
                                Long responseTimeoutMs,
                                RetrySnapshot retry,
                                FlowControlSnapshot flowControl,
                                CircuitBreakerSnapshot circuitBreaker,
                                List<String> releaseWeightHashHeaders,
                                List<ReleaseVariantSnapshot> releaseVariants) {
    }

    public record RetrySnapshot(boolean enabled,
                                Integer retries,
                                List<String> methods,
                                List<Integer> statuses,
                                List<String> series,
                                List<String> exceptions,
                                RetryBackoffSnapshot backoff) {
    }

    public record RetryBackoffSnapshot(Long firstBackoffMs,
                                       Long maxBackoffMs,
                                       int factor,
                                       boolean basedOnPreviousValue) {
    }

    public record FlowControlSnapshot(boolean enabled,
                                      Double count,
                                      Long intervalSec,
                                      Integer burst,
                                      String controlBehavior,
                                      Integer maxQueueingTimeoutMs,
                                      FlowControlParamSnapshot param) {
    }

    public record FlowControlParamSnapshot(boolean enabled,
                                           String parseStrategy,
                                           String fieldName,
                                           String pattern,
                                           String matchStrategy) {
    }

    public record CircuitBreakerSnapshot(boolean enabled,
                                         String name,
                                         List<Integer> statusCodes,
                                         URI fallbackUri,
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

    public record ReleaseVariantSnapshot(String variantKey,
                                         List<String> matchColors,
                                         int weight,
                                         URI serviceUri,
                                         String servicePathPrefix,
                                         URI actuatorUri,
                                         Integer connectTimeoutMs,
                                         Long responseTimeoutMs) {
    }
}
