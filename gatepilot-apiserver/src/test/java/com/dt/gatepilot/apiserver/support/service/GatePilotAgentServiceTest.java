package com.dt.gatepilot.apiserver.support.service;

import com.dt.gatepilot.api.resource.node.GatewayNode;
import com.dt.gatepilot.api.resource.node.GatewayNodeStatus;
import com.dt.gatepilot.apiserver.api.request.AgentHeartbeatRequest;
import com.dt.gatepilot.apiserver.support.resource.GatePilotResourceRegistry;
import com.dt.gatepilot.apiserver.support.resource.ResourceMetadataSupport;
import com.dt.gatepilot.apiserver.support.store.InMemoryGatePilotResourceStore;
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
        AgentHeartbeatRequest request = new AgentHeartbeatRequest();
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
}
