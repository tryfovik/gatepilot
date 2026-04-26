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
