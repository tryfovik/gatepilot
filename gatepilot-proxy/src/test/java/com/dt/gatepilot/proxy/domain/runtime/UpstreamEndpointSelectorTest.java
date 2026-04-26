package com.dt.gatepilot.proxy.domain.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 上游端点选择器测试。
 */
class UpstreamEndpointSelectorTest {

    @Test
    void shouldSkipUnhealthyEndpointWhenSelecting() {
        UpstreamEndpointHealthRegistry registry = new UpstreamEndpointHealthRegistry();
        UpstreamEndpointSelector selector = new UpstreamEndpointSelector(registry);
        CompiledUpstream upstream = upstream();
        registry.recordFailure(upstream, upstream.getEndpoints().get(0), "down");

        CompiledUpstream.CompiledEndpoint selected = selector.select(upstream, "hash-key");

        assertThat(selected.getHost()).isEqualTo("second.local");
    }

    @Test
    void shouldFailOpenWhenAllEndpointsUnhealthy() {
        UpstreamEndpointHealthRegistry registry = new UpstreamEndpointHealthRegistry();
        UpstreamEndpointSelector selector = new UpstreamEndpointSelector(registry);
        CompiledUpstream upstream = upstream();
        registry.recordFailure(upstream, upstream.getEndpoints().get(0), "down");
        registry.recordFailure(upstream, upstream.getEndpoints().get(1), "down");

        CompiledUpstream.CompiledEndpoint selected = selector.select(upstream, "hash-key");

        assertThat(selected).isNotNull();
    }

    private CompiledUpstream upstream() {
        CompiledUpstream upstream = new CompiledUpstream();
        upstream.setName("admin-upstream");
        upstream.setLoadBalance(ProxyLoadBalanceConstants.DEFAULT_STRATEGY);
        CompiledUpstream.CompiledHealthCheck healthCheck = new CompiledUpstream.CompiledHealthCheck();
        healthCheck.setUnhealthyThreshold(1);
        upstream.setHealthCheck(healthCheck);
        upstream.getEndpoints().add(endpoint("first.local"));
        upstream.getEndpoints().add(endpoint("second.local"));
        return upstream;
    }

    private CompiledUpstream.CompiledEndpoint endpoint(String host) {
        CompiledUpstream.CompiledEndpoint endpoint = new CompiledUpstream.CompiledEndpoint();
        endpoint.setHost(host);
        endpoint.setPort(8080);
        endpoint.setWeight(100);
        return endpoint;
    }
}
