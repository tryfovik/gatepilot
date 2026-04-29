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

import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.proxy.domain.runtime.CompiledUpstream;
import com.dt.gatepilot.proxy.domain.port.UpstreamDiscoveryRegistry;
import com.dt.gatepilot.proxy.domain.runtime.ProxyLoadBalanceConstants;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.domain.runtime.UpstreamEndpointHealthRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.WeightedServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

/**
 * 从 GatePilot 运行态提供上游实例列表
 */
public class GatePilotServiceInstanceListSupplier implements ServiceInstanceListSupplier {

    /**
     * LoadBalancer serviceId
     */
    private final String serviceId;

    /**
     * proxy 当前运行态
     */
    private final ProxyRuntimeState runtimeState;

    /**
     * 上游端点健康状态表
     */
    private final UpstreamEndpointHealthRegistry healthRegistry;

    /**
     * 上游服务发现注册表
     */
    private final UpstreamDiscoveryRegistry upstreamDiscoveryRegistry;

    /**
     * 创建 GatePilot 上游实例列表提供器
     *
     * @param serviceId LoadBalancer serviceId
     * @param runtimeState proxy 当前运行态
     * @param healthRegistry 上游端点健康状态表
     * @param upstreamDiscoveryRegistry 上游服务发现注册表
     */
    public GatePilotServiceInstanceListSupplier(String serviceId,
                                                ProxyRuntimeState runtimeState,
                                                UpstreamEndpointHealthRegistry healthRegistry,
                                                UpstreamDiscoveryRegistry upstreamDiscoveryRegistry) {
        this.serviceId = serviceId;
        this.runtimeState = runtimeState;
        this.healthRegistry = healthRegistry;
        this.upstreamDiscoveryRegistry = upstreamDiscoveryRegistry;
    }

    /**
     * 获取 LoadBalancer serviceId
     *
     * @return LoadBalancer serviceId
     */
    @Override
    public String getServiceId() {
        return serviceId;
    }

    /**
     * 获取上游实例列表
     *
     * @return 上游实例列表
     */
    @Override
    public Flux<List<ServiceInstance>> get() {
        return get(null);
    }

    /**
     * 按请求上下文获取上游实例列表
     *
     * @param request LoadBalancer 请求
     * @return 上游实例列表
     */
    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        return Flux.defer(() -> GatePilotLoadBalancerUpstreams.find(runtimeState, serviceId)
                .map(upstream -> instances(upstream, request))
                .orElseGet(() -> Flux.just(List.of())));
    }

    /**
     * 构建当前上游可选实例列表
     *
     * @param upstream 已编译上游
     * @param request LoadBalancer 请求
     * @return 上游实例列表
     */
    private Flux<List<ServiceInstance>> instances(CompiledUpstream upstream, Request request) {
        List<CompiledUpstream.CompiledEndpoint> endpoints = selectableEndpoints(upstream);
        List<ServiceInstance> instances = IntStream.range(0, endpoints.size())
                .mapToObj(index -> instance(upstream, endpoints.get(index), index))
                .toList();
        if (!GatePilotLoadBalancerStrategies.weightedRoundRobin(upstream.getLoadBalance())) {
            return Flux.just(instances);
        }
        // 加权列表展开交给 Spring Cloud LoadBalancer
        ServiceInstanceListSupplier delegate = new StaticServiceInstanceListSupplier(serviceId, instances);
        return new WeightedServiceInstanceListSupplier(delegate).get(request);
    }

    /**
     * 过滤本机已摘除端点
     *
     * @param upstream 已编译上游
     * @return 可选端点列表
     */
    private List<CompiledUpstream.CompiledEndpoint> selectableEndpoints(CompiledUpstream upstream) {
        List<CompiledUpstream.CompiledEndpoint> discoveredEndpoints = upstreamDiscoveryRegistry.instances(upstream);
        List<CompiledUpstream.CompiledEndpoint> endpoints = discoveredEndpoints.stream()
                .filter(endpoint -> healthRegistry.selectable(upstream.getName(), endpoint))
                .toList();
        // 全部摘除时失败开放，让真实请求继续暴露端点状态
        return endpoints.isEmpty() ? discoveredEndpoints : endpoints;
    }

    /**
     * 将 GatePilot 端点转换成 Spring Cloud 实例
     *
     * @param upstream 已编译上游
     * @param endpoint 已编译端点
     * @param index 端点序号
     * @return Spring Cloud 服务实例
     */
    private ServiceInstance instance(CompiledUpstream upstream,
                                     CompiledUpstream.CompiledEndpoint endpoint,
                                     int index) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put(GatePilotLoadBalancerConstants.METADATA_UPSTREAM_NAME, upstream.getName());
        metadata.put(GatePilotLoadBalancerConstants.METADATA_WEIGHT, String.valueOf(weight(endpoint)));
        return new DefaultServiceInstance(
                instanceId(index),
                serviceId,
                endpoint.getHost(),
                port(upstream, endpoint),
                secure(upstream),
                metadata
        );
    }

    /**
     * 生成实例标识
     *
     * @param index 端点序号
     * @return 实例标识
     */
    private String instanceId(int index) {
        return serviceId + GatePilotLoadBalancerConstants.INSTANCE_ID_SEPARATOR + index;
    }

    /**
     * 解析端点端口
     *
     * @param upstream 已编译上游
     * @param endpoint 已编译端点
     * @return 端点端口
     */
    private int port(CompiledUpstream upstream, CompiledUpstream.CompiledEndpoint endpoint) {
        if (endpoint.getPort() != null) {
            return endpoint.getPort();
        }
        return secure(upstream)
                ? GatePilotLoadBalancerConstants.DEFAULT_HTTPS_PORT
                : GatePilotLoadBalancerConstants.DEFAULT_HTTP_PORT;
    }

    /**
     * 判断上游是否使用安全协议
     *
     * @param upstream 已编译上游
     * @return 是否安全协议
     */
    private boolean secure(CompiledUpstream upstream) {
        return upstream.getProtocol() == Protocol.HTTPS || upstream.getProtocol() == Protocol.WSS;
    }

    /**
     * 解析端点权重
     *
     * @param endpoint 已编译端点
     * @return 端点权重
     */
    private int weight(CompiledUpstream.CompiledEndpoint endpoint) {
        Integer weight = endpoint.getWeight();
        return weight == null || weight <= 0 ? ProxyLoadBalanceConstants.MIN_ENDPOINT_WEIGHT : weight;
    }

    /**
     * 固定实例列表提供器
     */
    private static class StaticServiceInstanceListSupplier implements ServiceInstanceListSupplier {

        /**
         * LoadBalancer serviceId
         */
        private final String serviceId;

        /**
         * 固定实例列表
         */
        private final List<ServiceInstance> instances;

        StaticServiceInstanceListSupplier(String serviceId, List<ServiceInstance> instances) {
            this.serviceId = serviceId;
            this.instances = instances;
        }

        /**
         * 获取 LoadBalancer serviceId
         *
         * @return LoadBalancer serviceId
         */
        @Override
        public String getServiceId() {
            return serviceId;
        }

        /**
         * 获取固定实例列表
         *
         * @return 固定实例列表
         */
        @Override
        public Flux<List<ServiceInstance>> get() {
            return Flux.just(instances);
        }
    }
}
