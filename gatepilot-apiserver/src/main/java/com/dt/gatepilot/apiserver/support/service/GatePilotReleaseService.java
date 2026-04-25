package com.dt.gatepilot.apiserver.support.service;

import com.dt.gatepilot.api.enums.EventSeverity;
import com.dt.gatepilot.api.enums.ResourceKind;
import com.dt.gatepilot.api.resource.common.ResourceMetadata;
import com.dt.gatepilot.api.resource.common.ResourceReference;
import com.dt.gatepilot.api.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.api.resource.event.GatewayEvent;
import com.dt.gatepilot.apiserver.api.request.ReleaseRequest;
import com.dt.gatepilot.apiserver.api.request.RollbackRequest;
import com.dt.gatepilot.apiserver.api.response.CursorPageResponse;
import com.dt.gatepilot.apiserver.api.response.ReleaseDryRunResponse;
import com.dt.gatepilot.apiserver.api.response.ReleaseResponse;
import com.dt.gatepilot.apiserver.support.resource.GatePilotResourceType;
import com.getboot.exception.api.code.CommonErrorCode;
import com.getboot.exception.api.exception.BusinessException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * 发布请求服务，负责接收发布意图并记录等待 controller-manager reconcile 的事件。
 */
@Service
public class GatePilotReleaseService {

    private final GatePilotResourceService resourceService;

    private final GatePilotConfigSnapshotService snapshotService;

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
    }

    /**
     * 创建发布请求。
     *
     * @param request 发布请求
     * @return 发布响应
     */
    public ReleaseResponse createRelease(ReleaseRequest request) {
        Instant now = Instant.now();
        String releaseId = "rel-" + UUID.randomUUID();
        String version = request.getProjectName() + "-" + now.toEpochMilli();
        GatewayEvent event = buildReleaseEvent(request, releaseId, version, now);
        GatePilotResourceType eventType = resourceService.requireResourceType(ResourceKind.GATEWAY_EVENT);
        resourceService.save(eventType, request.getNamespace(), releaseId, event);

        ReleaseResponse response = new ReleaseResponse();
        response.setReleaseId(releaseId);
        response.setVersion(version);
        response.setPhase("PENDING");
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
    public ReleaseDryRunResponse dryRun(ReleaseRequest request) {
        Instant now = Instant.now();
        ReleaseDryRunResponse response = new ReleaseDryRunResponse();
        response.setNamespace(request.getNamespace());
        response.setProjectName(request.getProjectName());
        response.setVersion(request.getProjectName() + "-dry-run-" + now.toEpochMilli());
        response.setConfigShard(request.getConfigShard());
        response.setCheckedAt(now);
        if (!resourceExists("projects", request.getNamespace(), request.getProjectName())) {
            addDryRunMessage(response, "ERROR", "ProjectNotFound", "项目资源不存在，不能发布");
        }
        CursorPageResponse<Object> routes = resourceService.list("routes", request.getNamespace(), null, 500);
        CursorPageResponse<Object> upstreams = resourceService.list("upstreams", request.getNamespace(), null, 500);
        CursorPageResponse<Object> trafficPolicies =
                resourceService.list("traffic-policies", request.getNamespace(), null, 500);
        CursorPageResponse<Object> releasePolicies =
                resourceService.list("release-policies", request.getNamespace(), null, 500);
        CursorPageResponse<Object> authPolicies =
                resourceService.list("auth-policies", request.getNamespace(), null, 500);
        response.setRouteCount(routes.getItems().size());
        response.setUpstreamCount(upstreams.getItems().size());
        response.setPolicyCount(trafficPolicies.getItems().size()
                + releasePolicies.getItems().size()
                + authPolicies.getItems().size());
        if (response.getRouteCount() == 0) {
            addDryRunMessage(response, "WARN", "NoRoute", "当前命名空间没有路由资源，本次发布不会产生业务入口");
        }
        if (response.getUpstreamCount() == 0) {
            addDryRunMessage(response, "WARN", "NoUpstream", "当前命名空间没有上游资源，路由可能无法转发");
        }
        response.setPassed(response.getMessages().stream().noneMatch(message -> "ERROR".equals(message.getLevel())));
        return response;
    }

    /**
     * 创建回滚请求。
     *
     * @param request 回滚请求
     * @return 发布响应
     */
    public ReleaseResponse createRollback(RollbackRequest request) {
        Instant now = Instant.now();
        GatewayConfigSnapshot snapshot = snapshotService.findSnapshot(
                        request.getNamespace(),
                        request.getProjectName(),
                        request.getTargetVersion(),
                        request.getConfigShard())
                .orElseThrow(() -> BusinessException.of(CommonErrorCode.NOT_FOUND.code(), "目标快照不存在"));
        String releaseId = "rb-" + UUID.randomUUID();
        String version = request.getProjectName() + "-rollback-" + now.toEpochMilli();
        GatewayEvent event = buildRollbackEvent(request, snapshot, releaseId, version, now);
        GatePilotResourceType eventType = resourceService.requireResourceType(ResourceKind.GATEWAY_EVENT);
        resourceService.save(eventType, request.getNamespace(), releaseId, event);
        snapshot.getStatus().setLastRollbackAt(now);
        snapshot.getStatus().setLastRollbackReleaseId(releaseId);
        GatePilotResourceType snapshotType = resourceService.requireResourceType(ResourceKind.CONFIG_SNAPSHOT);
        resourceService.save(snapshotType, request.getNamespace(), snapshot.getMetadata().getName(), snapshot);

        ReleaseResponse response = new ReleaseResponse();
        response.setReleaseId(releaseId);
        response.setVersion(version);
        response.setPhase("PENDING");
        response.setConfigShard(request.getConfigShard());
        response.setCreatedAt(now);
        return response;
    }

    private GatewayEvent buildReleaseEvent(ReleaseRequest request, String releaseId, String version, Instant now) {
        GatewayEvent event = new GatewayEvent();
        ResourceMetadata metadata = event.getMetadata();
        metadata.setName(releaseId);
        metadata.setNamespace(request.getNamespace());
        metadata.getLabels().put("gatepilot.io/event-type", "release-request");
        metadata.getLabels().put("gatepilot.io/reconcile-state", "pending");
        metadata.getLabels().put("gatepilot.io/project", request.getProjectName());
        if (StringUtils.hasText(request.getConfigShard())) {
            metadata.getLabels().put("gatepilot.io/config-shard", request.getConfigShard());
        }
        GatewayEvent.GatewayEventSpec spec = event.getSpec();
        spec.setSeverity(EventSeverity.INFO);
        spec.setSource("apiserver");
        spec.setReason("ReleaseRequested");
        spec.setMessage("发布请求已创建，等待 controller-manager 推进");
        spec.setFirstObservedAt(now);
        spec.setLastObservedAt(now);
        spec.setCount(1);
        ResourceReference involvedObject = new ResourceReference();
        involvedObject.setKind(ResourceKind.GATEWAY_PROJECT);
        involvedObject.setNamespace(request.getNamespace());
        involvedObject.setName(request.getProjectName());
        spec.setInvolvedObject(involvedObject);
        spec.getAttributes().put("releaseId", releaseId);
        spec.getAttributes().put("version", version);
        spec.getAttributes().put("projectName", request.getProjectName());
        spec.getAttributes().put("createdBy", request.getCreatedBy());
        spec.getAttributes().put("description", request.getDescription());
        return event;
    }

    private GatewayEvent buildRollbackEvent(RollbackRequest request,
                                            GatewayConfigSnapshot snapshot,
                                            String releaseId,
                                            String version,
                                            Instant now) {
        GatewayEvent event = new GatewayEvent();
        ResourceMetadata metadata = event.getMetadata();
        metadata.setName(releaseId);
        metadata.setNamespace(request.getNamespace());
        metadata.getLabels().put("gatepilot.io/event-type", "rollback-request");
        metadata.getLabels().put("gatepilot.io/reconcile-state", "pending");
        metadata.getLabels().put("gatepilot.io/project", request.getProjectName());
        metadata.getLabels().put("gatepilot.io/target-version", request.getTargetVersion());
        if (StringUtils.hasText(request.getConfigShard())) {
            metadata.getLabels().put("gatepilot.io/config-shard", request.getConfigShard());
        }
        GatewayEvent.GatewayEventSpec spec = event.getSpec();
        spec.setSeverity(EventSeverity.WARNING);
        spec.setSource("apiserver");
        spec.setReason("RollbackRequested");
        spec.setMessage("回滚请求已创建，等待 controller-manager 推进");
        spec.setFirstObservedAt(now);
        spec.setLastObservedAt(now);
        spec.setCount(1);
        ResourceReference involvedObject = new ResourceReference();
        involvedObject.setKind(ResourceKind.CONFIG_SNAPSHOT);
        involvedObject.setNamespace(request.getNamespace());
        involvedObject.setName(snapshot.getMetadata().getName());
        involvedObject.setUid(snapshot.getMetadata().getUid());
        spec.setInvolvedObject(involvedObject);
        spec.getAttributes().put("releaseId", releaseId);
        spec.getAttributes().put("version", version);
        spec.getAttributes().put("projectName", request.getProjectName());
        spec.getAttributes().put("targetVersion", request.getTargetVersion());
        spec.getAttributes().put("configHash", snapshot.getSpec().getConfigHash());
        spec.getAttributes().put("createdBy", request.getCreatedBy());
        spec.getAttributes().put("description", request.getDescription());
        return event;
    }

    private boolean resourceExists(String resourcePath, String namespace, String name) {
        try {
            resourceService.get(resourcePath, namespace, name);
            return true;
        } catch (ResponseStatusException exception) {
            return false;
        }
    }

    private void addDryRunMessage(ReleaseDryRunResponse response, String level, String reason, String message) {
        ReleaseDryRunResponse.DryRunMessage dryRunMessage = new ReleaseDryRunResponse.DryRunMessage();
        dryRunMessage.setLevel(level);
        dryRunMessage.setReason(reason);
        dryRunMessage.setMessage(message);
        response.getMessages().add(dryRunMessage);
    }
}
