package com.dt.gatepilot.apiserver.infrastructure.controller;

import com.dt.gatepilot.api.enums.EventSeverity;
import com.dt.gatepilot.api.enums.ResourceKind;
import com.dt.gatepilot.api.resource.common.LabelSelector;
import com.dt.gatepilot.api.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.api.resource.event.GatewayEvent;
import com.dt.gatepilot.api.resource.node.GatewayNode;
import com.dt.gatepilot.api.resource.policy.AuthPolicy;
import com.dt.gatepilot.api.resource.policy.ReleasePolicy;
import com.dt.gatepilot.api.resource.policy.TrafficPolicy;
import com.dt.gatepilot.api.resource.project.GatewayProject;
import com.dt.gatepilot.api.resource.publish.PublishedConfig;
import com.dt.gatepilot.api.resource.route.GatewayRoute;
import com.dt.gatepilot.api.resource.upstream.Upstream;
import com.dt.gatepilot.apiserver.api.response.CursorPageResponse;
import com.dt.gatepilot.apiserver.support.resource.GatePilotResourceType;
import com.dt.gatepilot.apiserver.support.service.GatePilotConfigSnapshotService;
import com.dt.gatepilot.apiserver.support.service.GatePilotResourceService;
import com.dt.gatepilot.controller.api.ReconcileResult;
import com.dt.gatepilot.controller.spi.GatewayDesiredStateReader;
import com.dt.gatepilot.controller.spi.ReconcileResultSink;
import com.dt.gatepilot.controller.spi.ReleaseIntentSource;
import com.dt.gatepilot.controller.support.model.GatewayDesiredState;
import com.dt.gatepilot.controller.support.model.ReleaseIntent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * controller-manager 使用的 apiserver 进程内资源适配器。
 */
@Component
public class GatePilotControllerResourceAdapter
        implements ReleaseIntentSource, GatewayDesiredStateReader, ReconcileResultSink {

    private static final int LIST_LIMIT = 500;

    private static final String EVENT_TYPE_LABEL = "gatepilot.io/event-type";

    private static final String RECONCILE_STATE_LABEL = "gatepilot.io/reconcile-state";

    private static final String RECONCILE_CONTROLLER_LABEL = "gatepilot.io/reconcile-controller";

    private static final String RELEASE_REQUEST = "release-request";

    private static final String STATE_PENDING = "pending";

    private static final String STATE_PROCESSING = "processing";

    private static final String STATE_COMPLETED = "completed";

    private static final String STATE_FAILED = "failed";

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
        int effectiveLimit = Math.min(limit, LIST_LIMIT);
        return list("events", null, GatewayEvent.class)
                .stream()
                .filter(this::isPendingReleaseRequest)
                .limit(effectiveLimit)
                .map(this::toReleaseIntent)
                .toList();
    }

    @Override
    public boolean claim(ReleaseIntent intent, String controllerId) {
        GatewayEvent event = loadSourceEvent(intent);
        if (!isPendingReleaseRequest(event)) {
            return false;
        }
        event.getMetadata().getLabels().put(RECONCILE_STATE_LABEL, STATE_PROCESSING);
        event.getMetadata().getLabels().put(RECONCILE_CONTROLLER_LABEL, controllerId);
        event.getSpec().getAttributes().put("claimedBy", controllerId);
        event.getSpec().getAttributes().put("claimedAt", Instant.now().toString());
        event.getSpec().setLastObservedAt(Instant.now());
        intent.setSequence(nextSequence(intent.getNamespace(), intent.getConfigShard()));
        saveEvent(event);
        return true;
    }

    @Override
    public void markCompleted(ReleaseIntent intent, ReconcileResult result) {
        GatewayEvent event = loadSourceEvent(intent);
        PublishedConfig publishedConfig = result.getPublishedConfig();
        event.getMetadata().getLabels().put(RECONCILE_STATE_LABEL, STATE_COMPLETED);
        event.getMetadata().getLabels().put("gatepilot.io/version", publishedConfig.getSpec().getVersion());
        event.getSpec().setReason("ReleaseReconciled");
        event.getSpec().setMessage("controller-manager 已生成 PublishedConfig 并保存快照");
        event.getSpec().setSeverity(EventSeverity.INFO);
        event.getSpec().setLastObservedAt(Instant.now());
        event.getSpec().getAttributes().put("publishedConfigName", publishedConfig.getMetadata().getName());
        event.getSpec().getAttributes().put("configHash", publishedConfig.getSpec().getConfigHash());
        event.getSpec().getAttributes().put("completedAt", Instant.now().toString());
        saveEvent(event);
    }

    @Override
    public void markFailed(ReleaseIntent intent, String reason, String message) {
        GatewayEvent event = loadSourceEvent(intent);
        event.getMetadata().getLabels().put(RECONCILE_STATE_LABEL, STATE_FAILED);
        event.getSpec().setReason(reason);
        event.getSpec().setMessage(message);
        event.getSpec().setSeverity(EventSeverity.ERROR);
        event.getSpec().setLastObservedAt(Instant.now());
        event.getSpec().getAttributes().put("failedAt", Instant.now().toString());
        saveEvent(event);
    }

    @Override
    public GatewayDesiredState read(ReleaseIntent intent) {
        GatewayProject project = (GatewayProject) resourceService.get(
                "projects",
                intent.getNamespace(),
                intent.getProjectName()
        );
        GatewayDesiredState desiredState = new GatewayDesiredState();
        desiredState.setProject(project);
        desiredState.setRoutes(projectRoutes(intent));
        desiredState.setUpstreams(projectUpstreams(intent));
        desiredState.setTrafficPolicies(projectTrafficPolicies(intent));
        desiredState.setReleasePolicies(projectReleasePolicies(intent));
        desiredState.setAuthPolicies(projectAuthPolicies(intent));
        desiredState.setTargetNodes(targetNodes(intent, project));
        return desiredState;
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

    private List<GatewayRoute> projectRoutes(ReleaseIntent intent) {
        return list("routes", intent.getNamespace(), GatewayRoute.class)
                .stream()
                .filter(route -> projectMatches(route.getSpec().getProjectRef(), intent.getProjectName()))
                .toList();
    }

    private List<Upstream> projectUpstreams(ReleaseIntent intent) {
        return list("upstreams", intent.getNamespace(), Upstream.class)
                .stream()
                .filter(upstream -> projectMatches(upstream.getSpec().getProjectRef(), intent.getProjectName()))
                .toList();
    }

    private List<TrafficPolicy> projectTrafficPolicies(ReleaseIntent intent) {
        return list("traffic-policies", intent.getNamespace(), TrafficPolicy.class)
                .stream()
                .filter(policy -> projectMatches(policy.getSpec().getProjectRef(), intent.getProjectName()))
                .toList();
    }

    private List<ReleasePolicy> projectReleasePolicies(ReleaseIntent intent) {
        return list("release-policies", intent.getNamespace(), ReleasePolicy.class)
                .stream()
                .filter(policy -> projectMatches(policy.getSpec().getProjectRef(), intent.getProjectName()))
                .toList();
    }

    private List<AuthPolicy> projectAuthPolicies(ReleaseIntent intent) {
        return list("auth-policies", intent.getNamespace(), AuthPolicy.class)
                .stream()
                .filter(policy -> projectMatches(policy.getSpec().getProjectRef(), intent.getProjectName()))
                .toList();
    }

    private List<GatewayNode> targetNodes(ReleaseIntent intent, GatewayProject project) {
        return list("nodes", intent.getNamespace(), GatewayNode.class)
                .stream()
                .filter(node -> shardMatches(node, intent.getConfigShard()))
                .filter(node -> isolationGroupMatches(node, project))
                .filter(node -> projectSelectorMatches(node, project))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> list(String resourcePath, String namespace, Class<T> resourceType) {
        CursorPageResponse<Object> page = resourceService.list(resourcePath, namespace, null, LIST_LIMIT);
        return page.getItems()
                .stream()
                .map(resource -> (T) resourceType.cast(resource))
                .toList();
    }

    private boolean isPendingReleaseRequest(GatewayEvent event) {
        Map<String, String> labels = event.getMetadata().getLabels();
        return RELEASE_REQUEST.equals(labels.get(EVENT_TYPE_LABEL))
                && STATE_PENDING.equals(labels.get(RECONCILE_STATE_LABEL));
    }

    private ReleaseIntent toReleaseIntent(GatewayEvent event) {
        Map<String, String> attributes = event.getSpec().getAttributes();
        ReleaseIntent intent = new ReleaseIntent();
        intent.setReleaseId(attributes.get("releaseId"));
        intent.setNamespace(event.getMetadata().getNamespace());
        intent.setProjectName(attributes.get("projectName"));
        if (!StringUtils.hasText(intent.getProjectName()) && event.getSpec().getInvolvedObject() != null) {
            intent.setProjectName(event.getSpec().getInvolvedObject().getName());
        }
        intent.setVersion(attributes.get("version"));
        intent.setConfigShard(event.getMetadata().getLabels().get("gatepilot.io/config-shard"));
        intent.setTrigger("publish");
        intent.setRequestedBy(attributes.get("createdBy"));
        intent.setDescription(attributes.get("description"));
        intent.setRequestedAt(event.getSpec().getFirstObservedAt());
        intent.setSourceEventName(event.getMetadata().getName());
        return intent;
    }

    private long nextSequence(String namespace, String configShard) {
        return list("published-configs", namespace, PublishedConfig.class)
                .stream()
                .filter(config -> shardEquals(config.getSpec().getConfigShard(), configShard))
                .map(PublishedConfig::getSpec)
                .map(PublishedConfig.PublishedConfigSpec::getSequence)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private boolean projectMatches(com.dt.gatepilot.api.resource.common.ResourceReference reference,
                                   String projectName) {
        return reference != null && Objects.equals(reference.getName(), projectName);
    }

    private boolean shardMatches(GatewayNode node, String configShard) {
        if (!StringUtils.hasText(configShard)) {
            return true;
        }
        return node.getSpec().getConfigShards().isEmpty()
                || node.getSpec().getConfigShards().contains(configShard);
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

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private GatewayEvent loadSourceEvent(ReleaseIntent intent) {
        return (GatewayEvent) resourceService.get("events", intent.getNamespace(), intent.getSourceEventName());
    }

    private void saveEvent(GatewayEvent event) {
        GatePilotResourceType eventType = resourceService.requireResourceType(ResourceKind.GATEWAY_EVENT);
        resourceService.save(eventType, event.getMetadata().getNamespace(), event.getMetadata().getName(), event);
    }
}
