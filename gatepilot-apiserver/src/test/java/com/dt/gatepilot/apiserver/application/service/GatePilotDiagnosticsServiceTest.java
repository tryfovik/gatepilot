package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.apiserver.application.dto.RouteCatalogResponse;
import com.dt.gatepilot.apiserver.application.dto.RouteDiagnosticsRequest;
import com.dt.gatepilot.apiserver.application.dto.RouteDiagnosticsResponse;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceRegistry;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceType;
import com.dt.gatepilot.apiserver.domain.resource.ResourceMetadataSupport;
import com.dt.gatepilot.apiserver.infrastructure.persistence.memory.InMemoryGatePilotResourceStore;
import com.dt.gatepilot.domain.enums.AuthType;
import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.enums.HttpMethod;
import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.enums.TrafficColorSource;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.publish.PublishedConfigConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GatePilot 诊断服务测试。
 */
class GatePilotDiagnosticsServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GatePilotResourceService resourceService = new GatePilotResourceService(
            new GatePilotResourceRegistry(),
            new InMemoryGatePilotResourceStore(new ResourceMetadataSupport()),
            objectMapper
    );

    private final GatePilotDiagnosticsService diagnosticsService = new GatePilotDiagnosticsService(resourceService);

    @Test
    void shouldBuildRouteCatalogFromLatestPublishedConfig() {
        savePublishedConfig(publishedConfig("default", "game", "v1", 1L));
        PublishedConfig latest = publishedConfig("default", "game", "v2", 2L);
        latest.getSpec().getRoutes().add(route("orders-route", "/api/orders", "orders-stable"));
        latest.getSpec().getUpstreams().add(upstream("orders-stable", "10.0.0.1"));
        PublishedConfig.NodeApplyResult applyResult = new PublishedConfig.NodeApplyResult();
        applyResult.setNodeId("node-1");
        applyResult.setZone("az-a");
        applyResult.setState(ConfigApplyState.APPLIED);
        applyResult.setAppliedVersion("v2");
        latest.getStatus().getNodeApplyResults().add(applyResult);
        savePublishedConfig(latest);

        RouteCatalogResponse catalog = diagnosticsService.catalog("default", "game", null, "shard-a");

        assertThat(catalog.getVersion()).isEqualTo("v2");
        assertThat(catalog.getRouteCount()).isEqualTo(1);
        assertThat(catalog.getRoutes().get(0).isUpstreamAvailable()).isTrue();
        assertThat(catalog.getNodeApplyResults())
                .extracting(RouteCatalogResponse.NodeApplyView::getNodeId)
                .containsExactly("node-1");
    }

    @Test
    void shouldDiagnoseRouteTrafficColorAndReleaseUpstream() {
        PublishedConfig config = publishedConfig("default", "game", "v1", 1L);
        PublishedConfig.PublishedRoute route = route("orders-route", "/api/orders", "orders-stable");
        route.getHosts().add("api.example.com");
        route.getPolicyNames().addAll(List.of("traffic-main", "release-main", "auth-main"));
        config.getSpec().getRoutes().add(route);
        config.getSpec().getUpstreams().add(upstream("orders-stable", "10.0.0.1"));
        config.getSpec().getUpstreams().add(upstream("orders-canary", "10.0.0.2"));
        config.getSpec().getPolicies().add(trafficPolicy());
        config.getSpec().getPolicies().add(releasePolicy());
        config.getSpec().getPolicies().add(authPolicy());
        savePublishedConfig(config);
        RouteDiagnosticsRequest request = new RouteDiagnosticsRequest();
        request.setNamespace("default");
        request.setProjectName("game");
        request.setMethod("GET");
        request.setPath("https://api.example.com/api/orders/1?debug=1");
        request.getHeaders().put("X-Canary", List.of("true"));

        RouteDiagnosticsResponse result = diagnosticsService.diagnose(request);

        assertThat(result.isMatched()).isTrue();
        assertThat(result.getTraffic().getColor()).isEqualTo("canary");
        assertThat(result.getTraffic().getReleaseTarget()).isEqualTo("canary");
        assertThat(result.getUpstream().getName()).isEqualTo("orders-canary");
        assertThat(result.getAccess().isAuthenticationRequired()).isTrue();
        assertThat(result.getWarnings()).contains(GatePilotDiagnosticsConstants.WARNING_AUTH_REQUIRED);
    }

    private void savePublishedConfig(PublishedConfig config) {
        GatePilotResourceType type = resourceService.requireResourceType(ResourceKind.PUBLISHED_CONFIG);
        // 测试直接保存已发布配置，模拟 controller-manager 写回结果
        resourceService.save(type, config.getMetadata().getNamespace(), config.getMetadata().getName(), config);
    }

    private PublishedConfig publishedConfig(String namespace, String projectName, String version, Long sequence) {
        PublishedConfig config = new PublishedConfig();
        config.getMetadata().setNamespace(namespace);
        config.getMetadata().setName(version + "-shard-a");
        ResourceReference projectRef = new ResourceReference();
        projectRef.setKind(ResourceKind.GATEWAY_PROJECT);
        projectRef.setNamespace(namespace);
        projectRef.setName(projectName);
        config.getSpec().setProjectRef(projectRef);
        config.getSpec().setVersion(version);
        config.getSpec().setConfigShard("shard-a");
        config.getSpec().setSequence(sequence);
        config.getSpec().setConfigHash("hash-" + version);
        config.getSpec().setGeneratedAt(Instant.now().plusSeconds(sequence));
        return config;
    }

    private PublishedConfig.PublishedRoute route(String name, String path, String upstreamName) {
        PublishedConfig.PublishedRoute route = new PublishedConfig.PublishedRoute();
        route.setRouteId(name);
        ResourceReference sourceRef = new ResourceReference();
        sourceRef.setKind(ResourceKind.GATEWAY_ROUTE);
        sourceRef.setNamespace("default");
        sourceRef.setName(name);
        route.setSourceRef(sourceRef);
        route.getProtocols().add(Protocol.HTTP);
        route.getMethods().add(HttpMethod.GET);
        route.setPath(path);
        route.setUpstreamName(upstreamName);
        return route;
    }

    private PublishedConfig.PublishedUpstream upstream(String name, String host) {
        PublishedConfig.PublishedUpstream upstream = new PublishedConfig.PublishedUpstream();
        upstream.setName(name);
        upstream.setProtocol(Protocol.HTTP);
        upstream.setLoadBalance("ROUND_ROBIN");
        PublishedConfig.PublishedEndpoint endpoint = new PublishedConfig.PublishedEndpoint();
        endpoint.setHost(host);
        endpoint.setPort(8080);
        endpoint.setWeight(100);
        upstream.getEndpoints().add(endpoint);
        return upstream;
    }

    private PublishedConfig.PublishedPolicy trafficPolicy() {
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName("traffic-main");
        policy.setType(PublishedConfigConstants.POLICY_TYPE_TRAFFIC);
        TrafficPolicy.TrafficColorRule rule = new TrafficPolicy.TrafficColorRule();
        rule.setSource(TrafficColorSource.HEADER);
        rule.setKey("X-Canary");
        rule.setMatch("true");
        rule.setColor("canary");
        TrafficPolicy.RetryPolicy retry = new TrafficPolicy.RetryPolicy();
        retry.setEnabled(true);
        retry.setMaxAttempts(2);
        retry.getStatuses().add(502);
        policy.getConfig().put(PublishedConfigConstants.KEY_COLOR_RULES, List.of(rule));
        policy.getConfig().put(PublishedConfigConstants.KEY_RETRY, retry);
        return policy;
    }

    private PublishedConfig.PublishedPolicy releasePolicy() {
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName("release-main");
        policy.setType(PublishedConfigConstants.POLICY_TYPE_RELEASE);
        ReleasePolicy.TrafficSplit split = new ReleasePolicy.TrafficSplit();
        split.setTarget("canary");
        split.setColor("canary");
        split.setWeight(100);
        ResourceReference upstreamRef = new ResourceReference();
        upstreamRef.setKind(ResourceKind.UPSTREAM);
        upstreamRef.setNamespace("default");
        upstreamRef.setName("orders-canary");
        split.setUpstreamRef(upstreamRef);
        policy.getConfig().put(PublishedConfigConstants.KEY_TRAFFIC_SPLITS, List.of(split));
        return policy;
    }

    private PublishedConfig.PublishedPolicy authPolicy() {
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName("auth-main");
        policy.setType(PublishedConfigConstants.POLICY_TYPE_AUTH);
        policy.getConfig().put(PublishedConfigConstants.KEY_TYPE, AuthType.JWT);
        policy.getConfig().put(PublishedConfigConstants.KEY_ANONYMOUS_ALLOWED, false);
        return policy;
    }
}
