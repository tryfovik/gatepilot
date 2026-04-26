package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.domain.enums.EventSeverity;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.policy.AuthPolicy;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.route.GatewayRoute;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import com.dt.gatepilot.domain.resource.event.GatewayEventConstants;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.domain.resource.event.GatewayEvent;
import com.dt.gatepilot.apiserver.application.command.CreateReleaseCommand;
import com.dt.gatepilot.apiserver.application.command.CreateRollbackCommand;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.application.dto.ReleaseDryRunResult;
import com.dt.gatepilot.apiserver.application.dto.ReleaseResult;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourcePaths;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceType;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.getboot.exception.api.code.CommonErrorCode;
import com.getboot.exception.api.exception.BusinessException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * 发布请求服务，负责接收发布意图并记录等待 controller-manager reconcile 的事件。
 */
@Service
@ConditionalOnGatePilotApiserverEnabled
public class GatePilotReleaseService {

    private final GatePilotResourceService resourceService;

    private final GatePilotConfigSnapshotService snapshotService;

    private final GatePilotReleaseIdentityGenerator identityGenerator;

    /**
     * 创建发布请求服务。
     *
     * @param resourceService 资源服务
     * @param snapshotService 配置快照服务
     */
    public GatePilotReleaseService(GatePilotResourceService resourceService,
                                   GatePilotConfigSnapshotService snapshotService) {
        this.resourceService = resourceService;
        this.snapshotService = snapshotService;
        this.identityGenerator = new GatePilotReleaseIdentityGenerator();
    }

    /**
     * 创建发布请求。
     *
     * @param request 发布请求
     * @return 发布响应
     */
    public ReleaseResult createRelease(CreateReleaseCommand request) {
        Instant now = Instant.now();
        String releaseId = identityGenerator.nextReleaseId();
        String version = identityGenerator.releaseVersion(request.getProjectName(), releaseId);
        GatewayEvent event = buildReleaseEvent(request, releaseId, version, now);
        GatePilotResourceType eventType = resourceService.requireResourceType(ResourceKind.GATEWAY_EVENT);
        // 发布入口只保存事件，具体推进由 controller-manager 异步处理
        resourceService.save(eventType, request.getNamespace(), releaseId, event);

        ReleaseResult response = new ReleaseResult();
        response.setReleaseId(releaseId);
        response.setVersion(version);
        response.setPhase(GatePilotReleaseConstants.PHASE_PENDING);
        response.setConfigShard(request.getConfigShard());
        response.setCreatedAt(now);
        return response;
    }

    /**
     * 发布 dry-run 校验，不写入发布事件。
     *
     * @param request 发布请求
     * @return dry-run 结果
     */
    public ReleaseDryRunResult dryRun(CreateReleaseCommand request) {
        Instant now = Instant.now();
        ReleaseDryRunResult response = new ReleaseDryRunResult();
        response.setNamespace(request.getNamespace());
        response.setProjectName(request.getProjectName());
        response.setVersion(identityGenerator.dryRunVersion(request.getProjectName()));
        response.setConfigShard(request.getConfigShard());
        response.setCheckedAt(now);
        // dry-run 先确认项目存在，再统计项目相关资源规模
        if (!resourceExists(GatePilotResourcePaths.PROJECTS, request.getNamespace(), request.getProjectName())) {
            addDryRunMessage(response, GatePilotReleaseConstants.DRY_RUN_LEVEL_ERROR,
                    GatePilotReleaseConstants.REASON_PROJECT_NOT_FOUND, GatePilotReleaseConstants.MESSAGE_PROJECT_NOT_FOUND);
        }
        List<GatewayRoute> routes = projectRoutes(request);
        List<Upstream> upstreams = projectUpstreams(request);
        List<TrafficPolicy> trafficPolicies = projectTrafficPolicies(request);
        List<ReleasePolicy> releasePolicies = projectReleasePolicies(request);
        List<AuthPolicy> authPolicies = projectAuthPolicies(request);
        response.setRouteCount(routes.size());
        response.setUpstreamCount(upstreams.size());
        response.setPolicyCount(trafficPolicies.size() + releasePolicies.size() + authPolicies.size());
        if (response.getRouteCount() == 0) {
            addDryRunMessage(response, GatePilotReleaseConstants.DRY_RUN_LEVEL_WARN,
                    GatePilotReleaseConstants.REASON_NO_ROUTE, GatePilotReleaseConstants.MESSAGE_NO_ROUTE);
        }
        if (response.getUpstreamCount() == 0) {
            addDryRunMessage(response, GatePilotReleaseConstants.DRY_RUN_LEVEL_WARN,
                    GatePilotReleaseConstants.REASON_NO_UPSTREAM, GatePilotReleaseConstants.MESSAGE_NO_UPSTREAM);
        }
        validateRoutes(response, routes, upstreams, trafficPolicies, releasePolicies, authPolicies);
        validateUpstreams(response, upstreams);
        validateReleasePolicies(response, releasePolicies, upstreams);
        response.setPassed(response.getMessages().stream().noneMatch(message ->
                GatePilotReleaseConstants.DRY_RUN_LEVEL_ERROR.equals(message.getLevel())));
        return response;
    }

    /**
     * 创建回滚请求。
     *
     * @param request 回滚请求
     * @return 发布响应
     */
    public ReleaseResult createRollback(CreateRollbackCommand request) {
        Instant now = Instant.now();
        GatewayConfigSnapshot snapshot = snapshotService.findSnapshot(
                        request.getNamespace(),
                        request.getProjectName(),
                        request.getTargetVersion(),
                        request.getConfigShard())
                .orElseThrow(() -> BusinessException.of(CommonErrorCode.NOT_FOUND.code(),
                        GatePilotReleaseConstants.MESSAGE_TARGET_SNAPSHOT_NOT_FOUND));
        String releaseId = identityGenerator.nextRollbackId();
        String version = identityGenerator.rollbackVersion(request.getProjectName(), releaseId);
        GatewayEvent event = buildRollbackEvent(request, snapshot, releaseId, version, now);
        GatePilotResourceType eventType = resourceService.requireResourceType(ResourceKind.GATEWAY_EVENT);
        // 回滚同样只生成发布意图，避免 apiserver 同步推整条链路
        resourceService.save(eventType, request.getNamespace(), releaseId, event);
        snapshot.getStatus().setLastRollbackAt(now);
        snapshot.getStatus().setLastRollbackReleaseId(releaseId);
        GatePilotResourceType snapshotType = resourceService.requireResourceType(ResourceKind.CONFIG_SNAPSHOT);
        resourceService.save(snapshotType, request.getNamespace(), snapshot.getMetadata().getName(), snapshot);

        ReleaseResult response = new ReleaseResult();
        response.setReleaseId(releaseId);
        response.setVersion(version);
        response.setPhase(GatePilotReleaseConstants.PHASE_PENDING);
        response.setConfigShard(request.getConfigShard());
        response.setCreatedAt(now);
        return response;
    }

    private GatewayEvent buildReleaseEvent(CreateReleaseCommand request, String releaseId, String version, Instant now) {
        GatewayEvent event = new GatewayEvent();
        ResourceMetadata metadata = event.getMetadata();
        metadata.setName(releaseId);
        metadata.setNamespace(request.getNamespace());
        // label 用于 controller-manager 高效筛选待处理事件
        metadata.getLabels().put(ResourceMetadataConstants.LABEL_EVENT_TYPE,
                GatewayEventConstants.EVENT_TYPE_RELEASE_REQUEST);
        metadata.getLabels().put(ResourceMetadataConstants.LABEL_RECONCILE_STATE,
                GatewayEventConstants.RECONCILE_STATE_PENDING);
        metadata.getLabels().put(ResourceMetadataConstants.LABEL_PROJECT, request.getProjectName());
        if (StringUtils.hasText(request.getConfigShard())) {
            metadata.getLabels().put(ResourceMetadataConstants.LABEL_CONFIG_SHARD, request.getConfigShard());
        }
        GatewayEvent.GatewayEventSpec spec = event.getSpec();
        spec.setSeverity(EventSeverity.INFO);
        spec.setSource(GatewayEventConstants.SOURCE_APISERVER);
        spec.setReason(GatewayEventConstants.REASON_CREATE_RELEASE_COMMANDED);
        spec.setMessage(GatePilotReleaseConstants.MESSAGE_RELEASE_REQUEST_CREATED);
        spec.setFirstObservedAt(now);
        spec.setLastObservedAt(now);
        spec.setCount(1);
        ResourceReference involvedObject = new ResourceReference();
        involvedObject.setKind(ResourceKind.GATEWAY_PROJECT);
        involvedObject.setNamespace(request.getNamespace());
        involvedObject.setName(request.getProjectName());
        spec.setInvolvedObject(involvedObject);
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_RELEASE_ID, releaseId);
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_VERSION, version);
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_PROJECT_NAME, request.getProjectName());
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_CREATED_BY, request.getCreatedBy());
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_DESCRIPTION, request.getDescription());
        return event;
    }

    private GatewayEvent buildRollbackEvent(CreateRollbackCommand request,
                                            GatewayConfigSnapshot snapshot,
                                            String releaseId,
                                            String version,
                                            Instant now) {
        GatewayEvent event = new GatewayEvent();
        ResourceMetadata metadata = event.getMetadata();
        metadata.setName(releaseId);
        metadata.setNamespace(request.getNamespace());
        // 回滚事件也走同一套 reconcile label
        metadata.getLabels().put(ResourceMetadataConstants.LABEL_EVENT_TYPE,
                GatewayEventConstants.EVENT_TYPE_ROLLBACK_REQUEST);
        metadata.getLabels().put(ResourceMetadataConstants.LABEL_RECONCILE_STATE,
                GatewayEventConstants.RECONCILE_STATE_PENDING);
        metadata.getLabels().put(ResourceMetadataConstants.LABEL_PROJECT, request.getProjectName());
        metadata.getLabels().put(ResourceMetadataConstants.LABEL_TARGET_VERSION, request.getTargetVersion());
        if (StringUtils.hasText(request.getConfigShard())) {
            metadata.getLabels().put(ResourceMetadataConstants.LABEL_CONFIG_SHARD, request.getConfigShard());
        }
        GatewayEvent.GatewayEventSpec spec = event.getSpec();
        spec.setSeverity(EventSeverity.WARNING);
        spec.setSource(GatewayEventConstants.SOURCE_APISERVER);
        spec.setReason(GatewayEventConstants.REASON_CREATE_ROLLBACK_COMMANDED);
        spec.setMessage(GatePilotReleaseConstants.MESSAGE_ROLLBACK_REQUEST_CREATED);
        spec.setFirstObservedAt(now);
        spec.setLastObservedAt(now);
        spec.setCount(1);
        ResourceReference involvedObject = new ResourceReference();
        involvedObject.setKind(ResourceKind.CONFIG_SNAPSHOT);
        involvedObject.setNamespace(request.getNamespace());
        involvedObject.setName(snapshot.getMetadata().getName());
        involvedObject.setUid(snapshot.getMetadata().getUid());
        spec.setInvolvedObject(involvedObject);
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_RELEASE_ID, releaseId);
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_VERSION, version);
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_PROJECT_NAME, request.getProjectName());
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_TARGET_VERSION, request.getTargetVersion());
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_CONFIG_HASH, snapshot.getSpec().getConfigHash());
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_CREATED_BY, request.getCreatedBy());
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_DESCRIPTION, request.getDescription());
        return event;
    }

    private boolean resourceExists(String resourcePath, String namespace, String name) {
        try {
            // 复用资源服务判断存在性，避免绕过统一存储端口
            resourceService.get(resourcePath, namespace, name);
            return true;
        } catch (ResponseStatusException exception) {
            return false;
        }
    }

    private List<GatewayRoute> projectRoutes(CreateReleaseCommand request) {
        return listAll(GatePilotResourcePaths.ROUTES, request.getNamespace(), GatewayRoute.class).stream()
                .filter(route -> projectMatches(route.getSpec().getProjectRef(),
                        route.getMetadata().getLabels().get(ResourceMetadataConstants.LABEL_PROJECT),
                        request))
                .toList();
    }

    private List<Upstream> projectUpstreams(CreateReleaseCommand request) {
        return listAll(GatePilotResourcePaths.UPSTREAMS, request.getNamespace(), Upstream.class).stream()
                .filter(upstream -> projectMatches(upstream.getSpec().getProjectRef(),
                        upstream.getMetadata().getLabels().get(ResourceMetadataConstants.LABEL_PROJECT),
                        request))
                .toList();
    }

    private List<TrafficPolicy> projectTrafficPolicies(CreateReleaseCommand request) {
        return listAll(GatePilotResourcePaths.TRAFFIC_POLICIES, request.getNamespace(), TrafficPolicy.class).stream()
                .filter(policy -> projectMatches(policy.getSpec().getProjectRef(),
                        policy.getMetadata().getLabels().get(ResourceMetadataConstants.LABEL_PROJECT),
                        request))
                .toList();
    }

    private List<ReleasePolicy> projectReleasePolicies(CreateReleaseCommand request) {
        return listAll(GatePilotResourcePaths.RELEASE_POLICIES, request.getNamespace(), ReleasePolicy.class).stream()
                .filter(policy -> projectMatches(policy.getSpec().getProjectRef(),
                        policy.getMetadata().getLabels().get(ResourceMetadataConstants.LABEL_PROJECT),
                        request))
                .toList();
    }

    private List<AuthPolicy> projectAuthPolicies(CreateReleaseCommand request) {
        return listAll(GatePilotResourcePaths.AUTH_POLICIES, request.getNamespace(), AuthPolicy.class).stream()
                .filter(policy -> projectMatches(policy.getSpec().getProjectRef(),
                        policy.getMetadata().getLabels().get(ResourceMetadataConstants.LABEL_PROJECT),
                        request))
                .toList();
    }

    private boolean projectMatches(ResourceReference projectRef, String projectLabel, CreateReleaseCommand request) {
        if (projectRef != null && StringUtils.hasText(projectRef.getName())) {
            // projectRef 是主判断依据，label 只做兼容
            return Objects.equals(projectRef.getName(), request.getProjectName())
                    && (!StringUtils.hasText(projectRef.getNamespace())
                    || Objects.equals(projectRef.getNamespace(), request.getNamespace()));
        }
        return Objects.equals(projectLabel, request.getProjectName());
    }

    private void validateRoutes(ReleaseDryRunResult response,
                                List<GatewayRoute> routes,
                                List<Upstream> upstreams,
                                List<TrafficPolicy> trafficPolicies,
                                List<ReleasePolicy> releasePolicies,
                                List<AuthPolicy> authPolicies) {
        Set<String> upstreamNames = names(upstreams);
        Set<String> policyNames = policyNames(trafficPolicies, releasePolicies, authPolicies);
        for (GatewayRoute route : routes) {
            validateRoutePath(response, route);
            validateRouteHosts(response, route);
            validateRouteUpstream(response, route, upstreamNames);
            validateRoutePolicies(response, route, policyNames);
        }
    }

    private void validateRoutePath(ReleaseDryRunResult response, GatewayRoute route) {
        String path = route.getSpec().getPath() == null ? null : route.getSpec().getPath().getValue();
        if (!StringUtils.hasText(path)) {
            addDryRunMessage(response, GatePilotReleaseConstants.DRY_RUN_LEVEL_ERROR,
                    GatePilotReleaseConstants.REASON_ROUTE_PATH_MISSING,
                    GatePilotReleaseConstants.MESSAGE_ROUTE_PATH_MISSING_PREFIX + route.getMetadata().getName());
        }
    }

    private void validateRouteHosts(ReleaseDryRunResult response, GatewayRoute route) {
        if (route.getSpec().getHosts().isEmpty()) {
            addDryRunMessage(response, GatePilotReleaseConstants.DRY_RUN_LEVEL_WARN,
                    GatePilotReleaseConstants.REASON_ROUTE_HOST_MISSING,
                    GatePilotReleaseConstants.MESSAGE_ROUTE_HOST_MISSING_PREFIX + route.getMetadata().getName());
        }
    }

    private void validateRouteUpstream(ReleaseDryRunResult response, GatewayRoute route, Set<String> upstreamNames) {
        ResourceReference upstreamRef = route.getSpec().getUpstreamRef();
        String upstreamName = upstreamRef == null ? null : upstreamRef.getName();
        if (!StringUtils.hasText(upstreamName) || !upstreamNames.contains(upstreamName)) {
            addDryRunMessage(response, GatePilotReleaseConstants.DRY_RUN_LEVEL_ERROR,
                    GatePilotReleaseConstants.REASON_ROUTE_UPSTREAM_MISSING,
                    GatePilotReleaseConstants.MESSAGE_ROUTE_UPSTREAM_MISSING_PREFIX
                            + route.getMetadata().getName()
                            + GatePilotReleaseConstants.REFERENCE_SEPARATOR
                            + Objects.toString(upstreamName, GatePilotReleaseConstants.EMPTY_REFERENCE_VALUE));
        }
    }

    private void validateRoutePolicies(ReleaseDryRunResult response, GatewayRoute route, Set<String> policyNames) {
        for (ResourceReference policyRef : route.getSpec().getPolicyRefs()) {
            if (policyRef != null && StringUtils.hasText(policyRef.getName())
                    && !policyNames.contains(policyRef.getName())) {
                addDryRunMessage(response, GatePilotReleaseConstants.DRY_RUN_LEVEL_ERROR,
                        GatePilotReleaseConstants.REASON_ROUTE_POLICY_MISSING,
                        GatePilotReleaseConstants.MESSAGE_ROUTE_POLICY_MISSING_PREFIX
                                + route.getMetadata().getName()
                                + GatePilotReleaseConstants.REFERENCE_SEPARATOR
                                + policyRef.getName());
            }
        }
    }

    private void validateUpstreams(ReleaseDryRunResult response, List<Upstream> upstreams) {
        for (Upstream upstream : upstreams) {
            if (upstream.getSpec().getEndpoints().isEmpty()) {
                addDryRunMessage(response, GatePilotReleaseConstants.DRY_RUN_LEVEL_ERROR,
                        GatePilotReleaseConstants.REASON_UPSTREAM_ENDPOINT_MISSING,
                        GatePilotReleaseConstants.MESSAGE_UPSTREAM_ENDPOINT_MISSING_PREFIX
                                + upstream.getMetadata().getName());
            }
        }
    }

    private void validateReleasePolicies(ReleaseDryRunResult response,
                                         List<ReleasePolicy> releasePolicies,
                                         List<Upstream> upstreams) {
        Set<String> upstreamNames = names(upstreams);
        for (ReleasePolicy policy : releasePolicies) {
            validateReleaseUpstreamRef(response, policy, upstreamNames, policy.getSpec().getStableUpstreamRef());
            validateReleaseUpstreamRef(response, policy, upstreamNames, policy.getSpec().getCandidateUpstreamRef());
            for (ReleasePolicy.TrafficSplit split : policy.getSpec().getTrafficSplits()) {
                validateReleaseUpstreamRef(response, policy, upstreamNames, split.getUpstreamRef());
            }
        }
    }

    private void validateReleaseUpstreamRef(ReleaseDryRunResult response,
                                            ReleasePolicy policy,
                                            Set<String> upstreamNames,
                                            ResourceReference upstreamRef) {
        if (upstreamRef != null && StringUtils.hasText(upstreamRef.getName())
                && !upstreamNames.contains(upstreamRef.getName())) {
            addDryRunMessage(response, GatePilotReleaseConstants.DRY_RUN_LEVEL_ERROR,
                    GatePilotReleaseConstants.REASON_RELEASE_UPSTREAM_MISSING,
                    GatePilotReleaseConstants.MESSAGE_RELEASE_UPSTREAM_MISSING_PREFIX
                            + policy.getMetadata().getName()
                            + GatePilotReleaseConstants.REFERENCE_SEPARATOR
                            + upstreamRef.getName());
        }
    }

    private <T> List<T> listAll(String resourcePath, String namespace, Class<T> resourceType) {
        List<T> resources = new ArrayList<>();
        String cursor = null;
        do {
            CursorPage<Object> page = resourceService.list(resourcePath, namespace, cursor,
                    GatePilotReleaseConstants.DRY_RUN_LOOKUP_LIMIT);
            resources.addAll(page.getItems()
                    .stream()
                    .map(resourceType::cast)
                    .toList());
            cursor = page.getNextCursor();
        } while (StringUtils.hasText(cursor));
        return resources;
    }

    private Set<String> names(List<Upstream> upstreams) {
        Set<String> names = new HashSet<>();
        for (Upstream upstream : upstreams) {
            // 上游名称来自 metadata，是发布产物里的稳定 key
            names.add(upstream.getMetadata().getName());
        }
        return names;
    }

    private Set<String> policyNames(List<TrafficPolicy> trafficPolicies,
                                    List<ReleasePolicy> releasePolicies,
                                    List<AuthPolicy> authPolicies) {
        Set<String> names = new HashSet<>();
        trafficPolicies.forEach(policy -> names.add(policy.getMetadata().getName()));
        releasePolicies.forEach(policy -> names.add(policy.getMetadata().getName()));
        authPolicies.forEach(policy -> names.add(policy.getMetadata().getName()));
        return names;
    }

    private void addDryRunMessage(ReleaseDryRunResult response, String level, String reason, String message) {
        ReleaseDryRunResult.DryRunMessage dryRunMessage = new ReleaseDryRunResult.DryRunMessage();
        // dry-run 信息面向控制台展示，保留明确的 level 和 reason
        dryRunMessage.setLevel(level);
        dryRunMessage.setReason(reason);
        dryRunMessage.setMessage(message);
        response.getMessages().add(dryRunMessage);
    }

}
