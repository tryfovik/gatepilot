package com.dt.gatepilot.agent.application.service;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.application.dto.AgentRuntimeAuditBatch;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.agent.domain.port.PublishedConfigSyncAdapter;
import com.dt.gatepilot.agent.infrastructure.persistence.memory.InMemoryLocalConfigStore;
import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * agent 运行编排器测试。
 */
class AgentRuntimeCoordinatorTest {

    @Test
    void shouldReportFailedResultAndKeepLastGoodWhenProxyApplyThrows() {
        AgentNodeProfile profile = new AgentNodeProfile();
        profile.setNamespace("default");
        profile.setNodeId("node-1");
        InMemoryLocalConfigStore localConfigStore = new InMemoryLocalConfigStore();
        localConfigStore.promoteLastGood(config("v1", "hash-v1"));
        StubControlPlaneClient controlPlaneClient = new StubControlPlaneClient(config("v2", "hash-v2"));
        AgentRuntimeCoordinator coordinator = new AgentRuntimeCoordinator(
                profile,
                controlPlaneClient,
                localConfigStore,
                ignored -> {
                    throw new IllegalStateException("compile failed");
                }
        );

        Optional<AgentApplyResult> result = coordinator.pullAndApply();

        assertThat(result).hasValueSatisfying(applyResult -> {
            assertThat(applyResult.getState()).isEqualTo(ConfigApplyState.FAILED);
            assertThat(applyResult.getReason()).isEqualTo("ProxyApplyFailed");
            assertThat(applyResult.getVersion()).isEqualTo("v2");
        });
        assertThat(controlPlaneClient.reportedResult).isNotNull();
        assertThat(controlPlaneClient.reportedResult.getState()).isEqualTo(ConfigApplyState.FAILED);
        assertThat(localConfigStore.loadStaged()).hasValueSatisfying(config ->
                assertThat(config.getSpec().getVersion()).isEqualTo("v2"));
        assertThat(localConfigStore.loadLastGood()).hasValueSatisfying(config ->
                assertThat(config.getSpec().getVersion()).isEqualTo("v1"));
    }

    @Test
    void shouldPromoteLastGoodWhenProxyApplySucceeded() {
        AgentNodeProfile profile = new AgentNodeProfile();
        profile.setNamespace("default");
        profile.setNodeId("node-1");
        InMemoryLocalConfigStore localConfigStore = new InMemoryLocalConfigStore();
        StubControlPlaneClient controlPlaneClient = new StubControlPlaneClient(config("v2", "hash-v2"));
        AgentRuntimeCoordinator coordinator = new AgentRuntimeCoordinator(
                profile,
                controlPlaneClient,
                localConfigStore,
                config -> {
                    AgentApplyResult result = new AgentApplyResult();
                    // 成功结果只模拟 proxy 已应用状态
                    result.setState(ConfigApplyState.APPLIED);
                    return result;
                }
        );

        Optional<AgentApplyResult> result = coordinator.pullAndApply();

        assertThat(result).hasValueSatisfying(applyResult -> {
            assertThat(applyResult.getState()).isEqualTo(ConfigApplyState.APPLIED);
            assertThat(applyResult.getNodeId()).isEqualTo("node-1");
            assertThat(applyResult.getVersion()).isEqualTo("v2");
        });
        assertThat(localConfigStore.loadLastGood()).hasValueSatisfying(config ->
                assertThat(config.getSpec().getVersion()).isEqualTo("v2"));
        assertThat(controlPlaneClient.reportedResult).isNotNull();
        assertThat(controlPlaneClient.reportedResult.getState()).isEqualTo(ConfigApplyState.APPLIED);
    }

    @Test
    void shouldPullConfigThroughSyncAdapter() {
        AgentNodeProfile profile = new AgentNodeProfile();
        profile.setNamespace("default");
        profile.setNodeId("node-1");
        InMemoryLocalConfigStore localConfigStore = new InMemoryLocalConfigStore();
        StubControlPlaneClient controlPlaneClient = new StubControlPlaneClient(null);
        PublishedConfigSyncAdapter syncAdapter = cursor -> Optional.of(config("v3", "hash-v3"));
        AgentRuntimeCoordinator coordinator = new AgentRuntimeCoordinator(
                profile,
                controlPlaneClient,
                syncAdapter,
                localConfigStore,
                config -> {
                    AgentApplyResult result = new AgentApplyResult();
                    // 成功结果只模拟 proxy 已应用状态
                    result.setState(ConfigApplyState.APPLIED);
                    return result;
                },
                null
        );

        Optional<AgentApplyResult> result = coordinator.pullAndApply();

        assertThat(result).hasValueSatisfying(applyResult ->
                assertThat(applyResult.getVersion()).isEqualTo("v3"));
        assertThat(controlPlaneClient.pullCount).isZero();
        assertThat(controlPlaneClient.reportedResult).isNotNull();
        assertThat(controlPlaneClient.reportedResult.getVersion()).isEqualTo("v3");
    }

    private PublishedConfig config(String version, String configHash) {
        PublishedConfig config = new PublishedConfig();
        // 测试配置只填 agent 游标和 apply 必需字段
        config.getSpec().setVersion(version);
        config.getSpec().setConfigHash(configHash);
        config.getSpec().setSequence(version.endsWith("1") ? 1L : 2L);
        return config;
    }

    private static class StubControlPlaneClient implements AgentControlPlaneClient {

        private final PublishedConfig config;

        private AgentApplyResult reportedResult;

        private int pullCount;

        StubControlPlaneClient(PublishedConfig config) {
            this.config = config;
        }

        @Override
        public void register(AgentNodeProfile profile) {
        }

        @Override
        public void heartbeat(AgentHeartbeatSnapshot heartbeat) {
        }

        @Override
        public Optional<PublishedConfig> pullConfig(AgentConfigCursor cursor) {
            pullCount++;
            return Optional.of(config);
        }

        @Override
        public void reportApplyResult(AgentApplyResult result) {
            this.reportedResult = result;
        }

        @Override
        public void reportRuntimeAudits(AgentRuntimeAuditBatch batch) {
            // 当前测试只关心配置应用结果
        }
    }
}
