package com.dt.gatepilot.embedded.infrastructure.controller;

import com.dt.gatepilot.domain.enums.EventSeverity;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.event.GatewayEventConstants;
import com.dt.gatepilot.domain.resource.meta.LabelSelector;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.domain.resource.event.GatewayEvent;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.policy.AuthPolicy;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.project.GatewayProject;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.route.GatewayRoute;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourcePaths;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceType;
import com.dt.gatepilot.apiserver.application.service.GatePilotConfigSnapshotService;
import com.dt.gatepilot.apiserver.application.service.GatePilotResourceService;
import com.dt.gatepilot.controller.application.command.ReconcileResult;
import com.dt.gatepilot.controller.domain.port.GatewayDesiredStateReader;
import com.dt.gatepilot.controller.domain.port.PublishedConfigStatusStore;
import com.dt.gatepilot.controller.domain.port.ReconcileResultSink;
import com.dt.gatepilot.controller.domain.port.ReleaseIntentSource;
import com.dt.gatepilot.controller.domain.port.RollbackConfigReader;
import com.dt.gatepilot.controller.domain.model.GatewayDesiredState;
import com.dt.gatepilot.controller.domain.model.ReleaseIntent;
import com.dt.gatepilot.domain.deployment.GatePilotDeploymentModeConstants;
import com.getboot.exception.api.code.CommonErrorCode;
import com.getboot.exception.api.exception.BusinessException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * controller-manager 使用的 apiserver 进程内资源适配器。
 */
@Component
@ConditionalOnProperty(prefix = GatePilotDeploymentModeConstants.CONFIG_PREFIX,
        name = GatePilotDeploymentModeConstants.MODE_PROPERTY,
        havingValue = GatePilotDeploymentModeConstants.MODE_STANDALONE)
public class GatePilotControllerResourceAdapter
        implements ReleaseIntentSource, GatewayDesiredStateReader, ReconcileResultSink, RollbackConfigReader,
        PublishedConfigStatusStore {

    private final GatePilotResourceService resourceService;

    private final GatePilotConfigSnapshotService snapshotService;

    /**
     * 创建 controller-manager 资源适配器。
     *
     * @param resourceService 资源服务
     * @param snapshotService 配置快照服务
     */
    public GatePilotControllerResourceAdapter(GatePilotResourceService resourceService,
                                              GatePilotConfigSnapshotService snapshotService) {
        this.resourceService = resourceService;
        this.snapshotService = snapshotService;
    }

    @Override
    public List<ReleaseIntent> listPending(int limit) {
        int effectiveLimit = Math.min(limit, EmbeddedAdapterConstants.RESOURCE_PAGE_LIMIT);
        // controller 每轮只领取有限数量，避免单副本长时间占用
        return list(GatePilotResourcePaths.EVENTS, null, GatewayEvent.class)
                .stream()
                .filter(this::isPendingReleaseIntent)
                .limit(effectiveLimit)
                .map(this::toReleaseIntent)
                .toList();
    }

    @Override
    public boolean claim(ReleaseIntent intent, String controllerId) {
        GatewayEvent event = loadSourceEvent(intent);
        if (!isPendingReleaseIntent(event)) {
            return false;
        }
        // claim 通过事件标签表达，后续数据库乐观锁会继续加强
        event.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_RECONCILE_STATE,
                GatewayEventConstants.RECONCILE_STATE_PROCESSING);
        event.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_RECONCILE_CONTROLLER, controllerId);
        event.getSpec().getAttributes().put(GatewayEventConstants.ATTRIBUTE_CLAIMED_BY, controllerId);
        event.getSpec().getAttributes().put(GatewayEventConstants.ATTRIBUTE_CLAIMED_AT, Instant.now().toString());
        event.getSpec().setLastObservedAt(Instant.now());
        intent.setSequence(nextSequence(intent.getNamespace(), intent.getConfigShard()));
        if (!trySaveClaimedEvent(event)) {
            return false;
        }
        return true;
    }

    @Override
    public void markCompleted(ReleaseIntent intent, ReconcileResult result) {
        GatewayEvent event = loadSourceEvent(intent);
        PublishedConfig publishedConfig = result.getPublishedConfig();
        // reconcile 成功后把版本写回原始事件，控制台直接查事件即可展示进度
        event.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_RECONCILE_STATE,
                GatewayEventConstants.RECONCILE_STATE_COMPLETED);
        event.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_VERSION,
                publishedConfig.getSpec().getVersion());
        event.getSpec().setReason(completedReason(intent));
        event.getSpec().setMessage(EmbeddedAdapterConstants.MESSAGE_RECONCILE_COMPLETED);
        event.getSpec().setSeverity(EventSeverity.INFO);
        event.getSpec().setLastObservedAt(Instant.now());
        event.getSpec().getAttributes().put(GatewayEventConstants.ATTRIBUTE_PUBLISHED_CONFIG_NAME,
                publishedConfig.getMetadata().getName());
        event.getSpec().getAttributes().put(GatewayEventConstants.ATTRIBUTE_CONFIG_HASH,
                publishedConfig.getSpec().getConfigHash());
        event.getSpec().getAttributes().put(GatewayEventConstants.ATTRIBUTE_COMPLETED_AT, Instant.now().toString());
        saveEvent(event);
    }

    @Override
    public void markFailed(ReleaseIntent intent, String reason, String message) {
        GatewayEvent event = loadSourceEvent(intent);
        // 失败原因保留在事件上，方便控制台按发布单追踪
        event.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_RECONCILE_STATE,
                GatewayEventConstants.RECONCILE_STATE_FAILED);
        event.getSpec().setReason(reason);
        event.getSpec().setMessage(message);
        event.getSpec().setSeverity(EventSeverity.ERROR);
        event.getSpec().setLastObservedAt(Instant.now());
        event.getSpec().getAttributes().put(GatewayEventConstants.ATTRIBUTE_FAILED_AT, Instant.now().toString());
        saveEvent(event);
    }

    @Override
    public GatewayDesiredState read(ReleaseIntent intent) {
        GatewayProject project = (GatewayProject) resourceService.get(
                GatePilotResourcePaths.PROJECTS,
                intent.getNamespace(),
                intent.getProjectName()
        );
        List<String> runtimeProjectNames = runtimeProjects(intent, project)
                .stream()
                .map(item -> item.getMetadata().getName())
                .toList();
        GatewayDesiredState desiredState = new GatewayDesiredState();
        desiredState.setProject(project);
        desiredState.setRoutes(projectRoutes(intent.getNamespace(), runtimeProjectNames));
        desiredState.setUpstreams(projectUpstreams(intent.getNamespace(), runtimeProjectNames));
        desiredState.setTrafficPolicies(projectTrafficPolicies(intent.getNamespace(), runtimeProjectNames));
        desiredState.setReleasePolicies(projectReleasePolicies(intent.getNamespace(), runtimeProjectNames));
        desiredState.setAuthPolicies(projectAuthPolicies(intent.getNamespace(), runtimeProjectNames));
        desiredState.setTargetNodes(targetNodes(intent, project));
        return desiredState;
    }

    @Override
    public PublishedConfig readRollbackConfig(ReleaseIntent intent) {
        if (!StringUtils.hasText(intent.getTargetVersion())) {
            throw new IllegalStateException(EmbeddedAdapterConstants.MESSAGE_ROLLBACK_TARGET_VERSION_REQUIRED);
        }
        // 回滚配置来自已保存快照，不读取当前草稿资源
        GatewayConfigSnapshot snapshot = snapshotService.findSnapshot(intent.getNamespace(), intent.getProjectName(),
                        intent.getTargetVersion(), intent.getConfigShard())
                .orElseThrow(() -> new IllegalStateException(EmbeddedAdapterConstants.MESSAGE_ROLLBACK_SNAPSHOT_MISSING));
        if (StringUtils.hasText(intent.getTargetConfigHash())
                && !Objects.equals(intent.getTargetConfigHash(), snapshot.getSpec().getConfigHash())) {
            throw new IllegalStateException(EmbeddedAdapterConstants.MESSAGE_ROLLBACK_CONFIG_HASH_MISMATCH);
        }
        PublishedConfig publishedConfig = snapshot.getSpec().getPublishedConfig();
        if (publishedConfig == null) {
            throw new IllegalStateException(EmbeddedAdapterConstants.MESSAGE_ROLLBACK_CONFIG_MISSING);
        }
        return publishedConfig;
    }

    @Override
    public void save(ReleaseIntent intent, ReconcileResult result) {
        PublishedConfig publishedConfig = result.getPublishedConfig();
        GatePilotResourceType publishedConfigType = resourceService.requireResourceType(ResourceKind.PUBLISHED_CONFIG);
        resourceService.save(publishedConfigType, intent.getNamespace(), publishedConfig.getMetadata().getName(),
                publishedConfig);
        snapshotService.saveSnapshot(publishedConfig, intent.getReleaseId(), intent.getRequestedBy(),
                intent.getDescription());
        if (result.getEvent() != null) {
            saveEvent(result.getEvent());
        }
    }

    @Override
    public List<PublishedConfig> listPublishedConfigs(int limit) {
        int effectiveLimit = Math.min(limit, EmbeddedAdapterConstants.RESOURCE_PAGE_LIMIT);
        CursorPage<Object> page = resourceService.list(GatePilotResourcePaths.PUBLISHED_CONFIGS, null, null,
                effectiveLimit);
        return page.getItems()
                .stream()
                .map(PublishedConfig.class::cast)
                .toList();
    }

    @Override
    public List<GatewayNode> listTargetNodes(PublishedConfig publishedConfig) {
        return list(GatePilotResourcePaths.NODES, publishedConfig.getMetadata().getNamespace(), GatewayNode.class)
                .stream()
                .filter(node -> shardMatches(node, publishedConfig.getSpec().getConfigShard()))
                .filter(node -> isolationGroupMatches(node, publishedConfig.getSpec().getIsolationGroup()))
                .toList();
    }

    @Override
    public void saveStatus(PublishedConfig publishedConfig) {
        GatePilotResourceType publishedConfigType = resourceService.requireResourceType(ResourceKind.PUBLISHED_CONFIG);
        resourceService.save(publishedConfigType, publishedConfig.getMetadata().getNamespace(),
                publishedConfig.getMetadata().getName(), publishedConfig);
    }

    private List<GatewayProject> runtimeProjects(ReleaseIntent intent, GatewayProject targetProject) {
        return list(GatePilotResourcePaths.PROJECTS, intent.getNamespace(), GatewayProject.class)
                .stream()
                .filter(project -> shardEquals(project.getSpec().getConfigShard(), intent.getConfigShard()))
                .filter(project -> isolationGroupEquals(project.getSpec().getIsolationGroup(),
                        targetProject.getSpec().getIsolationGroup()))
                .toList();
    }

    private List<GatewayRoute> projectRoutes(String namespace, List<String> projectNames) {
        return list(GatePilotResourcePaths.ROUTES, namespace, GatewayRoute.class)
                .stream()
                .filter(route -> projectMatches(route.getSpec().getProjectRef(), projectNames))
                .toList();
    }

    private List<Upstream> projectUpstreams(String namespace, List<String> projectNames) {
        return list(GatePilotResourcePaths.UPSTREAMS, namespace, Upstream.class)
                .stream()
                .filter(upstream -> projectMatches(upstream.getSpec().getProjectRef(), projectNames))
                .toList();
    }

    private List<TrafficPolicy> projectTrafficPolicies(String namespace, List<String> projectNames) {
        return list(GatePilotResourcePaths.TRAFFIC_POLICIES, namespace, TrafficPolicy.class)
                .stream()
                .filter(policy -> projectMatches(policy.getSpec().getProjectRef(), projectNames))
                .toList();
    }

    private List<ReleasePolicy> projectReleasePolicies(String namespace, List<String> projectNames) {
        return list(GatePilotResourcePaths.RELEASE_POLICIES, namespace, ReleasePolicy.class)
                .stream()
                .filter(policy -> projectMatches(policy.getSpec().getProjectRef(), projectNames))
                .toList();
    }

    private List<AuthPolicy> projectAuthPolicies(String namespace, List<String> projectNames) {
        return list(GatePilotResourcePaths.AUTH_POLICIES, namespace, AuthPolicy.class)
                .stream()
                .filter(policy -> projectMatches(policy.getSpec().getProjectRef(), projectNames))
                .toList();
    }

    private List<GatewayNode> targetNodes(ReleaseIntent intent, GatewayProject project) {
        return list(GatePilotResourcePaths.NODES, intent.getNamespace(), GatewayNode.class)
                .stream()
                .filter(node -> shardMatches(node, intent.getConfigShard()))
                .filter(node -> isolationGroupMatches(node, project))
                .filter(node -> projectSelectorMatches(node, project))
                .toList();
    }

    private <T> List<T> list(String resourcePath, String namespace, Class<T> resourceType) {
        List<T> resources = new ArrayList<>();
        String cursor = null;
        do {
            CursorPage<Object> page = resourceService.list(resourcePath, namespace, cursor,
                    EmbeddedAdapterConstants.RESOURCE_PAGE_LIMIT);
            // adapter 只做分页读取和类型转换，不在这里改写资源内容
            page.getItems()
                    .stream()
                    .map(resourceType::cast)
                    .forEach(resources::add);
            cursor = page.getNextCursor();
        } while (StringUtils.hasText(cursor));
        return resources;
    }

    private boolean isPendingReleaseIntent(GatewayEvent event) {
        Map<String, String> labels = event.getMetadata().getLabels();
        return isReleaseEventType(labels.get(ResourceMetadataConstants.LABEL_EVENT_TYPE))
                && GatewayEventConstants.RECONCILE_STATE_PENDING.equals(
                labels.get(ResourceMetadataConstants.LABEL_RECONCILE_STATE));
    }

    private boolean isReleaseEventType(String eventType) {
        return GatewayEventConstants.EVENT_TYPE_RELEASE_REQUEST.equals(eventType)
                || GatewayEventConstants.EVENT_TYPE_ROLLBACK_REQUEST.equals(eventType);
    }

    private ReleaseIntent toReleaseIntent(GatewayEvent event) {
        Map<String, String> attributes = event.getSpec().getAttributes();
        ReleaseIntent intent = new ReleaseIntent();
        // ReleaseIntent 是 controller-manager 的内部发布意图视图
        intent.setReleaseId(attributes.get(GatewayEventConstants.ATTRIBUTE_RELEASE_ID));
        intent.setNamespace(event.getMetadata().getNamespace());
        intent.setProjectName(attributes.get(GatewayEventConstants.ATTRIBUTE_PROJECT_NAME));
        if (!StringUtils.hasText(intent.getProjectName()) && event.getSpec().getInvolvedObject() != null) {
            intent.setProjectName(event.getSpec().getInvolvedObject().getName());
        }
        intent.setVersion(attributes.get(GatewayEventConstants.ATTRIBUTE_VERSION));
        intent.setTargetVersion(targetVersion(event));
        intent.setTargetConfigHash(attributes.get(GatewayEventConstants.ATTRIBUTE_CONFIG_HASH));
        intent.setConfigShard(event.getMetadata().getLabels().get(ResourceMetadataConstants.LABEL_CONFIG_SHARD));
        intent.setTrigger(trigger(event));
        intent.setRequestedBy(attributes.get(GatewayEventConstants.ATTRIBUTE_CREATED_BY));
        intent.setDescription(attributes.get(GatewayEventConstants.ATTRIBUTE_DESCRIPTION));
        intent.setRequestedAt(event.getSpec().getFirstObservedAt());
        intent.setSourceEventName(event.getMetadata().getName());
        return intent;
    }

    private String targetVersion(GatewayEvent event) {
        String attributeValue = event.getSpec().getAttributes().get(GatewayEventConstants.ATTRIBUTE_TARGET_VERSION);
        if (StringUtils.hasText(attributeValue)) {
            return attributeValue;
        }
        return event.getMetadata().getLabels().get(ResourceMetadataConstants.LABEL_TARGET_VERSION);
    }

    private String trigger(GatewayEvent event) {
        String eventType = event.getMetadata().getLabels().get(ResourceMetadataConstants.LABEL_EVENT_TYPE);
        if (GatewayEventConstants.EVENT_TYPE_ROLLBACK_REQUEST.equals(eventType)) {
            return GatewayEventConstants.TRIGGER_ROLLBACK;
        }
        return GatewayEventConstants.TRIGGER_PUBLISH;
    }

    private String completedReason(ReleaseIntent intent) {
        if (GatewayEventConstants.TRIGGER_ROLLBACK.equals(intent.getTrigger())) {
            return GatewayEventConstants.REASON_ROLLBACK_RECONCILED;
        }
        return GatewayEventConstants.REASON_RELEASE_RECONCILED;
    }

    private long nextSequence(String namespace, String configShard) {
        return list(GatePilotResourcePaths.PUBLISHED_CONFIGS, namespace, PublishedConfig.class)
                .stream()
                .filter(config -> shardEquals(config.getSpec().getConfigShard(), configShard))
                .map(PublishedConfig::getSpec)
                .map(PublishedConfig.PublishedConfigSpec::getSequence)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private boolean projectMatches(ResourceReference reference, String projectName) {
        return reference != null && Objects.equals(reference.getName(), projectName);
    }

    private boolean projectMatches(ResourceReference reference, List<String> projectNames) {
        return reference != null && projectNames.contains(reference.getName());
    }

    private boolean shardMatches(GatewayNode node, String configShard) {
        if (!StringUtils.hasText(configShard)) {
            return true;
        }
        return node.getSpec().getConfigShards().isEmpty()
                || node.getSpec().getConfigShards().contains(configShard);
    }

    private boolean isolationGroupMatches(GatewayNode node, String isolationGroup) {
        String nodeIsolationGroup = node.getSpec().getIsolationGroup();
        if (StringUtils.hasText(isolationGroup)) {
            return Objects.equals(isolationGroup, nodeIsolationGroup);
        }
        return !StringUtils.hasText(nodeIsolationGroup);
    }

    private boolean isolationGroupMatches(GatewayNode node, GatewayProject project) {
        String projectIsolationGroup = project.getSpec().getIsolationGroup();
        String nodeIsolationGroup = node.getSpec().getIsolationGroup();
        if (StringUtils.hasText(projectIsolationGroup)) {
            return Objects.equals(projectIsolationGroup, nodeIsolationGroup);
        }
        return !StringUtils.hasText(nodeIsolationGroup);
    }

    private boolean projectSelectorMatches(GatewayNode node, GatewayProject project) {
        return selectorMatches(node.getSpec().getProjectSelector(), project.getMetadata().getLabels());
    }

    private boolean selectorMatches(LabelSelector selector, Map<String, String> labels) {
        if (selector == null) {
            return true;
        }
        for (Map.Entry<String, String> entry : selector.getMatchLabels().entrySet()) {
            if (!Objects.equals(labels.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        for (LabelSelector.MatchExpression expression : selector.getMatchExpressions()) {
            if (!expressionMatches(expression, labels)) {
                return false;
            }
        }
        return true;
    }

    private boolean expressionMatches(LabelSelector.MatchExpression expression, Map<String, String> labels) {
        String value = labels.get(expression.getKey());
        return switch (Objects.toString(expression.getOperator(), "")) {
            case "In" -> expression.getValues().contains(value);
            case "NotIn" -> !expression.getValues().contains(value);
            case "Exists" -> value != null;
            case "DoesNotExist" -> value == null;
            default -> false;
        };
    }

    private boolean shardEquals(String left, String right) {
        return Objects.equals(normalize(left), normalize(right));
    }

    private boolean isolationGroupEquals(String left, String right) {
        return Objects.equals(normalize(left), normalize(right));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private GatewayEvent loadSourceEvent(ReleaseIntent intent) {
        return (GatewayEvent) resourceService.get(GatePilotResourcePaths.EVENTS, intent.getNamespace(),
                intent.getSourceEventName());
    }

    private void saveEvent(GatewayEvent event) {
        GatePilotResourceType eventType = resourceService.requireResourceType(ResourceKind.GATEWAY_EVENT);
        resourceService.save(eventType, event.getMetadata().getNamespace(), event.getMetadata().getName(), event);
    }

    /**
     * 尝试保存已抢占事件。
     *
     * @param event 发布事件
     * @return 是否保存成功
     */
    private boolean trySaveClaimedEvent(GatewayEvent event) {
        try {
            saveEvent(event);
            return true;
        } catch (BusinessException exception) {
            if (isWriteConflict(exception)) {
                return false;
            }
            throw exception;
        }
    }

    /**
     * 判断是否为资源写入冲突。
     *
     * @param exception 业务异常
     * @return 是否为写入冲突
     */
    private boolean isWriteConflict(BusinessException exception) {
        return Objects.equals(exception.getErrorCodeValue(), CommonErrorCode.REQUEST_PROCESSING.code());
    }
}
