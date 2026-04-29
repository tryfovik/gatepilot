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
package com.dt.gatepilot.agent.infrastructure.config;

import com.dt.gatepilot.agent.domain.port.LocalConfigStore;
import com.dt.gatepilot.agent.domain.port.ProxyApplyClient;
import com.dt.gatepilot.agent.infrastructure.persistence.file.FileLocalConfigStore;
import com.dt.gatepilot.agent.infrastructure.persistence.memory.InMemoryLocalConfigStore;
import com.dt.gatepilot.agent.infrastructure.proxy.HttpProxyApplyClient;
import com.dt.gatepilot.domain.deployment.GatePilotDeploymentModeConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.reactive.function.client.WebClient;

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

    @Test
    void shouldUseHttpProxyApplyClientWhenModeIsCluster() {
        contextRunner
                .withBean(WebClient.Builder.class, WebClient::builder)
                .withPropertyValues(GatePilotDeploymentModeConstants.CONFIG_PREFIX + "."
                        + GatePilotDeploymentModeConstants.MODE_PROPERTY + "="
                        + GatePilotDeploymentModeConstants.MODE_CLUSTER)
                .run(context -> {
                    assertThat(context).hasSingleBean(ProxyApplyClient.class);
                    assertThat(context).hasSingleBean(HttpProxyApplyClient.class);
                });
    }

    @Test
    void shouldFailFastWhenStandaloneMissesProxyApplyClient() {
        contextRunner
                .withPropertyValues(GatePilotDeploymentModeConstants.CONFIG_PREFIX + "."
                        + GatePilotDeploymentModeConstants.MODE_PROPERTY + "="
                        + GatePilotDeploymentModeConstants.MODE_STANDALONE)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining(AgentRuntimeConstants.MESSAGE_MISSING_PROXY_APPLY_CLIENT);
                });
    }
}
