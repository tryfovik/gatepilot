package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyRequest;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyResult;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyConstants;
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

    @Test
    void shouldNotSwitchRuntimeWhenGovernanceRulesPublishFailed() {
        ProxyConfigApplier applier = new ProxyConfigApplier(
                new PublishedConfigCompiler(),
                new ProxyRuntimeState(),
                runtime -> {
                    throw new IllegalStateException("sentinel unavailable");
                }
        );
        ProxyApplyRequest request = new ProxyApplyRequest();
        request.setPublishedConfig(config("v1", "hash-v1"));

        ProxyApplyResult result = applier.apply(request);

        assertThat(result.getState()).isEqualTo(ConfigApplyState.FAILED);
        assertThat(result.getReason()).isEqualTo(ProxyApplyConstants.REASON_GOVERNANCE_RULE_PUBLISH_FAILED);
        assertThat(applier.runtimeState().current()).isEmpty();
    }

    @Test
    void shouldSkipGovernancePublisherWhenDryRun() {
        ProxyConfigApplier applier = new ProxyConfigApplier(
                new PublishedConfigCompiler(),
                new ProxyRuntimeState(),
                runtime -> {
                    throw new IllegalStateException("should not publish");
                }
        );
        ProxyApplyRequest request = new ProxyApplyRequest();
        request.setDryRun(true);
        request.setPublishedConfig(config("v1", "hash-v1"));

        ProxyApplyResult result = applier.apply(request);

        assertThat(result.getState()).isEqualTo(ConfigApplyState.APPLIED);
        assertThat(applier.runtimeState().current()).isEmpty();
    }

    private PublishedConfig config(String version, String configHash) {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion(version);
        config.getSpec().setConfigHash(configHash);
        return config;
    }
}
