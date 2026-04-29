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

import com.dt.gatepilot.proxy.domain.runtime.CompiledUpstream;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

/**
 * GatePilot 上游负载均衡器
 */
public class GatePilotReactorServiceInstanceLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    /**
     * LoadBalancer serviceId
     */
    private final String serviceId;

    /**
     * proxy 当前运行态
     */
    private final ProxyRuntimeState runtimeState;

    /**
     * Spring Cloud 轮询负载均衡器
     */
    private final RoundRobinLoadBalancer roundRobinLoadBalancer;

    /**
     * Spring Cloud 随机负载均衡器
     */
    private final RandomLoadBalancer randomLoadBalancer;

    /**
     * 创建 GatePilot 上游负载均衡器
     *
     * @param serviceId LoadBalancer serviceId
     * @param supplierProvider 实例列表提供器
     * @param runtimeState proxy 当前运行态
     */
    public GatePilotReactorServiceInstanceLoadBalancer(
            String serviceId,
            ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
            ProxyRuntimeState runtimeState) {
        this.serviceId = serviceId;
        this.runtimeState = runtimeState;
        this.roundRobinLoadBalancer = new RoundRobinLoadBalancer(supplierProvider, serviceId);
        this.randomLoadBalancer = new RandomLoadBalancer(supplierProvider, serviceId);
    }

    /**
     * 选择上游实例
     *
     * @param request LoadBalancer 请求
     * @return 选择结果
     */
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        if (GatePilotLoadBalancerStrategies.random(strategy())) {
            return randomLoadBalancer.choose(request);
        }
        // 轮询和加权轮询都交给 Spring Cloud LoadBalancer
        return roundRobinLoadBalancer.choose(request);
    }

    /**
     * 读取当前上游的负载均衡策略
     *
     * @return 归一化后的策略文本
     */
    private String strategy() {
        return GatePilotLoadBalancerUpstreams.find(runtimeState, serviceId)
                .map(CompiledUpstream::getLoadBalance)
                .map(GatePilotLoadBalancerStrategies::normalize)
                .orElse(null);
    }
}
