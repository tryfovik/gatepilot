package com.dt.gatepilot.agent.infrastructure.config;

import com.dt.gatepilot.agent.domain.port.LocalConfigStore;
import com.dt.gatepilot.agent.infrastructure.persistence.file.FileLocalConfigStore;
import com.dt.gatepilot.agent.infrastructure.persistence.memory.InMemoryLocalConfigStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GatePilot agent 自动配置测试。
 */
class GatePilotAgentAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GatePilotAgentAutoConfiguration.class);

    @Test
    void shouldUseFileLocalConfigStoreByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LocalConfigStore.class);
            assertThat(context).hasSingleBean(FileLocalConfigStore.class);
        });
    }

    @Test
    void shouldUseMemoryLocalConfigStoreWhenConfigured() {
        contextRunner
                .withPropertyValues(AgentRuntimeConstants.CONFIG_PREFIX
                        + ".local-config.store-type="
                        + AgentRuntimeConstants.LOCAL_CONFIG_STORE_TYPE_MEMORY)
                .run(context -> {
                    assertThat(context).hasSingleBean(LocalConfigStore.class);
                    assertThat(context).hasSingleBean(InMemoryLocalConfigStore.class);
                });
    }
}
