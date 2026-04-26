package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.apiserver.application.command.CreateReleaseCommand;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplateApplyResponse;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplateDefaultsResponse;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplateDryRunResponse;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplatePreviewResponse;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplateRenderRequest;
import com.dt.gatepilot.apiserver.application.dto.ReleaseDryRunResult;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceType;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.dt.gatepilot.domain.enums.AuthType;
import com.dt.gatepilot.domain.enums.HttpMethod;
import com.dt.gatepilot.domain.enums.LoadBalanceStrategy;
import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.domain.enums.ReleaseStrategy;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.enums.TrafficColorSource;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.policy.AuthPolicy;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.project.GatewayProject;
import com.dt.gatepilot.domain.resource.route.GatewayRoute;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import com.getboot.exception.api.code.CommonErrorCode;
import com.getboot.exception.api.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * 项目接入模板服务
 */
@Service
@ConditionalOnGatePilotApiserverEnabled
public class ProjectTemplateService {

    private final GatePilotResourceService resourceService;

    /**
     * 创建项目接入模板服务
     *
     * @param resourceService 资源服务
     */
    public ProjectTemplateService(GatePilotResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 查询项目接入模板默认值
     *
     * @return 默认配置响应
     */
    public ProjectTemplateDefaultsResponse defaults() {
        ProjectTemplateDefaultsResponse response = new ProjectTemplateDefaultsResponse();
        response.setValues(defaultValues());
        response.setEnvironments(ProjectTemplateConstants.ENVIRONMENT_OPTIONS.stream()
                .map(value -> option(value, value, true))
                .toList());
        response.setProtocols(ProjectTemplateConstants.PROTOCOL_OPTIONS.stream()
                .map(value -> option(value.name(), ProjectTemplateConstants.PROTOCOL_LABELS.get(value), true))
                .toList());
        response.setLoadBalances(ProjectTemplateConstants.LOAD_BALANCE_OPTIONS.stream()
                .map(value -> option(value.name(), ProjectTemplateConstants.LOAD_BALANCE_LABELS.get(value),
                        value.isSupported()))
                .toList());
        response.setReleaseStrategies(ProjectTemplateConstants.RELEASE_STRATEGY_OPTIONS.stream()
                .map(value -> option(value.name(), ProjectTemplateConstants.RELEASE_STRATEGY_LABELS.get(value), true))
                .toList());
        response.setAuthTypes(ProjectTemplateConstants.AUTH_TYPE_OPTIONS.stream()
                .map(value -> option(value.name(), ProjectTemplateConstants.AUTH_TYPE_LABELS.get(value), true))
                .toList());
        return response;
    }

    /**
     * 预览项目接入模板
     *
     * @param request 模板 values
     * @return 预览响应
     */
    public ProjectTemplatePreviewResponse preview(ProjectTemplateRenderRequest request) {
        TemplateValues values = normalize(request);
        List<ResourceEnvelope> envelopes = render(values);
        ProjectTemplatePreviewResponse response = new ProjectTemplatePreviewResponse();
        response.setNamespace(values.namespace());
        response.setProjectName(values.projectName());
        response.setConfigShard(values.configShard());
        response.setReleaseRequest(releaseRequest(values, envelopes));
        for (ResourceEnvelope envelope : envelopes) {
            response.getResources().add(renderedResource(envelope));
        }
        fillDiff(response);
        return response;
    }

    /**
     * dry-run 项目接入模板
     *
     * @param request 模板 values
     * @return dry-run 响应
     */
    public ProjectTemplateDryRunResponse dryRun(ProjectTemplateRenderRequest request) {
        ProjectTemplateDryRunResponse response = new ProjectTemplateDryRunResponse();
        response.setPreview(preview(request));
        validate(response, request);
        response.setPassed(response.getMessages().isEmpty());
        return response;
    }

    /**
     * 保存项目接入模板渲染出的资源
     *
     * @param request 模板 values
     * @return 保存响应
     */
    public ProjectTemplateApplyResponse apply(ProjectTemplateRenderRequest request) {
        ProjectTemplateDryRunResponse dryRun = dryRun(request);
        if (!dryRun.isPassed()) {
            throw BusinessException.of(CommonErrorCode.PARAM_ERROR.code(),
                    ProjectTemplateConstants.MESSAGE_TEMPLATE_DRY_RUN_FAILED);
        }
        int saved = 0;
        for (ProjectTemplatePreviewResponse.RenderedResource resource : dryRun.getPreview().getResources()) {
            if (ProjectTemplateConstants.ACTION_UNCHANGED.equals(resource.getAction())) {
                continue;
            }
            ResourceKind kind = ResourceKind.valueOf(resource.getKind());
            GatePilotResourceType resourceType = resourceService.requireResourceType(kind);
            // 模板只保存有变化的声明式资源，不创建发布意图
            resourceService.save(resourceType, resource.getNamespace(), resource.getName(), resource.getResource());
            saved++;
        }
        ProjectTemplateApplyResponse response = new ProjectTemplateApplyResponse();
        response.setSavedResourceCount(saved);
        response.setDryRun(dryRun);
        return response;
    }

    private void validate(ProjectTemplateDryRunResponse response, ProjectTemplateRenderRequest request) {
        ProjectTemplateRenderRequest.UpstreamValues upstream = request.getUpstream();
        LoadBalanceStrategy strategy = upstream == null ? null : upstream.getLoadBalance();
        if (strategy != null && !strategy.isSupported()) {
            addMessage(response, ProjectTemplateConstants.REASON_TEMPLATE_INVALID,
                    ProjectTemplateConstants.MESSAGE_UNSUPPORTED_LOAD_BALANCE_PREFIX + strategy.name());
        }
    }

    private void addMessage(ProjectTemplateDryRunResponse response, String reason, String message) {
        ReleaseDryRunResult.DryRunMessage dryRunMessage = new ReleaseDryRunResult.DryRunMessage();
        // 模板 dry-run 消息直接给 Console 展示
        dryRunMessage.setLevel(ProjectTemplateConstants.DRY_RUN_LEVEL_ERROR);
        dryRunMessage.setReason(reason);
        dryRunMessage.setMessage(message);
        response.getMessages().add(dryRunMessage);
    }

    private ProjectTemplateRenderRequest defaultValues() {
        ProjectTemplateRenderRequest request = new ProjectTemplateRenderRequest();
        request.setNamespace(ResourceMetadataConstants.DEFAULT_NAMESPACE);
        request.setProjectName(ProjectTemplateConstants.DEFAULT_PROJECT_NAME);
        request.setDisplayName(ProjectTemplateConstants.DEFAULT_PROJECT_NAME);
        request.setEnvironment(ProjectTemplateConstants.DEFAULT_ENVIRONMENT);
        request.setTrafficTier(ProjectTemplateConstants.DEFAULT_TRAFFIC_TIER);
        request.setConfigShard(ProjectTemplateConstants.DEFAULT_CONFIG_SHARD);
        request.getRoute().setHost(ProjectTemplateConstants.DEFAULT_HOST);
        request.getRoute().setPath(ProjectTemplateConstants.DEFAULT_PATH);
        request.getRoute().setStripPrefix(true);
        request.getRoute().setMethods(List.of(HttpMethod.ANY));
        request.getUpstream().setHost(ProjectTemplateConstants.DEFAULT_UPSTREAM_HOST);
        request.getUpstream().setPort(ProjectTemplateConstants.DEFAULT_HTTP_PORT);
        request.getUpstream().setProtocol(Protocol.HTTP);
        request.getUpstream().setLoadBalance(LoadBalanceStrategy.ROUND_ROBIN);
        request.getUpstream().setHealthCheckEnabled(true);
        request.getUpstream().setHealthPath(ProjectTemplateConstants.DEFAULT_HEALTH_PATH);
        request.getCandidate().setHost(ProjectTemplateConstants.DEFAULT_UPSTREAM_HOST);
        request.getCandidate().setPort(ProjectTemplateConstants.DEFAULT_HTTP_PORT);
        request.getGovernance().setRateLimitEnabled(false);
        request.getGovernance().setRequestsPerSecond(ProjectTemplateConstants.DEFAULT_REQUESTS_PER_SECOND);
        request.getGovernance().setBurstCapacity(ProjectTemplateConstants.DEFAULT_BURST_CAPACITY);
        request.getGovernance().setRetryEnabled(false);
        request.getGovernance().setMaxAttempts(ProjectTemplateConstants.DEFAULT_MAX_ATTEMPTS);
        request.getRelease().setStrategy(ReleaseStrategy.TRAFFIC_SPLIT);
        request.getRelease().setCandidateWeight(ProjectTemplateConstants.DEFAULT_CANDIDATE_WEIGHT);
        request.getRelease().setColorHeader(ProjectTemplateConstants.DEFAULT_COLOR_HEADER);
        request.getRelease().setCandidateColor(ProjectTemplateConstants.DEFAULT_CANDIDATE_COLOR);
        request.getAuth().setType(AuthType.NONE);
        request.getAuth().setAnonymousAllowed(true);
        return request;
    }

    private ProjectTemplateDefaultsResponse.OptionItem option(String value, String label, boolean enabled) {
        ProjectTemplateDefaultsResponse.OptionItem item = new ProjectTemplateDefaultsResponse.OptionItem();
        item.setValue(value);
        item.setLabel(label);
        item.setEnabled(enabled);
        return item;
    }

    private ProjectTemplatePreviewResponse.RenderedResource renderedResource(ResourceEnvelope envelope) {
        ProjectTemplatePreviewResponse.RenderedResource rendered = new ProjectTemplatePreviewResponse.RenderedResource();
        GatePilotResourceType resourceType = resourceService.requireResourceType(envelope.kind());
        rendered.setKind(envelope.kind().name());
        rendered.setResourceType(resourceType.getPath());
        rendered.setNamespace(envelope.namespace());
        rendered.setName(envelope.name());
        rendered.setResource(envelope.resource());
        rendered.setAction(action(envelope, resourceType));
        return rendered;
    }

    private String action(ResourceEnvelope envelope, GatePilotResourceType resourceType) {
        try {
            Object existing = resourceService.get(resourceType.getPath(), envelope.namespace(), envelope.name());
            if (Objects.equals(fingerprint(existing), fingerprint(envelope.resource()))) {
                return ProjectTemplateConstants.ACTION_UNCHANGED;
            }
            return ProjectTemplateConstants.ACTION_UPDATE;
        } catch (ResponseStatusException exception) {
            return ProjectTemplateConstants.ACTION_CREATE;
        }
    }

    private ResourceFingerprint fingerprint(Object resource) {
        ResourceMetadata metadata = metadataOf(resource);
        Map<String, String> labels = metadata.getLabels() == null ? Map.of() : Map.copyOf(metadata.getLabels());
        // diff 只比较影响运行态的 spec 和 labels，避免时间戳造成误判
        return new ResourceFingerprint(specOf(resource), labels);
    }

    private ResourceMetadata metadataOf(Object resource) {
        if (resource instanceof GatewayProject item) {
            return item.getMetadata();
        }
        if (resource instanceof GatewayRoute item) {
            return item.getMetadata();
        }
        if (resource instanceof TrafficPolicy item) {
            return item.getMetadata();
        }
        if (resource instanceof ReleasePolicy item) {
            return item.getMetadata();
        }
        if (resource instanceof AuthPolicy item) {
            return item.getMetadata();
        }
        if (resource instanceof Upstream item) {
            return item.getMetadata();
        }
        throw new BusinessException(CommonErrorCode.PARAM_ERROR.code(),
                ProjectTemplateConstants.MESSAGE_UNSUPPORTED_TEMPLATE_RESOURCE_TYPE + resource.getClass().getName());
    }

    private Object specOf(Object resource) {
        if (resource instanceof GatewayProject item) {
            return item.getSpec();
        }
        if (resource instanceof GatewayRoute item) {
            return item.getSpec();
        }
        if (resource instanceof TrafficPolicy item) {
            return item.getSpec();
        }
        if (resource instanceof ReleasePolicy item) {
            return item.getSpec();
        }
        if (resource instanceof AuthPolicy item) {
            return item.getSpec();
        }
        if (resource instanceof Upstream item) {
            return item.getSpec();
        }
        throw new BusinessException(CommonErrorCode.PARAM_ERROR.code(),
                ProjectTemplateConstants.MESSAGE_UNSUPPORTED_TEMPLATE_RESOURCE_TYPE + resource.getClass().getName());
    }

    private void fillDiff(ProjectTemplatePreviewResponse response) {
        ProjectTemplatePreviewResponse.TemplateDiff diff = response.getDiff();
        for (ProjectTemplatePreviewResponse.RenderedResource resource : response.getResources()) {
            if (ProjectTemplateConstants.ACTION_CREATE.equals(resource.getAction())) {
                diff.setCreateCount(diff.getCreateCount() + 1);
            } else if (ProjectTemplateConstants.ACTION_UPDATE.equals(resource.getAction())) {
                diff.setUpdateCount(diff.getUpdateCount() + 1);
            } else {
                diff.setUnchangedCount(diff.getUnchangedCount() + 1);
            }
        }
        diff.setChanged(diff.getCreateCount() > 0 || diff.getUpdateCount() > 0);
    }

    private CreateReleaseCommand releaseRequest(TemplateValues values, List<ResourceEnvelope> envelopes) {
        CreateReleaseCommand command = new CreateReleaseCommand();
        command.setNamespace(values.namespace());
        command.setProjectName(values.projectName());
        command.setConfigShard(values.configShard());
        command.setDescription(ProjectTemplateConstants.DEFAULT_RELEASE_DESCRIPTION);
        command.setCreatedBy(ProjectTemplateConstants.DEFAULT_CREATED_BY);
        command.setResourceRefs(envelopes.stream()
                .map(envelope -> ref(envelope.kind(), envelope.namespace(), envelope.name()))
                .toList());
        return command;
    }

    private List<ResourceEnvelope> render(TemplateValues values) {
        List<ResourceEnvelope> resources = new ArrayList<>();
        resources.add(new ResourceEnvelope(ResourceKind.GATEWAY_PROJECT, values.namespace(), values.projectName(),
                project(values)));
        String stableUpstreamName = name(values.projectName(), ProjectTemplateConstants.SUFFIX_STABLE_UPSTREAM);
        String routeName = name(values.projectName(), ProjectTemplateConstants.SUFFIX_ROUTE);
        String trafficPolicyName = name(values.projectName(), ProjectTemplateConstants.SUFFIX_TRAFFIC_POLICY);
        String authPolicyName = name(values.projectName(), ProjectTemplateConstants.SUFFIX_AUTH_POLICY);
        String releasePolicyName = name(values.projectName(), ProjectTemplateConstants.SUFFIX_RELEASE_POLICY);
        boolean releaseEnabled = values.releaseEnabled();
        String candidateUpstreamName = name(values.projectName(), ProjectTemplateConstants.SUFFIX_CANDIDATE_UPSTREAM);
        resources.add(new ResourceEnvelope(ResourceKind.UPSTREAM, values.namespace(), stableUpstreamName,
                upstream(values, stableUpstreamName, values.upstreamHost(), values.upstreamPort())));
        if (releaseEnabled) {
            resources.add(new ResourceEnvelope(ResourceKind.UPSTREAM, values.namespace(), candidateUpstreamName,
                    upstream(values, candidateUpstreamName, values.candidateHost(), values.candidatePort())));
        }
        resources.add(new ResourceEnvelope(ResourceKind.TRAFFIC_POLICY, values.namespace(), trafficPolicyName,
                trafficPolicy(values, trafficPolicyName, routeName, releaseEnabled)));
        if (values.authType() != AuthType.NONE) {
            resources.add(new ResourceEnvelope(ResourceKind.AUTH_POLICY, values.namespace(), authPolicyName,
                    authPolicy(values, authPolicyName, routeName)));
        }
        if (releaseEnabled) {
            resources.add(new ResourceEnvelope(ResourceKind.RELEASE_POLICY, values.namespace(), releasePolicyName,
                    releasePolicy(values, releasePolicyName, routeName, stableUpstreamName, candidateUpstreamName)));
        }
        resources.add(new ResourceEnvelope(ResourceKind.GATEWAY_ROUTE, values.namespace(), routeName,
                route(values, routeName, stableUpstreamName, trafficPolicyName, authPolicyName, releasePolicyName,
                        releaseEnabled)));
        return resources;
    }

    private GatewayProject project(TemplateValues values) {
        GatewayProject project = new GatewayProject();
        decorate(project.getMetadata(), values, values.projectName());
        project.getSpec().setDisplayName(values.displayName());
        project.getSpec().setOwnerTeam(values.ownerTeam());
        project.getSpec().setEnvironment(values.environment());
        project.getSpec().setTrafficTier(values.trafficTier());
        project.getSpec().setConfigShard(values.configShard());
        project.getSpec().setIsolationGroup(values.isolationGroup());
        project.getSpec().getDomains().add(values.host());
        return project;
    }

    private Upstream upstream(TemplateValues values, String name, String host, Integer port) {
        Upstream upstream = new Upstream();
        decorate(upstream.getMetadata(), values, name);
        upstream.getSpec().setProjectRef(projectRef(values));
        upstream.getSpec().setProtocol(values.protocol());
        upstream.getSpec().setLoadBalance(values.loadBalance());
        Upstream.UpstreamEndpoint endpoint = new Upstream.UpstreamEndpoint();
        endpoint.setHost(host);
        endpoint.setPort(port);
        endpoint.setWeight(ProjectTemplateConstants.MAX_TRAFFIC_WEIGHT);
        upstream.getSpec().getEndpoints().add(endpoint);
        upstream.getSpec().getHealthCheck().setEnabled(values.healthCheckEnabled());
        upstream.getSpec().getHealthCheck().setPath(values.healthPath());
        upstream.getSpec().getHealthCheck().setTimeout(ProjectTemplateConstants.DEFAULT_HEALTH_TIMEOUT);
        upstream.getSpec().getHealthCheck().setUnhealthyThreshold(ProjectTemplateConstants.DEFAULT_UNHEALTHY_THRESHOLD);
        return upstream;
    }

    private TrafficPolicy trafficPolicy(TemplateValues values,
                                        String name,
                                        String routeName,
                                        boolean releaseEnabled) {
        TrafficPolicy policy = new TrafficPolicy();
        decorate(policy.getMetadata(), values, name);
        policy.getSpec().setProjectRef(projectRef(values));
        policy.getSpec().getTargetRefs().add(ref(ResourceKind.GATEWAY_ROUTE, values.namespace(), routeName));
        policy.getSpec().getRateLimit().setEnabled(values.rateLimitEnabled());
        policy.getSpec().getRateLimit().setRequestsPerSecond(values.requestsPerSecond());
        policy.getSpec().getRateLimit().setBurstCapacity(values.burstCapacity());
        policy.getSpec().getRetry().setEnabled(values.retryEnabled());
        policy.getSpec().getRetry().setMaxAttempts(values.maxAttempts());
        policy.getSpec().getRetry().getStatuses().addAll(ProjectTemplateConstants.DEFAULT_RETRY_STATUSES);
        if (releaseEnabled) {
            TrafficPolicy.TrafficColorRule rule = new TrafficPolicy.TrafficColorRule();
            rule.setSource(TrafficColorSource.HEADER);
            rule.setKey(values.colorHeader());
            rule.setMatch(values.candidateColor());
            rule.setColor(values.candidateColor());
            rule.getPropagateHeaders().put(values.colorHeader(), values.candidateColor());
            policy.getSpec().getColorRules().add(rule);
        }
        return policy;
    }

    private AuthPolicy authPolicy(TemplateValues values, String name, String routeName) {
        AuthPolicy policy = new AuthPolicy();
        decorate(policy.getMetadata(), values, name);
        policy.getSpec().setProjectRef(projectRef(values));
        policy.getSpec().setType(values.authType());
        policy.getSpec().setAnonymousAllowed(values.anonymousAllowed());
        policy.getSpec().getTargetRefs().add(ref(ResourceKind.GATEWAY_ROUTE, values.namespace(), routeName));
        return policy;
    }

    private ReleasePolicy releasePolicy(TemplateValues values,
                                        String name,
                                        String routeName,
                                        String stableUpstreamName,
                                        String candidateUpstreamName) {
        ReleasePolicy policy = new ReleasePolicy();
        decorate(policy.getMetadata(), values, name);
        policy.getSpec().setProjectRef(projectRef(values));
        policy.getSpec().setStrategy(values.releaseStrategy());
        policy.getSpec().getRouteRefs().add(ref(ResourceKind.GATEWAY_ROUTE, values.namespace(), routeName));
        policy.getSpec().setStableUpstreamRef(ref(ResourceKind.UPSTREAM, values.namespace(), stableUpstreamName));
        policy.getSpec().setCandidateUpstreamRef(ref(ResourceKind.UPSTREAM, values.namespace(), candidateUpstreamName));
        policy.getSpec().getTrafficSplits().add(split(ProjectTemplateConstants.RELEASE_TARGET_STABLE,
                stableUpstreamName, ProjectTemplateConstants.MAX_TRAFFIC_WEIGHT - values.candidateWeight(), null,
                values));
        policy.getSpec().getTrafficSplits().add(split(ProjectTemplateConstants.RELEASE_TARGET_CANDIDATE,
                candidateUpstreamName, values.candidateWeight(), values.candidateColor(), values));
        return policy;
    }

    private ReleasePolicy.TrafficSplit split(String target,
                                             String upstreamName,
                                             int weight,
                                             String color,
                                             TemplateValues values) {
        ReleasePolicy.TrafficSplit split = new ReleasePolicy.TrafficSplit();
        split.setTarget(target);
        split.setUpstreamRef(ref(ResourceKind.UPSTREAM, values.namespace(), upstreamName));
        split.setWeight(weight);
        split.setColor(color);
        return split;
    }

    private GatewayRoute route(TemplateValues values,
                               String routeName,
                               String stableUpstreamName,
                               String trafficPolicyName,
                               String authPolicyName,
                               String releasePolicyName,
                               boolean releaseEnabled) {
        GatewayRoute route = new GatewayRoute();
        decorate(route.getMetadata(), values, routeName);
        route.getSpec().setProjectRef(projectRef(values));
        route.getSpec().getProtocols().add(Protocol.HTTP);
        route.getSpec().getHosts().add(values.host());
        route.getSpec().getPath().setType("Prefix");
        route.getSpec().getPath().setValue(values.path());
        route.getSpec().getPath().setStripPrefix(values.stripPrefix());
        route.getSpec().getMethods().addAll(values.methods());
        route.getSpec().setUpstreamRef(ref(ResourceKind.UPSTREAM, values.namespace(), stableUpstreamName));
        route.getSpec().getPolicyRefs().add(ref(ResourceKind.TRAFFIC_POLICY, values.namespace(), trafficPolicyName));
        if (values.authType() != AuthType.NONE) {
            route.getSpec().getPolicyRefs().add(ref(ResourceKind.AUTH_POLICY, values.namespace(), authPolicyName));
        }
        if (releaseEnabled) {
            route.getSpec().getPolicyRefs().add(ref(ResourceKind.RELEASE_POLICY, values.namespace(), releasePolicyName));
        }
        return route;
    }

    private void decorate(ResourceMetadata metadata, TemplateValues values, String name) {
        metadata.setNamespace(values.namespace());
        metadata.setName(name);
        metadata.getLabels().put(ResourceMetadataConstants.LABEL_PROJECT, values.projectName());
        if (StringUtils.hasText(values.configShard())) {
            metadata.getLabels().put(ResourceMetadataConstants.LABEL_CONFIG_SHARD, values.configShard());
        }
    }

    private ResourceReference projectRef(TemplateValues values) {
        return ref(ResourceKind.GATEWAY_PROJECT, values.namespace(), values.projectName());
    }

    private ResourceReference ref(ResourceKind kind, String namespace, String name) {
        ResourceReference reference = new ResourceReference();
        reference.setKind(kind);
        reference.setNamespace(namespace);
        reference.setName(name);
        return reference;
    }

    private TemplateValues normalize(ProjectTemplateRenderRequest request) {
        ProjectTemplateRenderRequest.RouteValues route = Objects.requireNonNullElseGet(request.getRoute(),
                ProjectTemplateRenderRequest.RouteValues::new);
        ProjectTemplateRenderRequest.UpstreamValues upstream = Objects.requireNonNullElseGet(request.getUpstream(),
                ProjectTemplateRenderRequest.UpstreamValues::new);
        ProjectTemplateRenderRequest.CandidateUpstreamValues candidate = Objects.requireNonNullElseGet(
                request.getCandidate(), ProjectTemplateRenderRequest.CandidateUpstreamValues::new);
        ProjectTemplateRenderRequest.GovernanceValues governance = Objects.requireNonNullElseGet(
                request.getGovernance(), ProjectTemplateRenderRequest.GovernanceValues::new);
        ProjectTemplateRenderRequest.ReleaseValues release = Objects.requireNonNullElseGet(request.getRelease(),
                ProjectTemplateRenderRequest.ReleaseValues::new);
        ProjectTemplateRenderRequest.AuthValues auth = Objects.requireNonNullElseGet(request.getAuth(),
                ProjectTemplateRenderRequest.AuthValues::new);
        String projectName = safeName(text(request.getProjectName(), ProjectTemplateConstants.DEFAULT_PROJECT_NAME));
        Integer upstreamPort = value(upstream.getPort(), ProjectTemplateConstants.DEFAULT_HTTP_PORT);
        boolean releaseEnabled = Boolean.TRUE.equals(release.getEnabled()) || Boolean.TRUE.equals(candidate.getEnabled());
        ReleaseStrategy releaseStrategy = value(release.getStrategy(), ReleaseStrategy.TRAFFIC_SPLIT);
        return new TemplateValues(
                text(request.getNamespace(), ResourceMetadataConstants.DEFAULT_NAMESPACE),
                projectName,
                text(request.getDisplayName(), projectName),
                text(request.getOwnerTeam(), null),
                text(request.getEnvironment(), ProjectTemplateConstants.DEFAULT_ENVIRONMENT),
                text(request.getTrafficTier(), ProjectTemplateConstants.DEFAULT_TRAFFIC_TIER),
                text(request.getConfigShard(), ProjectTemplateConstants.DEFAULT_CONFIG_SHARD),
                text(request.getIsolationGroup(), null),
                text(route.getHost(), ProjectTemplateConstants.DEFAULT_HOST),
                normalizePath(text(route.getPath(), ProjectTemplateConstants.DEFAULT_PATH)),
                value(route.getStripPrefix(), true),
                route.getMethods() == null || route.getMethods().isEmpty() ? List.of(HttpMethod.ANY) : route.getMethods(),
                text(upstream.getHost(), ProjectTemplateConstants.DEFAULT_UPSTREAM_HOST),
                upstreamPort,
                value(upstream.getProtocol(), Protocol.HTTP),
                value(upstream.getLoadBalance(), LoadBalanceStrategy.ROUND_ROBIN),
                value(upstream.getHealthCheckEnabled(), true),
                text(upstream.getHealthPath(), ProjectTemplateConstants.DEFAULT_HEALTH_PATH),
                releaseEnabled,
                text(candidate.getHost(), text(upstream.getHost(), ProjectTemplateConstants.DEFAULT_UPSTREAM_HOST)),
                value(candidate.getPort(), upstreamPort),
                value(governance.getRateLimitEnabled(), false),
                value(governance.getRequestsPerSecond(), ProjectTemplateConstants.DEFAULT_REQUESTS_PER_SECOND),
                value(governance.getBurstCapacity(), ProjectTemplateConstants.DEFAULT_BURST_CAPACITY),
                value(governance.getRetryEnabled(), false),
                value(governance.getMaxAttempts(), ProjectTemplateConstants.DEFAULT_MAX_ATTEMPTS),
                releaseStrategy,
                candidateWeight(releaseStrategy, release.getCandidateWeight()),
                text(release.getColorHeader(), ProjectTemplateConstants.DEFAULT_COLOR_HEADER),
                text(release.getCandidateColor(), ProjectTemplateConstants.DEFAULT_CANDIDATE_COLOR),
                value(auth.getType(), AuthType.NONE),
                value(auth.getAnonymousAllowed(), true)
        );
    }

    private String name(String projectName, String suffix) {
        if (!StringUtils.hasText(suffix)) {
            return projectName;
        }
        return projectName + ProjectTemplateConstants.NAME_SEPARATOR + suffix;
    }

    private String safeName(String value) {
        String normalized = value.trim().toLowerCase();
        return ProjectTemplateConstants.SAFE_NAME_PATTERN.matcher(normalized)
                .replaceAll(ProjectTemplateConstants.NAME_SEPARATOR);
    }

    private String normalizePath(String path) {
        if (path.startsWith("/")) {
            return path;
        }
        return "/" + path;
    }

    private String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private <T> T value(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private Integer candidateWeight(ReleaseStrategy releaseStrategy, Integer candidateWeight) {
        if (releaseStrategy == ReleaseStrategy.BLUE_GREEN) {
            // 蓝绿发布是稳定版本到绿色版本的整体切换
            return ProjectTemplateConstants.MAX_TRAFFIC_WEIGHT;
        }
        return value(candidateWeight, ProjectTemplateConstants.DEFAULT_CANDIDATE_WEIGHT);
    }

    private record ResourceEnvelope(ResourceKind kind, String namespace, String name, Object resource) {
    }

    private record ResourceFingerprint(Object spec, Map<String, String> labels) {
    }

    private record TemplateValues(String namespace,
                                  String projectName,
                                  String displayName,
                                  String ownerTeam,
                                  String environment,
                                  String trafficTier,
                                  String configShard,
                                  String isolationGroup,
                                  String host,
                                  String path,
                                  Boolean stripPrefix,
                                  List<HttpMethod> methods,
                                  String upstreamHost,
                                  Integer upstreamPort,
                                  Protocol protocol,
                                  LoadBalanceStrategy loadBalance,
                                  Boolean healthCheckEnabled,
                                  String healthPath,
                                  boolean releaseEnabled,
                                  String candidateHost,
                                  Integer candidatePort,
                                  Boolean rateLimitEnabled,
                                  Integer requestsPerSecond,
                                  Integer burstCapacity,
                                  Boolean retryEnabled,
                                  Integer maxAttempts,
                                  ReleaseStrategy releaseStrategy,
                                  Integer candidateWeight,
                                  String colorHeader,
                                  String candidateColor,
                                  AuthType authType,
                                  Boolean anonymousAllowed) {
    }
}
