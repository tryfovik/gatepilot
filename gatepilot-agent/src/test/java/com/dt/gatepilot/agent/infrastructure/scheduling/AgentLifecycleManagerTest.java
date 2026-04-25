package com.dt.gatepilot.agent.infrastructure.scheduling;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.application.dto.AgentRuntimeAuditBatch;
import com.dt.gatepilot.agent.application.service.AgentRuntimeCoordinator;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.agent.domain.port.ProxyApplyClient;
import com.dt.gatepilot.agent.infrastructure.config.GatePilotAgentProperties;
import com.dt.gatepilot.agent.infrastructure.persistence.memory.InMemoryLocalConfigStore;
import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * agent 生命周期调度器测试。
 */
class AgentLifecycleManagerTest {

    @Test
    void shouldRegisterAndStartWithLastGoodOnStartup() {
        StubControlPlaneClient controlPlaneClient = new StubControlPlaneClient(Optional.empty());
        InMemoryLocalConfigStore localConfigStore = new InMemoryLocalConfigStore();
        localConfigStore.promoteLastGood(config());
        CountingProxyApplyClient proxyApplyClient = new CountingProxyApplyClient();
        AgentRuntimeCoordinator coordinator = new AgentRuntimeCoordinator(
                profile(), controlPlaneClient, localConfigStore, proxyApplyClient);
        GatePilotAgentProperties properties = new GatePilotAgentProperties();
        AgentLifecycleManager manager = new AgentLifecycleManager(coordinator, properties);

        manager.run(new DefaultApplicationArguments());

        assertThat(controlPlaneClient.registerCount).isEqualTo(1);
        assertThat(proxyApplyClient.applyCount).isEqualTo(1);
    }

    @Test
    void shouldSkipDisabledLifecycle() {
        StubControlPlaneClient controlPlaneClient = new StubControlPlaneClient(Optional.of(config()));
        InMemoryLocalConfigStore localConfigStore = new InMemoryLocalConfigStore();
        localConfigStore.promoteLastGood(config());
        CountingProxyApplyClient proxyApplyClient = new CountingProxyApplyClient();
        AgentRuntimeCoordinator coordinator = new AgentRuntimeCoordinator(
                profile(), controlPlaneClient, localConfigStore, proxyApplyClient);
        GatePilotAgentProperties properties = new GatePilotAgentProperties();
        properties.setLifecycleEnabled(false);
        AgentLifecycleManager manager = new AgentLifecycleManager(coordinator, properties);

        manager.run(new DefaultApplicationArguments());
        manager.pullAndApply();
        manager.heartbeat();

        assertThat(controlPlaneClient.registerCount).isZero();
        assertThat(controlPlaneClient.pullCount).isZero();
        assertThat(controlPlaneClient.heartbeatCount).isZero();
        assertThat(proxyApplyClient.applyCount).isZero();
    }

    @Test
    void shouldTriggerPullAndHeartbeatWhenEnabled() {
        StubControlPlaneClient controlPlaneClient = new StubControlPlaneClient(Optional.of(config()));
        CountingProxyApplyClient proxyApplyClient = new CountingProxyApplyClient();
        AgentRuntimeCoordinator coordinator = new AgentRuntimeCoordinator(
                profile(), controlPlaneClient, new InMemoryLocalConfigStore(), proxyApplyClient);
        GatePilotAgentProperties properties = new GatePilotAgentProperties();
        AgentLifecycleManager manager = new AgentLifecycleManager(coordinator, properties);

        manager.pullAndApply();
        manager.heartbeat();

        assertThat(controlPlaneClient.pullCount).isEqualTo(1);
        assertThat(controlPlaneClient.heartbeatCount).isEqualTo(1);
        assertThat(controlPlaneClient.reportCount).isEqualTo(1);
        assertThat(proxyApplyClient.applyCount).isEqualTo(1);
    }

    private AgentNodeProfile profile() {
        AgentNodeProfile profile = new AgentNodeProfile();
        // 测试节点只保留调度链路需要的身份
        profile.setNamespace("default");
        profile.setNodeId("node-1");
        return profile;
    }

    private PublishedConfig config() {
        PublishedConfig config = new PublishedConfig();
        // 测试配置只填 apply 必需字段
        config.getSpec().setVersion("v1");
        config.getSpec().setConfigHash("hash-v1");
        return config;
    }

    private static class StubControlPlaneClient implements AgentControlPlaneClient {

        private final Optional<PublishedConfig> config;

        private int registerCount;

        private int heartbeatCount;

        private int pullCount;

        private int reportCount;

        StubControlPlaneClient(Optional<PublishedConfig> config) {
            this.config = config;
        }

        @Override
        public void register(AgentNodeProfile profile) {
            registerCount++;
        }

        @Override
        public void heartbeat(AgentHeartbeatSnapshot heartbeat) {
            heartbeatCount++;
        }

        @Override
        public Optional<PublishedConfig> pullConfig(AgentConfigCursor cursor) {
            pullCount++;
            return config;
        }

        @Override
        public void reportApplyResult(AgentApplyResult result) {
            reportCount++;
        }

        @Override
        public void reportRuntimeAudits(AgentRuntimeAuditBatch batch) {
            // 当前测试只关心生命周期主链路
        }
    }

    private static class CountingProxyApplyClient implements ProxyApplyClient {

        private int applyCount;

        @Override
        public AgentApplyResult apply(PublishedConfig config) {
            applyCount++;
            AgentApplyResult result = new AgentApplyResult();
            // proxy stub 只表达成功应用
            result.setState(ConfigApplyState.APPLIED);
            return result;
        }
    }
}
