package com.dt.gatepilot.embedded.infrastructure.controller;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.enums.LoadBalanceStrategy;
import com.dt.gatepilot.domain.enums.NodeRole;
import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.domain.resource.event.GatewayEvent;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.project.GatewayProject;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.route.GatewayRoute;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import com.dt.gatepilot.apiserver.application.command.ReportAgentApplyResultCommand;
import com.dt.gatepilot.apiserver.application.command.PullAgentConfigCommand;
import com.dt.gatepilot.apiserver.application.command.CreateReleaseCommand;
import com.dt.gatepilot.apiserver.application.command.CreateRollbackCommand;
import com.dt.gatepilot.apiserver.application.dto.AgentConfigPullResult;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourcePaths;
import com.dt.gatepilot.apiserver.application.dto.ReleaseResult;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceRegistry;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceType;
import com.dt.gatepilot.apiserver.domain.resource.ResourceMetadataSupport;
import com.dt.gatepilot.apiserver.application.service.GatePilotAgentService;
import com.dt.gatepilot.apiserver.application.service.GatePilotConfigSnapshotService;
import com.dt.gatepilot.apiserver.application.service.GatePilotReleaseService;
import com.dt.gatepilot.apiserver.application.service.GatePilotResourceService;
import com.dt.gatepilot.apiserver.infrastructure.persistence.memory.InMemoryGatePilotResourceStore;
import com.dt.gatepilot.controller.infrastructure.leader.LocalControllerLeaderElector;
import com.dt.gatepilot.controller.application.service.NodeApplyStatusAggregator;
import com.dt.gatepilot.controller.application.service.PublishedConfigReconciler;
import com.dt.gatepilot.controller.application.service.PublishedConfigStatusController;
import com.dt.gatepilot.controller.application.service.ReleaseReconcileController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * controller-manager 资源适配器测试。
 */
class GatePilotControllerResourceAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GatePilotResourceService resourceService = new GatePilotResourceService(
            new GatePilotResourceRegistry(),
            new InMemoryGatePilotResourceStore(new ResourceMetadataSupport()),
            objectMapper
    );

    private final GatePilotConfigSnapshotService snapshotService =
            new GatePilotConfigSnapshotService(resourceService, objectMapper);

    private final GatePilotReleaseService releaseService =
            new GatePilotReleaseService(resourceService, snapshotService);

    private final GatePilotAgentService agentService = new GatePilotAgentService(resourceService);

    private final GatePilotControllerResourceAdapter adapter =
            new GatePilotControllerResourceAdapter(resourceService, snapshotService);

    private final ReleaseReconcileController reconcileController = new ReleaseReconcileController(
            new PublishedConfigReconciler(),
            new LocalControllerLeaderElector("test-controller"),
            adapter,
            adapter,
            adapter,
            adapter
    );

    private final PublishedConfigStatusController statusController = new PublishedConfigStatusController(
            new LocalControllerLeaderElector("test-controller"),
            adapter,
            new NodeApplyStatusAggregator()
    );

    @Test
    void shouldReconcileReleaseIntentAndMakePublishedConfigPullableByAgent() {
        saveProject();
        saveUpstream();
        saveRoute("/game");
        saveNode();
        CreateReleaseCommand request = new CreateReleaseCommand();
        request.setNamespace("default");
        request.setProjectName("game");
        request.setConfigShard("shard-a");
        request.setCreatedBy("operator");
        request.setDescription("发布 game 项目");

        ReleaseResult release = releaseService.createRelease(request);
        int processed = reconcileController.reconcileBatch(10);
        PullAgentConfigCommand pullRequest = new PullAgentConfigCommand();
        pullRequest.setNamespace("default");
        pullRequest.setNodeId("node-1");
        pullRequest.getConfigShards().add("shard-a");
        AgentConfigPullResult pullResponse = agentService.pullConfig(pullRequest);
        CursorPage<Object> snapshots = resourceService.list("config-snapshots", "default", null, 50);
        CursorPage<Object> events = resourceService.list("events", "default", null, 50);

        assertThat(processed).isEqualTo(1);
        assertThat(pullResponse.isChanged()).isTrue();
        assertThat(pullResponse.getPublishedConfig().getSpec().getVersion()).isEqualTo(release.getVersion());
        assertThat(pullResponse.getPublishedConfig().getSpec().getSequence()).isEqualTo(1L);
        assertThat(pullResponse.getPublishedConfig().getSpec().getRoutes()).hasSize(1);
        assertThat(pullResponse.getPublishedConfig().getSpec().getUpstreams()).hasSize(1);
        assertThat(pullResponse.getPublishedConfig().getStatus().getDesiredNodeCount()).isEqualTo(1);
        assertThat(snapshots.getItems()).hasSize(1);
        assertThat(snapshots.getItems())
                .extracting(item -> ((GatewayConfigSnapshot) item).getSpec().getReleaseId())
                .containsExactly(release.getReleaseId());
        assertThat(events.getItems())
                .map(item -> ((GatewayEvent) item).getSpec().getReason())
                .contains("ReleaseReconciled", "PublishedConfigGenerated");
    }

    @Test
    void shouldReconcileRollbackIntentFromSavedSnapshot() {
        saveProject();
        saveUpstream();
        saveRoute("/game");
        saveNode();
        ReleaseResult firstRelease = releaseService.createRelease(releaseCommand("operator", "发布 v1"));
        reconcileController.reconcileBatch(10);
        saveRoute("/game-v2");
        ReleaseResult secondRelease = releaseService.createRelease(releaseCommand("operator", "发布 v2"));
        reconcileController.reconcileBatch(10);
        CreateRollbackCommand rollbackCommand = new CreateRollbackCommand();
        rollbackCommand.setNamespace("default");
        rollbackCommand.setProjectName("game");
        rollbackCommand.setTargetVersion(firstRelease.getVersion());
        rollbackCommand.setConfigShard("shard-a");
        rollbackCommand.setCreatedBy("operator");

        ReleaseResult rollback = releaseService.createRollback(rollbackCommand);
        int processed = reconcileController.reconcileBatch(10);
        PullAgentConfigCommand pullRequest = new PullAgentConfigCommand();
        pullRequest.setNamespace("default");
        pullRequest.setNodeId("node-1");
        pullRequest.getConfigShards().add("shard-a");
        AgentConfigPullResult pullResponse = agentService.pullConfig(pullRequest);
        CursorPage<Object> events = resourceService.list("events", "default", null, 50);

        assertThat(secondRelease.getVersion()).isNotEqualTo(firstRelease.getVersion());
        assertThat(processed).isEqualTo(1);
        assertThat(pullResponse.getPublishedConfig().getSpec().getVersion()).isEqualTo(rollback.getVersion());
        assertThat(pullResponse.getPublishedConfig().getSpec().getSequence()).isEqualTo(3L);
        assertThat(pullResponse.getPublishedConfig().getSpec().getBaseVersion()).isEqualTo(firstRelease.getVersion());
        assertThat(pullResponse.getPublishedConfig().getSpec().getRoutes())
                .extracting(PublishedConfig.PublishedRoute::getPath)
                .containsExactly("/game");
        assertThat(events.getItems())
                .map(item -> ((GatewayEvent) item).getSpec().getReason())
                .contains("RollbackReconciled", "RollbackConfigGenerated");
    }

    @Test
    void shouldAggregateMultipleProxyApplyResultsAndKeepScaleOutPullable() {
        saveProject("game-high");
        saveUpstream();
        saveRoute("/game");
        saveNode("node-1", "shard-a", "game-high", "az-a");
        saveNode("node-2", "shard-a", "game-high", "az-b");
        ReleaseResult firstRelease = releaseService.createRelease(releaseCommand("operator", "发布 v1"));
        reconcileController.reconcileBatch(10);
        AgentConfigPullResult node1Pull = pullConfig("node-1", "shard-a", "game-high", null, null);
        AgentConfigPullResult node2Pull = pullConfig("node-2", "shard-a", "game-high", null, null);
        PublishedConfig firstConfig = node1Pull.getPublishedConfig();

        reportApply("node-1", firstRelease.getVersion(), firstConfig.getSpec().getConfigHash(),
                ConfigApplyState.APPLIED, "applied");
        reportApply("node-2", firstRelease.getVersion(), firstConfig.getSpec().getConfigHash(),
                ConfigApplyState.FAILED, "connect failed");
        int refreshed = statusController.refreshBatch(10);
        PublishedConfig refreshedConfig = storedPublishedConfig(firstConfig);
        saveNode("node-3", "shard-a", "game-high", "az-c");
        saveNode("node-4", "shard-a", "other-high", "az-d");
        AgentConfigPullResult scaledNodePull = pullConfig("node-3", "shard-a", "game-high", null, null);
        AgentConfigPullResult otherGroupPull = pullConfig("node-4", "shard-a", "other-high", null, null);
        saveRoute("/game-v2");
        ReleaseResult secondRelease = releaseService.createRelease(releaseCommand("operator", "发布 v2"));
        reconcileController.reconcileBatch(10);
        AgentConfigPullResult secondPull = pullConfig("node-1", "shard-a", "game-high",
                firstRelease.getVersion(), 1L);

        assertThat(node2Pull.isChanged()).isTrue();
        assertThat(firstConfig.getSpec().getIsolationGroup()).isEqualTo("game-high");
        assertThat(refreshed).isEqualTo(1);
        assertThat(refreshedConfig.getStatus().getDesiredNodeCount()).isEqualTo(2);
        assertThat(refreshedConfig.getStatus().getAppliedNodeCount()).isEqualTo(1);
        assertThat(refreshedConfig.getStatus().getFailedNodeCount()).isEqualTo(1);
        assertThat(refreshedConfig.getStatus().getApplyState()).isEqualTo(ConfigApplyState.FAILED);
        assertThat(refreshedConfig.getStatus().getNodeApplyResults())
                .extracting(PublishedConfig.NodeApplyResult::getNodeId)
                .containsExactlyInAnyOrder("node-1", "node-2");
        assertThat(scaledNodePull.isChanged()).isTrue();
        assertThat(scaledNodePull.getPublishedConfig().getSpec().getVersion()).isEqualTo(firstRelease.getVersion());
        assertThat(otherGroupPull.isChanged()).isFalse();
        assertThat(secondPull.isChanged()).isTrue();
        assertThat(secondPull.getPublishedConfig().getSpec().getVersion()).isEqualTo(secondRelease.getVersion());
        assertThat(secondPull.getPublishedConfig().getStatus().getApplyState()).isEqualTo(ConfigApplyState.PENDING);
        assertThat(secondPull.getPublishedConfig().getStatus().getAppliedNodeCount()).isZero();
    }

    private CreateReleaseCommand releaseCommand(String createdBy, String description) {
        CreateReleaseCommand request = new CreateReleaseCommand();
        request.setNamespace("default");
        request.setProjectName("game");
        request.setConfigShard("shard-a");
        request.setCreatedBy(createdBy);
        request.setDescription(description);
        return request;
    }

    private void saveProject() {
        saveProject(null);
    }

    private void saveProject(String isolationGroup) {
        GatewayProject project = new GatewayProject();
        project.getMetadata().getLabels().put("team", "game");
        project.getSpec().setConfigShard("shard-a");
        project.getSpec().setIsolationGroup(isolationGroup);
        GatePilotResourceType resourceType = resourceService.requireResourceType(ResourceKind.GATEWAY_PROJECT);
        resourceService.save(resourceType, "default", "game", project);
    }

    private void saveRoute(String path) {
        GatewayRoute route = new GatewayRoute();
        route.getSpec().setProjectRef(projectRef());
        route.getSpec().getProtocols().add(Protocol.HTTP);
        route.getSpec().getHosts().add("game.example.com");
        route.getSpec().getPath().setType("Prefix");
        route.getSpec().getPath().setValue(path);
        route.getSpec().setUpstreamRef(upstreamRef());
        GatePilotResourceType resourceType = resourceService.requireResourceType(ResourceKind.GATEWAY_ROUTE);
        resourceService.save(resourceType, "default", "game-api", route);
    }

    private void saveUpstream() {
        Upstream upstream = new Upstream();
        upstream.getSpec().setProjectRef(projectRef());
        upstream.getSpec().setProtocol(Protocol.HTTP);
        upstream.getSpec().setLoadBalance(LoadBalanceStrategy.WEIGHTED_ROUND_ROBIN);
        Upstream.UpstreamEndpoint endpoint = new Upstream.UpstreamEndpoint();
        endpoint.setHost("10.0.0.10");
        endpoint.setPort(8080);
        endpoint.setWeight(100);
        upstream.getSpec().getEndpoints().add(endpoint);
        GatePilotResourceType resourceType = resourceService.requireResourceType(ResourceKind.UPSTREAM);
        resourceService.save(resourceType, "default", "game-service", upstream);
    }

    private void saveNode() {
        saveNode("node-1", "shard-a", null, null);
    }

    private void saveNode(String nodeId, String configShard, String isolationGroup, String zone) {
        GatewayNode node = new GatewayNode();
        node.getSpec().setNodeId(nodeId);
        node.getSpec().setRole(NodeRole.COMBINED);
        node.getSpec().setZone(zone);
        node.getSpec().setIsolationGroup(isolationGroup);
        node.getSpec().getConfigShards().add(configShard);
        GatePilotResourceType resourceType = resourceService.requireResourceType(ResourceKind.GATEWAY_NODE);
        resourceService.save(resourceType, "default", nodeId, node);
    }

    private AgentConfigPullResult pullConfig(String nodeId,
                                             String configShard,
                                             String isolationGroup,
                                             String currentVersion,
                                             Long currentSequence) {
        PullAgentConfigCommand request = new PullAgentConfigCommand();
        request.setNamespace("default");
        request.setNodeId(nodeId);
        request.setIsolationGroup(isolationGroup);
        request.setCurrentVersion(currentVersion);
        request.setCurrentSequence(currentSequence);
        request.getConfigShards().add(configShard);
        return agentService.pullConfig(request);
    }

    private void reportApply(String nodeId, String version, String configHash, ConfigApplyState state, String message) {
        ReportAgentApplyResultCommand request = new ReportAgentApplyResultCommand();
        request.setNamespace("default");
        request.setNodeId(nodeId);
        request.setVersion(version);
        request.setConfigHash(configHash);
        request.setState(state);
        request.setMessage(message);
        agentService.reportApplyResult(request);
    }

    private PublishedConfig storedPublishedConfig(PublishedConfig config) {
        return (PublishedConfig) resourceService.get(GatePilotResourcePaths.PUBLISHED_CONFIGS,
                config.getMetadata().getNamespace(), config.getMetadata().getName());
    }

    private ResourceReference projectRef() {
        ResourceReference reference = new ResourceReference();
        reference.setKind(ResourceKind.GATEWAY_PROJECT);
        reference.setNamespace("default");
        reference.setName("game");
        return reference;
    }

    private ResourceReference upstreamRef() {
        ResourceReference reference = new ResourceReference();
        reference.setKind(ResourceKind.UPSTREAM);
        reference.setNamespace("default");
        reference.setName("game-service");
        return reference;
    }
}
