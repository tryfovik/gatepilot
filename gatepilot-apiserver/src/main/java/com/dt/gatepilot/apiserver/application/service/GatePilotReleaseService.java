package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.domain.enums.EventSeverity;
import com.dt.gatepilot.domain.enums.ResourceKind;
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
    public ReleaseResult createRelease(CreateReleaseCommand request) {
        Instant now = Instant.now();
        String releaseId = GatePilotReleaseConstants.RELEASE_ID_PREFIX + UUID.randomUUID();
        String version = request.getProjectName() + GatePilotReleaseConstants.VERSION_SEPARATOR + now.toEpochMilli();
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
        response.setVersion(request.getProjectName()
                + GatePilotReleaseConstants.VERSION_SEPARATOR
                + GatePilotReleaseConstants.DRY_RUN_VERSION_PART
                + GatePilotReleaseConstants.VERSION_SEPARATOR
                + now.toEpochMilli());
        response.setConfigShard(request.getConfigShard());
        response.setCheckedAt(now);
        // dry-run 先确认项目存在，再统计相关资源规模
        if (!resourceExists(GatePilotResourcePaths.PROJECTS, request.getNamespace(), request.getProjectName())) {
            addDryRunMessage(response, GatePilotReleaseConstants.DRY_RUN_LEVEL_ERROR,
                    GatePilotReleaseConstants.REASON_PROJECT_NOT_FOUND, "项目资源不存在，不能发布");
        }
        CursorPage<Object> routes = resourceService.list(GatePilotResourcePaths.ROUTES, request.getNamespace(), null,
                500);
        CursorPage<Object> upstreams = resourceService.list(GatePilotResourcePaths.UPSTREAMS, request.getNamespace(),
                null, 500);
        CursorPage<Object> trafficPolicies =
                resourceService.list(GatePilotResourcePaths.TRAFFIC_POLICIES, request.getNamespace(), null, 500);
        CursorPage<Object> releasePolicies =
                resourceService.list(GatePilotResourcePaths.RELEASE_POLICIES, request.getNamespace(), null, 500);
        CursorPage<Object> authPolicies =
                resourceService.list(GatePilotResourcePaths.AUTH_POLICIES, request.getNamespace(), null, 500);
        response.setRouteCount(routes.getItems().size());
        response.setUpstreamCount(upstreams.getItems().size());
        response.setPolicyCount(trafficPolicies.getItems().size()
                + releasePolicies.getItems().size()
                + authPolicies.getItems().size());
        if (response.getRouteCount() == 0) {
            addDryRunMessage(response, GatePilotReleaseConstants.DRY_RUN_LEVEL_WARN,
                    GatePilotReleaseConstants.REASON_NO_ROUTE, "当前命名空间没有路由资源，本次发布不会产生业务入口");
        }
        if (response.getUpstreamCount() == 0) {
            addDryRunMessage(response, GatePilotReleaseConstants.DRY_RUN_LEVEL_WARN,
                    GatePilotReleaseConstants.REASON_NO_UPSTREAM, "当前命名空间没有上游资源，路由可能无法转发");
        }
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
                .orElseThrow(() -> BusinessException.of(CommonErrorCode.NOT_FOUND.code(), "目标快照不存在"));
        String releaseId = GatePilotReleaseConstants.ROLLBACK_ID_PREFIX + UUID.randomUUID();
        String version = request.getProjectName()
                + GatePilotReleaseConstants.VERSION_SEPARATOR
                + GatePilotReleaseConstants.ROLLBACK_VERSION_PART
                + GatePilotReleaseConstants.VERSION_SEPARATOR
                + now.toEpochMilli();
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
        spec.setMessage("发布请求已创建，等待 controller-manager 推进");
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

    private void addDryRunMessage(ReleaseDryRunResult response, String level, String reason, String message) {
        ReleaseDryRunResult.DryRunMessage dryRunMessage = new ReleaseDryRunResult.DryRunMessage();
        // dry-run 信息面向控制台展示，保留明确的 level 和 reason
        dryRunMessage.setLevel(level);
        dryRunMessage.setReason(reason);
        dryRunMessage.setMessage(message);
        response.getMessages().add(dryRunMessage);
    }
}
