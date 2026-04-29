/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dt.gatepilot.embedded.infrastructure.controller;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.application.dto.AgentRuntimeAuditBatch;
import com.dt.gatepilot.agent.application.service.AgentRuntimeCoordinator;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.agent.infrastructure.persistence.memory.InMemoryLocalConfigStore;
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
import com.dt.gatepilot.controller.domain.model.GatewayDesiredState;
import com.dt.gatepilot.controller.domain.model.ReleaseIntent;
import com.dt.gatepilot.controller.application.service.NodeApplyStatusAggregator;
import com.dt.gatepilot.controller.application.service.PublishedConfigReconciler;
import com.dt.gatepilot.controller.application.service.PublishedConfigStatusController;
import com.dt.gatepilot.controller.application.service.ReleaseReconcileController;
import com.dt.gatepilot.embedded.infrastructure.assembly.InProcessProxyApplyClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dt.gatepilot.proxy.domain.runtime.PublishedConfigCompiler;
import com.dt.gatepilot.proxy.domain.runtime.ProxyConfigApplier;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * controller-manager 资源适配器测试。
 */
class GatePilotControllerResourceAdapterTest {

    private static final int SCALE_PROJECT_COUNT = 1000;

    private static final int SCALE_PAGE_LIMIT = 200;

    private static final Duration SCALE_TIMEOUT = Duration.ofSeconds(15);

    private static final String DEFAULT_NAMESPACE = "default";

    private static final String DEFAULT_CONFIG_SHARD = "shard-a";

    private static final String DEFAULT_NODE_ID = "node-1";

    private static final String TEST_OPERATOR = "operator";

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

    @Test
    void shouldCompleteReleaseApplyPipelineThroughAgentAndProxy() {
        saveProject();
        saveUpstream();
        saveRoute("/game");
        saveNode();
        ProxyRuntimeState runtimeState = new ProxyRuntimeState();
        InMemoryLocalConfigStore localConfigStore = new InMemoryLocalConfigStore();
        AgentRuntimeCoordinator coordinator = new AgentRuntimeCoordinator(
                agentProfile(),
                new EmbeddedAgentControlPlaneClient(),
                localConfigStore,
                new InProcessProxyApplyClient(new ProxyConfigApplier(new PublishedConfigCompiler(), runtimeState))
        );

        ReleaseResult release = releaseService.createRelease(releaseCommand("operator", "端到端发布"));
        int processed = reconcileController.reconcileBatch(10);
        Optional<AgentApplyResult> applyResult = coordinator.pullAndApply();
        int refreshed = statusController.refreshBatch(10);
        PublishedConfig storedConfig = latestPublishedConfig();
        Optional<AgentApplyResult> secondApply = coordinator.pullAndApply();

        assertThat(processed).isEqualTo(1);
        assertThat(applyResult).hasValueSatisfying(result -> {
            assertThat(result.getState()).isEqualTo(ConfigApplyState.APPLIED);
            assertThat(result.getVersion()).isEqualTo(release.getVersion());
        });
        assertThat(localConfigStore.loadLastGood()).hasValueSatisfying(config ->
                assertThat(config.getSpec().getVersion()).isEqualTo(release.getVersion()));
        assertThat(runtimeState.current()).hasValueSatisfying(runtime -> {
            assertThat(runtime.getVersion()).isEqualTo(release.getVersion());
            assertThat(runtime.match("game.example.com", "/game/start")).isNotNull();
        });
        assertThat(refreshed).isEqualTo(1);
        assertThat(storedConfig.getStatus().getApplyState()).isEqualTo(ConfigApplyState.APPLIED);
        assertThat(storedConfig.getStatus().getAppliedNodeCount()).isEqualTo(1);
        assertThat(secondApply).isEmpty();
    }

    @Test
    void shouldLoadThousandProjectsGenerateConfigAndPageResources() {
        assertTimeout(SCALE_TIMEOUT, () -> {
            for (int index = 0; index < SCALE_PROJECT_COUNT; index++) {
                String projectName = scaleProjectName(index);
                String upstreamName = scaleUpstreamName(index);
                ResourceReference projectReference = projectRef(projectName);
                // 先装载足够多的声明式资源
                saveProjectResource(projectName, DEFAULT_CONFIG_SHARD, null, "team-" + index);
                saveUpstream(upstreamName, projectReference);
                saveRoute(scaleRouteName(index), scaleRoutePath(index), projectReference, upstreamRef(upstreamName));
            }
            saveNode();
            String targetProjectName = scaleProjectName(SCALE_PROJECT_COUNT - 1);

            // 发布最后一个项目，逼着 adapter 跨页读取路由和上游
            ReleaseResult release = releaseService.createRelease(releaseCommand(targetProjectName,
                    TEST_OPERATOR, "千项目发布压测"));
            int processed = reconcileController.reconcileBatch(10);
            AgentConfigPullResult pullResponse = pullConfig(DEFAULT_NODE_ID, DEFAULT_CONFIG_SHARD, null, null, null);
            PublishedConfig publishedConfig = pullResponse.getPublishedConfig();

            assertThat(processed).isEqualTo(1);
            assertThat(pullResponse.isChanged()).isTrue();
            assertThat(publishedConfig.getSpec().getVersion()).isEqualTo(release.getVersion());
            assertThat(publishedConfig.getSpec().getProjectRef().getName()).isEqualTo(targetProjectName);
            assertThat(publishedConfig.getSpec().getRoutes()).hasSize(SCALE_PROJECT_COUNT);
            assertThat(publishedConfig.getSpec().getRoutes())
                    .extracting(PublishedConfig.PublishedRoute::getPath)
                    .contains(scaleRoutePath(0), scaleRoutePath(SCALE_PROJECT_COUNT - 1));
            assertThat(publishedConfig.getSpec().getRoutes())
                    .extracting(PublishedConfig.PublishedRoute::getProjectName)
                    .contains(scaleProjectName(0), scaleProjectName(SCALE_PROJECT_COUNT - 1));
            assertThat(publishedConfig.getSpec().getUpstreams()).hasSize(SCALE_PROJECT_COUNT);
            assertThat(publishedConfig.getSpec().getUpstreams())
                    .extracting(PublishedConfig.PublishedUpstream::getName)
                    .contains(scaleUpstreamName(0), scaleUpstreamName(SCALE_PROJECT_COUNT - 1));

            // 页面列表也要按 cursor 走完整分页
            assertThat(countResourcesByPage(GatePilotResourcePaths.PROJECTS)).isEqualTo(SCALE_PROJECT_COUNT);
            assertThat(countResourcesByPage(GatePilotResourcePaths.ROUTES)).isEqualTo(SCALE_PROJECT_COUNT);
            assertThat(countResourcesByPage(GatePilotResourcePaths.UPSTREAMS)).isEqualTo(SCALE_PROJECT_COUNT);
        });
    }

    @Test
    void shouldReadDesiredStateBeyondFirstResourcePage() {
        saveProject();
        for (int index = 0; index < 600; index++) {
            ResourceReference noiseProject = projectRef("noise-" + index);
            String noiseUpstreamName = "a-noise-upstream-" + String.format("%03d", index);
            saveRoute("a-noise-route-" + String.format("%03d", index), "/noise-" + index,
                    noiseProject, upstreamRef(noiseUpstreamName));
            saveUpstream(noiseUpstreamName, noiseProject);
        }
        saveUpstream();
        saveRoute("/game");

        GatewayDesiredState desiredState = adapter.read(releaseIntent());

        assertThat(desiredState.getRoutes())
                .extracting(route -> route.getSpec().getPath().getValue())
                .containsExactly("/game");
        assertThat(desiredState.getUpstreams())
                .extracting(item -> item.getMetadata().getName())
                .containsExactly("game-service");
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

    private CreateReleaseCommand releaseCommand(String projectName, String createdBy, String description) {
        CreateReleaseCommand request = releaseCommand(createdBy, description);
        request.setProjectName(projectName);
        return request;
    }

    private void saveProject() {
        saveProject(null);
    }

    private void saveProject(String isolationGroup) {
        saveProjectResource("game", DEFAULT_CONFIG_SHARD, isolationGroup, "game");
    }

    private void saveProjectResource(String name, String configShard, String isolationGroup, String team) {
        GatewayProject project = new GatewayProject();
        project.getMetadata().getLabels().put("team", team);
        project.getSpec().setConfigShard(configShard);
        project.getSpec().setIsolationGroup(isolationGroup);
        GatePilotResourceType resourceType = resourceService.requireResourceType(ResourceKind.GATEWAY_PROJECT);
        resourceService.save(resourceType, "default", name, project);
    }

    private void saveRoute(String path) {
        saveRoute("game-api", path, projectRef(), upstreamRef());
    }

    private void saveRoute(String name, String path, ResourceReference projectRef, ResourceReference upstreamRef) {
        GatewayRoute route = new GatewayRoute();
        route.getSpec().setProjectRef(projectRef);
        route.getSpec().getProtocols().add(Protocol.HTTP);
        route.getSpec().getHosts().add("game.example.com");
        route.getSpec().getPath().setType("Prefix");
        route.getSpec().getPath().setValue(path);
        route.getSpec().setUpstreamRef(upstreamRef);
        GatePilotResourceType resourceType = resourceService.requireResourceType(ResourceKind.GATEWAY_ROUTE);
        resourceService.save(resourceType, "default", name, route);
    }

    private void saveUpstream() {
        saveUpstream("game-service", projectRef());
    }

    private void saveUpstream(String name, ResourceReference projectRef) {
        Upstream upstream = new Upstream();
        upstream.getSpec().setProjectRef(projectRef);
        upstream.getSpec().setProtocol(Protocol.HTTP);
        upstream.getSpec().setLoadBalance(LoadBalanceStrategy.WEIGHTED_ROUND_ROBIN);
        Upstream.UpstreamEndpoint endpoint = new Upstream.UpstreamEndpoint();
        endpoint.setHost("10.0.0.10");
        endpoint.setPort(8080);
        endpoint.setWeight(100);
        upstream.getSpec().getEndpoints().add(endpoint);
        GatePilotResourceType resourceType = resourceService.requireResourceType(ResourceKind.UPSTREAM);
        resourceService.save(resourceType, "default", name, upstream);
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

    private PublishedConfig latestPublishedConfig() {
        return (PublishedConfig) resourceService.list(GatePilotResourcePaths.PUBLISHED_CONFIGS,
                        "default", null, 10)
                .getItems()
                .get(0);
    }

    private AgentNodeProfile agentProfile() {
        AgentNodeProfile profile = new AgentNodeProfile();
        profile.setNamespace("default");
        profile.setNodeId("node-1");
        profile.getConfigShards().add("shard-a");
        return profile;
    }

    private ResourceReference projectRef() {
        return projectRef("game");
    }

    private ResourceReference projectRef(String name) {
        ResourceReference reference = new ResourceReference();
        reference.setKind(ResourceKind.GATEWAY_PROJECT);
        reference.setNamespace(DEFAULT_NAMESPACE);
        reference.setName(name);
        return reference;
    }

    private ResourceReference upstreamRef() {
        return upstreamRef("game-service");
    }

    private ResourceReference upstreamRef(String name) {
        ResourceReference reference = new ResourceReference();
        reference.setKind(ResourceKind.UPSTREAM);
        reference.setNamespace(DEFAULT_NAMESPACE);
        reference.setName(name);
        return reference;
    }

    private ReleaseIntent releaseIntent() {
        ReleaseIntent intent = new ReleaseIntent();
        intent.setNamespace("default");
        intent.setProjectName("game");
        intent.setConfigShard("shard-a");
        return intent;
    }

    private long countResourcesByPage(String resourcePath) {
        long count = 0;
        String cursor = null;
        do {
            CursorPage<Object> page = resourceService.list(resourcePath, DEFAULT_NAMESPACE, cursor, SCALE_PAGE_LIMIT);
            count += page.getItems().size();
            cursor = page.getNextCursor();
        } while (cursor != null);
        return count;
    }

    private String scaleProjectName(int index) {
        return "project-" + String.format("%04d", index);
    }

    private String scaleRouteName(int index) {
        return "route-" + String.format("%04d", index);
    }

    private String scaleRoutePath(int index) {
        return "/scale/project-" + String.format("%04d", index);
    }

    private String scaleUpstreamName(int index) {
        return "upstream-" + String.format("%04d", index);
    }

    private class EmbeddedAgentControlPlaneClient implements AgentControlPlaneClient {

        @Override
        public void register(AgentNodeProfile profile) {
            // 当前端到端测试已预置节点资源
        }

        @Override
        public void heartbeat(AgentHeartbeatSnapshot heartbeat) {
            // 当前端到端测试只验证发布和 apply 主链路
        }

        @Override
        public Optional<PublishedConfig> pullConfig(AgentConfigCursor cursor) {
            PullAgentConfigCommand request = new PullAgentConfigCommand();
            request.setNamespace(cursor.getNamespace());
            request.setNodeId(cursor.getNodeId());
            request.setZone(cursor.getZone());
            request.setIsolationGroup(cursor.getIsolationGroup());
            request.setCurrentVersion(cursor.getCurrentVersion());
            request.setCurrentSequence(cursor.getCurrentSequence());
            request.getConfigShards().addAll(cursor.getConfigShards());
            return Optional.ofNullable(agentService.pullConfig(request).getPublishedConfig());
        }

        @Override
        public void reportApplyResult(AgentApplyResult result) {
            reportApply(result.getNodeId(), result.getVersion(), result.getConfigHash(),
                    result.getState(), result.getMessage());
        }

        @Override
        public void reportRuntimeAudits(AgentRuntimeAuditBatch batch) {
            // 当前端到端测试不验证运行审计批量上报
        }
    }
}
