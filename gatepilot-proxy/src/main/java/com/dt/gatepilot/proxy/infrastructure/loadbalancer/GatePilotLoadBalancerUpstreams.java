package com.dt.gatepilot.proxy.infrastructure.loadbalancer;

import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;
import com.dt.gatepilot.proxy.domain.runtime.CompiledUpstream;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import java.util.Optional;

/**
 * GatePilot LoadBalancer 上游查询器
 */
final class GatePilotLoadBalancerUpstreams {

    private GatePilotLoadBalancerUpstreams() {
        // 上游查询器不允许实例化
    }

    /**
     * 根据 serviceId 查询上游
     *
     * @param runtimeState proxy 运行态
     * @param serviceId LoadBalancer serviceId
     * @return 上游
     */
    static Optional<CompiledUpstream> find(ProxyRuntimeState runtimeState, String serviceId) {
        return runtimeState.current()
                .map(CompiledProxyRuntime::getUpstreamsByName)
                .flatMap(upstreams -> upstreams.values().stream()
                        .filter(upstream -> GatePilotLoadBalancerServiceIds.matches(serviceId, upstream.getName()))
                        .findFirst());
    }
}
