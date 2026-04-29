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
package com.dt.gatepilot.proxy.interfaces.control;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyResult;
import com.dt.gatepilot.proxy.domain.runtime.ProxyConfigApplier;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.getboot.web.api.response.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * proxy runtime control API 测试
 */
class ProxyRuntimeControlControllerTest {

    @Test
    void shouldApplyPublishedConfigThroughRuntimeApplier() {
        ProxyRuntimeState runtimeState = new ProxyRuntimeState();
        ProxyRuntimeControlController controller =
                new ProxyRuntimeControlController(new ProxyConfigApplier(
                        new com.dt.gatepilot.proxy.domain.runtime.PublishedConfigCompiler(), runtimeState));

        ApiResponse<ProxyApplyResult> response = controller.applyConfig(config("v1", "hash-v1")).block();

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getState()).isEqualTo(ConfigApplyState.APPLIED);
        assertThat(runtimeState.current()).hasValueSatisfying(runtime ->
                assertThat(runtime.getVersion()).isEqualTo("v1"));
    }

    private PublishedConfig config(String version, String configHash) {
        PublishedConfig config = new PublishedConfig();
        // 测试配置只填 proxy apply 必需字段
        config.getSpec().setVersion(version);
        config.getSpec().setConfigHash(configHash);
        return config;
    }
}
