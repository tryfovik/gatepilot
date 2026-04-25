package com.dt.gatepilot.apiserver.infrastructure.controller;

import com.dt.gatepilot.domain.enums.LoadBalanceStrategy;
import com.dt.gatepilot.domain.enums.NodeRole;
import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.common.ResourceReference;
import com.dt.gatepilot.domain.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.domain.resource.event.GatewayEvent;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.project.GatewayProject;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.route.GatewayRoute;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import com.dt.gatepilot.apiserver.application.command.PullAgentConfigCommand;
import com.dt.gatepilot.apiserver.application.command.CreateReleaseCommand;
import com.dt.gatepilot.apiserver.application.dto.AgentConfigPullResult;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
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
import com.dt.gatepilot.controller.application.service.PublishedConfigReconciler;
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
            adapter
    );

    @Test
    void shouldReconcileReleaseIntentAndMakePublishedConfigPullableByAgent() {
        saveProject();
        saveUpstream();
        saveRoute();
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

    private void saveProject() {
        GatewayProject project = new GatewayProject();
        project.getMetadata().getLabels().put("team", "game");
        project.getSpec().setConfigShard("shard-a");
        GatePilotResourceType resourceType = resourceService.requireResourceType(ResourceKind.GATEWAY_PROJECT);
        resourceService.save(resourceType, "default", "game", project);
    }

    private void saveRoute() {
        GatewayRoute route = new GatewayRoute();
        route.getSpec().setProjectRef(projectRef());
        route.getSpec().getProtocols().add(Protocol.HTTP);
        route.getSpec().getHosts().add("game.example.com");
        route.getSpec().getPath().setType("Prefix");
        route.getSpec().getPath().setValue("/game");
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
        GatewayNode node = new GatewayNode();
        node.getSpec().setNodeId("node-1");
        node.getSpec().setRole(NodeRole.COMBINED);
        node.getSpec().getConfigShards().add("shard-a");
        GatePilotResourceType resourceType = resourceService.requireResourceType(ResourceKind.GATEWAY_NODE);
        resourceService.save(resourceType, "default", "node-1", node);
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
