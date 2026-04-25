package com.dt.gatepilot.apiserver.infrastructure.controller;

import com.dt.gatepilot.api.enums.LoadBalanceStrategy;
import com.dt.gatepilot.api.enums.NodeRole;
import com.dt.gatepilot.api.enums.Protocol;
import com.dt.gatepilot.api.enums.ResourceKind;
import com.dt.gatepilot.api.resource.common.ResourceReference;
import com.dt.gatepilot.api.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.api.resource.event.GatewayEvent;
import com.dt.gatepilot.api.resource.node.GatewayNode;
import com.dt.gatepilot.api.resource.project.GatewayProject;
import com.dt.gatepilot.api.resource.publish.PublishedConfig;
import com.dt.gatepilot.api.resource.route.GatewayRoute;
import com.dt.gatepilot.api.resource.upstream.Upstream;
import com.dt.gatepilot.apiserver.api.request.AgentConfigPullRequest;
import com.dt.gatepilot.apiserver.api.request.ReleaseRequest;
import com.dt.gatepilot.apiserver.api.response.AgentConfigPullResponse;
import com.dt.gatepilot.apiserver.api.response.CursorPageResponse;
import com.dt.gatepilot.apiserver.api.response.ReleaseResponse;
import com.dt.gatepilot.apiserver.support.resource.GatePilotResourceRegistry;
import com.dt.gatepilot.apiserver.support.resource.GatePilotResourceType;
import com.dt.gatepilot.apiserver.support.resource.ResourceMetadataSupport;
import com.dt.gatepilot.apiserver.support.service.GatePilotAgentService;
import com.dt.gatepilot.apiserver.support.service.GatePilotConfigSnapshotService;
import com.dt.gatepilot.apiserver.support.service.GatePilotReleaseService;
import com.dt.gatepilot.apiserver.support.service.GatePilotResourceService;
import com.dt.gatepilot.apiserver.support.store.InMemoryGatePilotResourceStore;
import com.dt.gatepilot.controller.support.leader.LocalControllerLeaderElector;
import com.dt.gatepilot.controller.support.reconcile.PublishedConfigReconciler;
import com.dt.gatepilot.controller.support.reconcile.ReleaseReconcileController;
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
        ReleaseRequest request = new ReleaseRequest();
        request.setNamespace("default");
        request.setProjectName("game");
        request.setConfigShard("shard-a");
        request.setCreatedBy("operator");
        request.setDescription("发布 game 项目");

        ReleaseResponse release = releaseService.createRelease(request);
        int processed = reconcileController.reconcileBatch(10);
        AgentConfigPullRequest pullRequest = new AgentConfigPullRequest();
        pullRequest.setNamespace("default");
        pullRequest.setNodeId("node-1");
        pullRequest.getConfigShards().add("shard-a");
        AgentConfigPullResponse pullResponse = agentService.pullConfig(pullRequest);
        CursorPageResponse<Object> snapshots = resourceService.list("config-snapshots", "default", null, 50);
        CursorPageResponse<Object> events = resourceService.list("events", "default", null, 50);

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
