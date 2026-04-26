package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.node.GatewayNodeStatus;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.apiserver.application.command.AgentHeartbeatCommand;
import com.dt.gatepilot.apiserver.application.command.PullAgentConfigCommand;
import com.dt.gatepilot.apiserver.application.dto.AgentConfigPullResult;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceRegistry;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceType;
import com.dt.gatepilot.apiserver.domain.resource.ResourceMetadataSupport;
import com.dt.gatepilot.apiserver.infrastructure.persistence.memory.InMemoryGatePilotResourceStore;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * agent 协议服务测试。
 */
class GatePilotAgentServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GatePilotResourceService resourceService = new GatePilotResourceService(
            new GatePilotResourceRegistry(),
            new InMemoryGatePilotResourceStore(new ResourceMetadataSupport()),
            objectMapper
    );

    private final GatePilotAgentService agentService = new GatePilotAgentService(resourceService);

    @Test
    void shouldPersistHealthMetricsAndUpstreamHealthFromHeartbeat() {
        AgentHeartbeatCommand request = new AgentHeartbeatCommand();
        request.setNamespace("default");
        request.setNodeId("node-1");
        GatewayNodeStatus.NodeHealth health = new GatewayNodeStatus.NodeHealth();
        health.setAgentHealthy(true);
        health.setProxyHealthy(true);
        health.setControlPlaneConnected(true);
        health.setMessage("ready");
        request.setHealth(health);
        GatewayNodeStatus.MetricsSummary metrics = new GatewayNodeStatus.MetricsSummary();
        metrics.setRequestsPerSecond(1200.5);
        metrics.setErrorRate(0.01);
        metrics.setP95LatencyMillis(18.2);
        metrics.setActiveConnections(88L);
        request.setMetricsSummary(metrics);
        request.getMetrics().put("cpu", "0.42");
        GatewayNodeStatus.UpstreamHealth upstreamHealth = new GatewayNodeStatus.UpstreamHealth();
        upstreamHealth.setUpstreamName("order-service");
        upstreamHealth.setHealthyEndpointCount(3);
        upstreamHealth.setUnhealthyEndpointCount(1);
        upstreamHealth.setCheckedAt(Instant.now());
        request.getUpstreamHealth().add(upstreamHealth);

        GatewayNode node = agentService.heartbeat(request);

        assertThat(node.getStatus().getHealth().getMessage()).isEqualTo("ready");
        assertThat(node.getStatus().getMetrics().getRequestsPerSecond()).isEqualTo(1200.5);
        assertThat(node.getStatus().getMetrics().getCustom()).containsEntry("cpu", "0.42");
        assertThat(node.getStatus().getUpstreamHealth())
                .extracting(GatewayNodeStatus.UpstreamHealth::getUpstreamName)
                .containsExactly("order-service");
    }

    @Test
    void shouldPullPublishedConfigBeyondFirstResourcePage() {
        GatePilotResourceType resourceType = resourceService.requireResourceType(ResourceKind.PUBLISHED_CONFIG);
        for (int index = 0; index < 600; index++) {
            String name = "a-config-" + String.format("%03d", index);
            resourceService.save(resourceType, "default", name, publishedConfig("old-" + index, index, "shard-a"));
        }
        resourceService.save(resourceType, "default", "z-target", publishedConfig("v-target", 1000L, "shard-a"));
        PullAgentConfigCommand request = new PullAgentConfigCommand();
        request.setNamespace("default");
        request.setNodeId("node-1");
        request.getConfigShards().add("shard-a");

        AgentConfigPullResult result = agentService.pullConfig(request);

        assertThat(result.isChanged()).isTrue();
        assertThat(result.getPublishedConfig().getSpec().getVersion()).isEqualTo("v-target");
    }

    private PublishedConfig publishedConfig(String version, long sequence, String configShard) {
        PublishedConfig config = new PublishedConfig();
        // 测试配置只填 agent 拉取排序和过滤字段
        config.getSpec().setVersion(version);
        config.getSpec().setSequence(sequence);
        config.getSpec().setConfigShard(configShard);
        config.getSpec().setConfigHash("hash-" + version);
        return config;
    }
}
