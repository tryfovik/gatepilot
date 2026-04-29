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
package com.dt.gatepilot.embedded.infrastructure.assembly;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.domain.runtime.ProxyConfigApplier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 进程内 proxy apply 客户端测试。
 */
class InProcessProxyApplyClientTest {

    @Test
    void shouldMapProxyApplyResultToAgentApplyResult() {
        InProcessProxyApplyClient client = new InProcessProxyApplyClient(new ProxyConfigApplier());

        AgentApplyResult result = client.apply(config("v1", "hash-v1"));

        assertThat(result.getState()).isEqualTo(ConfigApplyState.APPLIED);
        assertThat(result.getVersion()).isEqualTo("v1");
        assertThat(result.getConfigHash()).isEqualTo("hash-v1");
    }

    @Test
    void shouldReturnFailedResultWhenProxyRejectsConfig() {
        InProcessProxyApplyClient client = new InProcessProxyApplyClient(new ProxyConfigApplier());

        AgentApplyResult result = client.apply(config("v2", null));

        assertThat(result.getState()).isEqualTo(ConfigApplyState.FAILED);
        assertThat(result.getVersion()).isEqualTo("v2");
    }

    private PublishedConfig config(String version, String configHash) {
        PublishedConfig config = new PublishedConfig();
        // 测试只关心 apply 必需字段
        config.getSpec().setVersion(version);
        config.getSpec().setConfigHash(configHash);
        return config;
    }
}
