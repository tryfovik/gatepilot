package com.dt.gatepilot.proxy.support.runtime;

import com.dt.gatepilot.api.enums.ConfigApplyState;
import com.dt.gatepilot.api.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.api.ProxyApplyRequest;
import com.dt.gatepilot.proxy.api.ProxyApplyResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * proxy 配置应用器测试。
 */
class ProxyConfigApplierTest {

    @Test
    void shouldKeepCurrentRuntimeWhenApplyFailed() {
        ProxyConfigApplier applier = new ProxyConfigApplier();
        ProxyApplyRequest goodRequest = new ProxyApplyRequest();
        goodRequest.setPublishedConfig(config("v1", "hash-v1"));
        ProxyApplyResult applied = applier.apply(goodRequest);

        ProxyApplyRequest badRequest = new ProxyApplyRequest();
        badRequest.setPublishedConfig(config("v2", null));
        ProxyApplyResult failed = applier.apply(badRequest);

        assertThat(applied.getState()).isEqualTo(ConfigApplyState.APPLIED);
        assertThat(failed.getState()).isEqualTo(ConfigApplyState.FAILED);
        assertThat(applier.runtimeState().snapshot()).hasValueSatisfying(snapshot -> {
            assertThat(snapshot.getVersion()).isEqualTo("v1");
            assertThat(snapshot.getConfigHash()).isEqualTo("hash-v1");
        });
    }

    private PublishedConfig config(String version, String configHash) {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion(version);
        config.getSpec().setConfigHash(configHash);
        return config;
    }
}
