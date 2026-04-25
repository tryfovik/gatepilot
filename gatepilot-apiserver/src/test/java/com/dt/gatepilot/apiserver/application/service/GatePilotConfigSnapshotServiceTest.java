package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.domain.resource.common.ResourceReference;
import com.dt.gatepilot.domain.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.apiserver.application.dto.ConfigDiffResult;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceRegistry;
import com.dt.gatepilot.apiserver.domain.resource.ResourceMetadataSupport;
import com.dt.gatepilot.apiserver.infrastructure.persistence.memory.InMemoryGatePilotResourceStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 配置快照服务测试。
 */
class GatePilotConfigSnapshotServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GatePilotResourceService resourceService = new GatePilotResourceService(
            new GatePilotResourceRegistry(),
            new InMemoryGatePilotResourceStore(new ResourceMetadataSupport()),
            objectMapper
    );

    private final GatePilotConfigSnapshotService snapshotService =
            new GatePilotConfigSnapshotService(resourceService, objectMapper);

    @Test
    void shouldSavePublishedConfigSnapshotAndDiffVersions() {
        PublishedConfig base = publishedConfig("default", "game", "v1", "shard-a");
        base.getSpec().getRoutes().add(route("route-a", "/api/a", "upstream-a"));
        base.getSpec().getUpstreams().add(upstream("upstream-a", "10.0.0.1"));
        snapshotService.saveSnapshot(base, "rel-1", "tester", "base");

        PublishedConfig target = publishedConfig("default", "game", "v2", "shard-a");
        target.getSpec().getRoutes().add(route("route-a", "/api/a", "upstream-b"));
        target.getSpec().getRoutes().add(route("route-b", "/api/b", "upstream-b"));
        target.getSpec().getUpstreams().add(upstream("upstream-b", "10.0.0.2"));
        GatewayConfigSnapshot targetSnapshot = snapshotService.saveSnapshot(target, "rel-2", "tester", "target");

        ConfigDiffResult diff = snapshotService.diff("default", "v1", "v2", "shard-a");

        assertThat(targetSnapshot.getMetadata().getName()).isEqualTo("v2-shard-a");
        assertThat(diff.isChanged()).isTrue();
        assertThat(diff.getAddedRoutes()).isEqualTo(1);
        assertThat(diff.getChangedRoutes()).isEqualTo(1);
        assertThat(diff.getRemovedUpstreams()).isEqualTo(1);
        assertThat(diff.getAddedUpstreams()).isEqualTo(1);
        assertThat(diff.getItems())
                .extracting(ConfigDiffResult.ConfigDiffItem::getChangeType)
                .contains("ADDED", "CHANGED", "REMOVED");
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
        config.getSpec().setSequence(version.endsWith("1") ? 1L : 2L);
        config.getSpec().setConfigHash("hash-" + version);
        return config;
    }

    private PublishedConfig.PublishedRoute route(String name, String path, String upstreamName) {
        PublishedConfig.PublishedRoute route = new PublishedConfig.PublishedRoute();
        route.setRouteId(name);
        ResourceReference sourceRef = new ResourceReference();
        sourceRef.setNamespace("default");
        sourceRef.setName(name);
        route.setSourceRef(sourceRef);
        route.getProtocols().add(Protocol.HTTP);
        route.setPath(path);
        route.setUpstreamName(upstreamName);
        return route;
    }

    private PublishedConfig.PublishedUpstream upstream(String name, String host) {
        PublishedConfig.PublishedUpstream upstream = new PublishedConfig.PublishedUpstream();
        upstream.setName(name);
        upstream.setProtocol(Protocol.HTTP);
        PublishedConfig.PublishedEndpoint endpoint = new PublishedConfig.PublishedEndpoint();
        endpoint.setHost(host);
        endpoint.setPort(8080);
        endpoint.setWeight(100);
        upstream.getEndpoints().add(endpoint);
        return upstream;
    }
}
