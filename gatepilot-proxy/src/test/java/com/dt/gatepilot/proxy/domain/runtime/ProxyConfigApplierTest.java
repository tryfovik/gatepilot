package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyRequest;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyResult;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyConstants;
import com.dt.gatepilot.proxy.domain.port.UpstreamDiscoveryRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

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

    @Test
    void shouldCompileLargeRouteTableAndSwitchRuntimeAtomically() {
        ProxyConfigApplier applier = new ProxyConfigApplier();
        ProxyApplyRequest firstRequest = new ProxyApplyRequest();
        firstRequest.setPublishedConfig(largeRouteConfig("v1", "hash-v1", "/api/service-", 3000));
        ProxyApplyRequest secondRequest = new ProxyApplyRequest();
        secondRequest.setPublishedConfig(largeRouteConfig("v2", "hash-v2", "/api/new-service-", 3000));

        ProxyApplyResult firstResult = assertTimeout(Duration.ofSeconds(10), () -> applier.apply(firstRequest));
        ProxyApplyResult secondResult = assertTimeout(Duration.ofSeconds(10), () -> applier.apply(secondRequest));

        assertThat(firstResult.getState()).isEqualTo(ConfigApplyState.APPLIED);
        assertThat(secondResult.getState()).isEqualTo(ConfigApplyState.APPLIED);
        assertThat(applier.runtimeState().current()).hasValueSatisfying(runtime -> {
            assertThat(runtime.getVersion()).isEqualTo("v2");
            assertThat(runtime.getRoutes()).hasSize(3000);
            assertThat(runtime.match("tenant-2999.example.com", "/api/new-service-2999/orders")
                    .getRouteId()).isEqualTo("route-2999");
            assertThat(runtime.match("tenant-1.example.com", "/api/service-1/orders")).isNull();
        });
    }

    @Test
    void shouldRefreshUpstreamDiscoveryAfterRuntimeSwitched() {
        RecordingDiscoveryRegistry discoveryRegistry = new RecordingDiscoveryRegistry();
        ProxyConfigApplier applier = new ProxyConfigApplier(
                new PublishedConfigCompiler(),
                new ProxyRuntimeState(),
                runtime -> {
                },
                discoveryRegistry
        );
        ProxyApplyRequest request = new ProxyApplyRequest();
        request.setPublishedConfig(config("v1", "hash-v1"));

        ProxyApplyResult result = applier.apply(request);

        assertThat(result.getState()).isEqualTo(ConfigApplyState.APPLIED);
        assertThat(discoveryRegistry.refreshedRuntime).isNotNull();
        assertThat(discoveryRegistry.refreshedRuntime.getVersion()).isEqualTo("v1");
    }

    private PublishedConfig config(String version, String configHash) {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion(version);
        config.getSpec().setConfigHash(configHash);
        return config;
    }

    private PublishedConfig largeRouteConfig(String version, String configHash, String pathPrefix, int routeCount) {
        PublishedConfig config = config(version, configHash);
        for (int index = 0; index < routeCount; index++) {
            PublishedConfig.PublishedRoute route = new PublishedConfig.PublishedRoute();
            // 大路由表测试只关心编译索引和原子切换
            route.setRouteId("route-" + index);
            route.getHosts().add("tenant-" + index + ".example.com");
            route.setPath(pathPrefix + index);
            route.setUpstreamName("service-" + index);
            config.getSpec().getRoutes().add(route);
        }
        return config;
    }

    /**
     * 记录刷新调用的服务发现注册表。
     */
    private static class RecordingDiscoveryRegistry implements UpstreamDiscoveryRegistry {

        /**
         * 最近刷新运行态。
         */
        private CompiledProxyRuntime refreshedRuntime;

        /**
         * 根据新运行态刷新订阅。
         *
         * @param runtime 新运行态
         */
        @Override
        public void refresh(CompiledProxyRuntime runtime) {
            this.refreshedRuntime = runtime;
        }

        /**
         * 查询上游当前实例。
         *
         * @param upstream 已编译上游
         * @return 当前可用实例
         */
        @Override
        public List<CompiledUpstream.CompiledEndpoint> instances(CompiledUpstream upstream) {
            return upstream.getEndpoints();
        }
    }
}
