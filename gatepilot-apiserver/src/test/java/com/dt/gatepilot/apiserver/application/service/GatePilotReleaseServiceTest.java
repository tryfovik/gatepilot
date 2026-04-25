package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.domain.resource.project.GatewayProject;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.apiserver.application.command.CreateReleaseCommand;
import com.dt.gatepilot.apiserver.application.command.CreateRollbackCommand;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.application.dto.ReleaseDryRunResult;
import com.dt.gatepilot.apiserver.application.dto.ReleaseResult;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceRegistry;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceType;
import com.dt.gatepilot.apiserver.domain.resource.ResourceMetadataSupport;
import com.dt.gatepilot.apiserver.infrastructure.persistence.memory.InMemoryGatePilotResourceStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 发布请求服务测试。
 */
class GatePilotReleaseServiceTest {

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
        assertThat(events.getItems()).isEmpty();
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
