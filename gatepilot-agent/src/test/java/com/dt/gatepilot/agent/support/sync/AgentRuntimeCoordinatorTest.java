package com.dt.gatepilot.agent.support.sync;

import com.dt.gatepilot.agent.api.AgentApplyResult;
import com.dt.gatepilot.agent.api.AgentConfigCursor;
import com.dt.gatepilot.agent.api.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.api.AgentNodeProfile;
import com.dt.gatepilot.agent.spi.AgentControlPlaneClient;
import com.dt.gatepilot.agent.support.store.InMemoryLocalConfigStore;
import com.dt.gatepilot.api.enums.ConfigApplyState;
import com.dt.gatepilot.api.resource.publish.PublishedConfig;
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

    private PublishedConfig config(String version, String configHash) {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion(version);
        config.getSpec().setConfigHash(configHash);
        config.getSpec().setSequence(version.endsWith("1") ? 1L : 2L);
        return config;
    }

    private static class StubControlPlaneClient implements AgentControlPlaneClient {

        private final PublishedConfig config;

        private AgentApplyResult reportedResult;

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
            return Optional.of(config);
        }

        @Override
        public void reportApplyResult(AgentApplyResult result) {
            this.reportedResult = result;
        }
    }
}
