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
