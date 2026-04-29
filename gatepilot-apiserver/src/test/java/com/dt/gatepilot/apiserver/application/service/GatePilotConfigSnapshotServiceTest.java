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
package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.apiserver.application.dto.ConfigDiffResult;
import com.dt.gatepilot.apiserver.application.dto.ConfigSnapshotSummaryResponse;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
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

    @Test
    void shouldListSnapshotSummariesWithoutPublishedConfigPayload() {
        PublishedConfig config = publishedConfig("default", "game", "v1", "shard-a");
        config.getSpec().getRoutes().add(route("route-a", "/api/a", "upstream-a"));
        config.getSpec().getUpstreams().add(upstream("upstream-a", "10.0.0.1"));
        snapshotService.saveSnapshot(config, "rel-1", "tester", "base");

        CursorPage<ConfigSnapshotSummaryResponse> summaries =
                snapshotService.listSummaries("default", "game", "shard-a", null, 50);

        assertThat(summaries.getItems()).hasSize(1);
        assertThat(summaries.getItems().get(0).getVersion()).isEqualTo("v1");
        assertThat(summaries.getItems().get(0).getRouteCount()).isEqualTo(1);
        assertThat(summaries.getItems().get(0).getUpstreamCount()).isEqualTo(1);
        assertThat(summaries.getItems().get(0).getReleaseId()).isEqualTo("rel-1");
    }

    @Test
    void shouldKeepSnapshotPayloadImmutableAfterSourceConfigChanged() {
        PublishedConfig config = publishedConfig("default", "game", "v1", "shard-a");
        config.getSpec().getRoutes().add(route("route-a", "/api/a", "upstream-a"));
        config.getSpec().getUpstreams().add(upstream("upstream-a", "10.0.0.1"));

        GatewayConfigSnapshot snapshot = snapshotService.saveSnapshot(config, "rel-1", "tester", "base");
        config.getMetadata().setName("mutated");
        config.getSpec().getProjectRef().setName("mutated-project");
        config.getSpec().getRoutes().get(0).setPath("/api/mutated");
        config.getSpec().getUpstreams().get(0).getEndpoints().get(0).setHost("10.0.0.9");

        GatewayConfigSnapshot reloaded = snapshotService.findSnapshot("default", "game", "v1", "shard-a")
                .orElseThrow();
        PublishedConfig snapshotConfig = reloaded.getSpec().getPublishedConfig();

        assertThat(snapshot.getSpec().getPublishedConfig()).isNotSameAs(config);
        assertThat(reloaded.getSpec().getProjectRef().getName()).isEqualTo("game");
        assertThat(snapshotConfig.getMetadata().getName()).isEqualTo("v1");
        assertThat(snapshotConfig.getSpec().getProjectRef().getName()).isEqualTo("game");
        assertThat(snapshotConfig.getSpec().getRoutes().get(0).getPath()).isEqualTo("/api/a");
        assertThat(snapshotConfig.getSpec().getUpstreams().get(0).getEndpoints().get(0).getHost())
                .isEqualTo("10.0.0.1");
    }

    @Test
    void shouldPageSnapshotSummariesAfterProjectFilter() {
        snapshotService.saveSnapshot(publishedConfig("default", "order", "v1", "shard-a"),
                "rel-1", "tester", "order");
        snapshotService.saveSnapshot(publishedConfig("default", "game", "v2", "shard-a"),
                "rel-2", "tester", "game first");
        snapshotService.saveSnapshot(publishedConfig("default", "game", "v3", "shard-a"),
                "rel-3", "tester", "game second");

        CursorPage<ConfigSnapshotSummaryResponse> firstPage =
                snapshotService.listSummaries("default", "game", "shard-a", null, 1);
        CursorPage<ConfigSnapshotSummaryResponse> secondPage =
                snapshotService.listSummaries("default", "game", "shard-a", firstPage.getNextCursor(), 1);

        assertThat(firstPage.getItems()).hasSize(1);
        assertThat(firstPage.getItems().get(0).getVersion()).isEqualTo("v2");
        assertThat(firstPage.getTotal()).isEqualTo(2);
        assertThat(firstPage.getNextCursor()).isEqualTo("default/v2-shard-a");
        assertThat(secondPage.getItems()).hasSize(1);
        assertThat(secondPage.getItems().get(0).getVersion()).isEqualTo("v3");
        assertThat(secondPage.getNextCursor()).isNull();
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
