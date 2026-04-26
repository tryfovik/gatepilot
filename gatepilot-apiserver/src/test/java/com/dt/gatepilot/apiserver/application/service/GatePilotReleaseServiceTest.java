package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.apiserver.application.command.CreateReleaseCommand;
import com.dt.gatepilot.apiserver.application.command.CreateRollbackCommand;
import com.dt.gatepilot.apiserver.application.dto.ReleaseDryRunResult;
import com.dt.gatepilot.apiserver.application.dto.ReleaseResult;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceRegistry;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceType;
import com.dt.gatepilot.apiserver.domain.resource.ResourceMetadataSupport;
import com.dt.gatepilot.apiserver.infrastructure.persistence.memory.InMemoryGatePilotResourceStore;
import com.dt.gatepilot.domain.enums.LoadBalanceStrategy;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.project.GatewayProject;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.route.GatewayRoute;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 发布请求服务测试。
 */
class GatePilotReleaseServiceTest {

    private static final String RELEASE_VERSION_PATTERN = "game-release-[0-9a-f]{32}";

    private static final String ROLLBACK_VERSION_PATTERN = "game-rollback-[0-9a-f]{32}";

    private static final String DRY_RUN_VERSION_PATTERN = "missing-dry-run-[0-9a-f]{32}";

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

    @Test
    void shouldDryRunReleaseWithoutWritingReleaseEvent() {
        CreateReleaseCommand request = new CreateReleaseCommand();
        request.setNamespace("default");
        request.setProjectName("missing");

        ReleaseDryRunResult response = releaseService.dryRun(request);
        CursorPage<Object> events = resourceService.list("events", "default", null, 50);

        assertThat(response.isPassed()).isFalse();
        assertThat(response.getMessages())
                .extracting(ReleaseDryRunResult.DryRunMessage::getReason)
                .contains("ProjectNotFound");
        assertThat(response.getVersion()).matches(DRY_RUN_VERSION_PATTERN);
        assertThat(response.getVersion()).doesNotContain(String.valueOf(response.getCheckedAt().toEpochMilli()));
        assertThat(events.getItems()).isEmpty();
    }

    @Test
    void shouldCreateUniqueReleaseVersionsForBackToBackRequests() {
        saveProject("default", "game");
        CreateReleaseCommand request = new CreateReleaseCommand();
        request.setNamespace("default");
        request.setProjectName("game");
        request.setCreatedBy("operator");

        ReleaseResult first = releaseService.createRelease(request);
        ReleaseResult second = releaseService.createRelease(request);

        assertThat(first.getVersion()).isNotEqualTo(second.getVersion());
        assertThat(first.getVersion()).matches(RELEASE_VERSION_PATTERN);
        assertThat(second.getVersion()).matches(RELEASE_VERSION_PATTERN);
        assertThat(first.getVersion()).doesNotContain(String.valueOf(first.getCreatedAt().toEpochMilli()));
    }

    @Test
    void shouldValidateProjectScopedRoutesAndReferencesOnDryRun() {
        saveProject("default", "game");
        saveRouteWithMissingRefs();
        saveEmptyUpstream();
        CreateReleaseCommand request = new CreateReleaseCommand();
        request.setNamespace("default");
        request.setProjectName("game");

        ReleaseDryRunResult response = releaseService.dryRun(request);

        assertThat(response.isPassed()).isFalse();
        assertThat(response.getRouteCount()).isEqualTo(1);
        assertThat(response.getUpstreamCount()).isEqualTo(1);
        assertThat(response.getMessages())
                .extracting(ReleaseDryRunResult.DryRunMessage::getReason)
                .contains(
                        GatePilotReleaseConstants.REASON_ROUTE_HOST_MISSING,
                        GatePilotReleaseConstants.REASON_ROUTE_UPSTREAM_MISSING,
                        GatePilotReleaseConstants.REASON_ROUTE_POLICY_MISSING,
                        GatePilotReleaseConstants.REASON_UPSTREAM_ENDPOINT_MISSING
                );
    }

    @Test
    void shouldDryRunAllPagesWhenProjectHasLargeRouteSet() {
        saveProject("default", "game");
        saveAvailableUpstream();
        for (int index = 0; index <= GatePilotReleaseConstants.DRY_RUN_LOOKUP_LIMIT; index++) {
            saveValidRoute(index);
        }
        CreateReleaseCommand request = new CreateReleaseCommand();
        request.setNamespace("default");
        request.setProjectName("game");

        ReleaseDryRunResult response = releaseService.dryRun(request);

        assertThat(response.isPassed()).isTrue();
        assertThat(response.getRouteCount()).isEqualTo(GatePilotReleaseConstants.DRY_RUN_LOOKUP_LIMIT + 1);
        assertThat(response.getUpstreamCount()).isEqualTo(1);
    }

    @Test
    void shouldRejectUnsupportedLoadBalanceStrategyOnDryRun() {
        saveProject("default", "game");
        saveUnsupportedLoadBalanceUpstream();
        saveRouteForUpstream("hash-route", "hash-upstream");
        CreateReleaseCommand request = new CreateReleaseCommand();
        request.setNamespace("default");
        request.setProjectName("game");

        ReleaseDryRunResult response = releaseService.dryRun(request);

        assertThat(response.isPassed()).isFalse();
        assertThat(response.getMessages())
                .extracting(ReleaseDryRunResult.DryRunMessage::getReason)
                .contains(GatePilotReleaseConstants.REASON_UPSTREAM_LOAD_BALANCE_UNSUPPORTED);
    }

    @Test
    void shouldCreateRollbackEventAndMarkSnapshot() {
        saveProject("default", "game");
        PublishedConfig config = publishedConfig("default", "game", "v1", "shard-a");
        GatewayConfigSnapshot snapshot = snapshotService.saveSnapshot(config, "rel-1", "tester", "stable");
        CreateRollbackCommand request = new CreateRollbackCommand();
        request.setNamespace("default");
        request.setProjectName("game");
        request.setTargetVersion("v1");
        request.setConfigShard("shard-a");
        request.setCreatedBy("operator");

        ReleaseResult response = releaseService.createRollback(request);
        GatewayConfigSnapshot updatedSnapshot = snapshotService.findSnapshot("default", "game", "v1", "shard-a")
                .orElseThrow();
        CursorPage<Object> events = resourceService.list("events", "default", null, 50);

        assertThat(response.getReleaseId()).startsWith("rb-");
        assertThat(response.getVersion()).matches(ROLLBACK_VERSION_PATTERN);
        assertThat(response.getVersion()).doesNotContain(String.valueOf(response.getCreatedAt().toEpochMilli()));
        assertThat(response.getPhase()).isEqualTo("PENDING");
        assertThat(updatedSnapshot.getMetadata().getUid()).isEqualTo(snapshot.getMetadata().getUid());
        assertThat(updatedSnapshot.getStatus().getLastRollbackReleaseId()).isEqualTo(response.getReleaseId());
        assertThat(events.getItems()).hasSize(1);
    }

    private void saveProject(String namespace, String name) {
        GatewayProject project = new GatewayProject();
        GatePilotResourceType projectType = resourceService.requireResourceType(ResourceKind.GATEWAY_PROJECT);
        resourceService.save(projectType, namespace, name, project);
    }

    private void saveRouteWithMissingRefs() {
        GatewayRoute route = new GatewayRoute();
        route.getSpec().setProjectRef(projectRef("default", "game"));
        route.getSpec().getPath().setValue("/api/game");
        route.getSpec().setUpstreamRef(resourceRef(ResourceKind.UPSTREAM, "default", "missing-upstream"));
        route.getSpec().getPolicyRefs().add(resourceRef(ResourceKind.TRAFFIC_POLICY, "default", "missing-policy"));
        GatePilotResourceType routeType = resourceService.requireResourceType(ResourceKind.GATEWAY_ROUTE);
        resourceService.save(routeType, "default", "game-route", route);
    }

    private void saveEmptyUpstream() {
        Upstream upstream = new Upstream();
        upstream.getSpec().setProjectRef(projectRef("default", "game"));
        GatePilotResourceType upstreamType = resourceService.requireResourceType(ResourceKind.UPSTREAM);
        resourceService.save(upstreamType, "default", "empty-upstream", upstream);
    }

    private void saveAvailableUpstream() {
        Upstream upstream = new Upstream();
        upstream.getSpec().setProjectRef(projectRef("default", "game"));
        Upstream.UpstreamEndpoint endpoint = new Upstream.UpstreamEndpoint();
        endpoint.setHost("127.0.0.1");
        endpoint.setPort(8080);
        upstream.getSpec().getEndpoints().add(endpoint);
        GatePilotResourceType upstreamType = resourceService.requireResourceType(ResourceKind.UPSTREAM);
        resourceService.save(upstreamType, "default", "main-upstream", upstream);
    }

    private void saveUnsupportedLoadBalanceUpstream() {
        Upstream upstream = new Upstream();
        upstream.getSpec().setProjectRef(projectRef("default", "game"));
        upstream.getSpec().setLoadBalance(LoadBalanceStrategy.CONSISTENT_HASH);
        Upstream.UpstreamEndpoint endpoint = new Upstream.UpstreamEndpoint();
        endpoint.setHost("127.0.0.1");
        endpoint.setPort(8080);
        upstream.getSpec().getEndpoints().add(endpoint);
        GatePilotResourceType upstreamType = resourceService.requireResourceType(ResourceKind.UPSTREAM);
        resourceService.save(upstreamType, "default", "hash-upstream", upstream);
    }

    private void saveValidRoute(int index) {
        saveRouteForUpstream("game-route-" + index, "main-upstream");
    }

    private void saveRouteForUpstream(String routeName, String upstreamName) {
        GatewayRoute route = new GatewayRoute();
        route.getSpec().setProjectRef(projectRef("default", "game"));
        route.getSpec().getHosts().add("game.example.com");
        route.getSpec().getPath().setValue("/api/game/" + routeName);
        route.getSpec().setUpstreamRef(resourceRef(ResourceKind.UPSTREAM, "default", upstreamName));
        GatePilotResourceType routeType = resourceService.requireResourceType(ResourceKind.GATEWAY_ROUTE);
        resourceService.save(routeType, "default", routeName, route);
    }

    private ResourceReference projectRef(String namespace, String name) {
        return resourceRef(ResourceKind.GATEWAY_PROJECT, namespace, name);
    }

    private ResourceReference resourceRef(ResourceKind kind, String namespace, String name) {
        ResourceReference reference = new ResourceReference();
        reference.setKind(kind);
        reference.setNamespace(namespace);
        reference.setName(name);
        return reference;
    }

    private PublishedConfig publishedConfig(String namespace, String projectName, String version, String configShard) {
        PublishedConfig config = new PublishedConfig();
        config.getMetadata().setNamespace(namespace);
        config.getMetadata().setName(version);
        ResourceReference projectRef = new ResourceReference();
        projectRef.setNamespace(namespace);
        projectRef.setName(projectName);
        config.getSpec().setProjectRef(projectRef);
        config.getSpec().setVersion(version);
        config.getSpec().setConfigShard(configShard);
        config.getSpec().setSequence(1L);
        config.getSpec().setConfigHash("hash-" + version);
        return config;
    }
}
