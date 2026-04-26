package com.dt.gatepilot.embedded.infrastructure.config;

import com.dt.gatepilot.agent.domain.port.ProxyApplyClient;
import com.dt.gatepilot.agent.application.service.AgentRuntimeCoordinator;
import com.dt.gatepilot.agent.infrastructure.config.AgentRuntimeConstants;
import com.dt.gatepilot.agent.infrastructure.config.GatePilotAgentAutoConfiguration;
import com.dt.gatepilot.agent.infrastructure.scheduling.AgentLifecycleManager;
import com.dt.gatepilot.domain.deployment.GatePilotDeploymentModeConstants;
import com.dt.gatepilot.embedded.infrastructure.assembly.GatePilotEmbeddedAssemblyConfiguration;
import com.dt.gatepilot.embedded.infrastructure.controller.GatePilotControllerResourceAdapter;
import com.dt.gatepilot.proxy.domain.runtime.ProxyConfigApplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 嵌入式适配部署模式测试。
 */
class GatePilotEmbeddedSwitchTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void shouldDisableInProcessAssemblyWhenModeIsCluster() {
        contextRunner
                .withUserConfiguration(GatePilotEmbeddedAssemblyConfiguration.class)
                .withBean(ProxyConfigApplier.class, ProxyConfigApplier::new)
                .withPropertyValues(GatePilotDeploymentModeConstants.CONFIG_PREFIX + "."
                        + GatePilotDeploymentModeConstants.MODE_PROPERTY + "="
                        + GatePilotDeploymentModeConstants.MODE_CLUSTER)
                .run(context -> assertThat(context).doesNotHaveBean(ProxyApplyClient.class));
    }

    @Test
    void shouldDisableInProcessAssemblyWhenModeMissing() {
        contextRunner
                .withUserConfiguration(GatePilotEmbeddedAssemblyConfiguration.class)
                .withBean(ProxyConfigApplier.class, ProxyConfigApplier::new)
                .run(context -> assertThat(context).doesNotHaveBean(ProxyApplyClient.class));
    }

    @Test
    void shouldEnableInProcessAssemblyWhenModeIsStandalone() {
        contextRunner
                .withUserConfiguration(GatePilotEmbeddedAssemblyConfiguration.class)
                .withBean(ProxyConfigApplier.class, ProxyConfigApplier::new)
                .withPropertyValues(GatePilotDeploymentModeConstants.CONFIG_PREFIX + "."
                        + GatePilotDeploymentModeConstants.MODE_PROPERTY + "="
                        + GatePilotDeploymentModeConstants.MODE_STANDALONE)
                .run(context -> assertThat(context).hasSingleBean(ProxyApplyClient.class));
    }

    @Test
    void shouldCreateAgentCoordinatorAfterEmbeddedProxyClientInStandalone() {
        contextRunner
                .withUserConfiguration(GatePilotAgentAutoConfiguration.class,
                        GatePilotEmbeddedAssemblyConfiguration.class)
                .withBean(WebClient.Builder.class, WebClient::builder)
                .withBean(ProxyConfigApplier.class, ProxyConfigApplier::new)
                .withPropertyValues(
                        GatePilotDeploymentModeConstants.CONFIG_PREFIX + "."
                                + GatePilotDeploymentModeConstants.MODE_PROPERTY + "="
                                + GatePilotDeploymentModeConstants.MODE_STANDALONE,
                        AgentRuntimeConstants.CONFIG_PREFIX + ".lifecycle-enabled=false",
                        AgentRuntimeConstants.CONFIG_PREFIX + ".local-config.store-type="
                                + AgentRuntimeConstants.LOCAL_CONFIG_STORE_TYPE_MEMORY
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ProxyApplyClient.class);
                    assertThat(context).hasSingleBean(AgentRuntimeCoordinator.class);
                    assertThat(context).hasSingleBean(AgentLifecycleManager.class);
                });
    }

    @Test
    void shouldDisableControllerAdapterWhenModeIsCluster() {
        contextRunner
                .withUserConfiguration(GatePilotControllerResourceAdapter.class)
                .withPropertyValues(GatePilotDeploymentModeConstants.CONFIG_PREFIX + "."
                        + GatePilotDeploymentModeConstants.MODE_PROPERTY + "="
                        + GatePilotDeploymentModeConstants.MODE_CLUSTER)
                .run(context -> assertThat(context).doesNotHaveBean(GatePilotControllerResourceAdapter.class));
    }
}
