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
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.domain.port.UpstreamDiscoveryRegistry;
import com.dt.gatepilot.proxy.domain.runtime.CompiledUpstream;
import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.domain.runtime.PublishedConfigCompiler;
import com.dt.gatepilot.proxy.domain.runtime.StaticUpstreamDiscoveryRegistry;
import com.dt.gatepilot.proxy.domain.runtime.UpstreamEndpointHealthRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GatePilot LoadBalancer 实例列表测试
 */
class GatePilotServiceInstanceListSupplierTest {

    /**
     * 应从运行态暴露健康上游实例
     */
    @Test
    void shouldExposeHealthyInstancesFromRuntime() {
        ProxyRuntimeState state = runtime("ROUND_ROBIN", endpoint("upstream-a.local", 8080, 100),
                endpoint("upstream-b.local", 8081, 100));
        UpstreamEndpointHealthRegistry healthRegistry = new UpstreamEndpointHealthRegistry();
        CompiledUpstream upstream = upstream(state);
        healthRegistry.recordFailure(upstream, upstream.getEndpoints().get(0), "connect failed");
        healthRegistry.recordFailure(upstream, upstream.getEndpoints().get(0), "connect failed");
        GatePilotServiceInstanceListSupplier supplier = supplier(state, healthRegistry);

        List<ServiceInstance> instances = supplier.get().blockFirst(Duration.ofSeconds(1));

        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).getHost()).isEqualTo("upstream-b.local");
        assertThat(instances.get(0).getPort()).isEqualTo(8081);
    }

    /**
     * 应把加权轮询交给 Spring Cloud LoadBalancer 展开
     */
    @Test
    void shouldExpandWeightedInstancesBySpringLoadBalancer() {
        ProxyRuntimeState state = runtime("WEIGHTED_ROUND_ROBIN", endpoint("upstream-a.local", 8080, 100),
                endpoint("upstream-b.local", 8081, 50));
        GatePilotServiceInstanceListSupplier supplier = supplier(state, new UpstreamEndpointHealthRegistry());

        List<ServiceInstance> instances = supplier.get().blockFirst(Duration.ofSeconds(1));

        assertThat(instances).hasSize(3);
        assertThat(instances.stream().filter(instance -> "upstream-a.local".equals(instance.getHost())).count())
                .isEqualTo(2);
        assertThat(instances.stream().filter(instance -> "upstream-b.local".equals(instance.getHost())).count())
                .isEqualTo(1);
    }

    /**
     * 应让 Spring Cloud LoadBalancer 执行轮询选择
     */
    @Test
    void shouldUseSpringRoundRobinLoadBalancer() {
        ProxyRuntimeState state = runtime("ROUND_ROBIN", endpoint("upstream-a.local", 8080, 100),
                endpoint("upstream-b.local", 8081, 100));
        GatePilotServiceInstanceListSupplier supplier = supplier(state, new UpstreamEndpointHealthRegistry());
        GatePilotReactorServiceInstanceLoadBalancer loadBalancer = new GatePilotReactorServiceInstanceLoadBalancer(
                supplier.getServiceId(),
                new StaticObjectProvider<>(supplier),
                state
        );

        Response<ServiceInstance> first = loadBalancer.choose(new DefaultRequest<>()).block(Duration.ofSeconds(1));
        Response<ServiceInstance> second = loadBalancer.choose(new DefaultRequest<>()).block(Duration.ofSeconds(1));

        assertThat(List.of(first.getServer().getHost(), second.getServer().getHost()))
                .containsExactlyInAnyOrder("upstream-a.local", "upstream-b.local");
    }

    /**
     * 应优先使用服务发现注册表提供的动态实例
     */
    @Test
    void shouldExposeDiscoveredInstances() {
        ProxyRuntimeState state = runtime("ROUND_ROBIN");
        GatePilotServiceInstanceListSupplier supplier = supplier(state, new UpstreamEndpointHealthRegistry(),
                new FixedDiscoveryRegistry(endpoint("10.0.0.11", 8080, 100)));

        List<ServiceInstance> instances = supplier.get().blockFirst(Duration.ofSeconds(1));

        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).getHost()).isEqualTo("10.0.0.11");
        assertThat(instances.get(0).getPort()).isEqualTo(8080);
    }

    private GatePilotServiceInstanceListSupplier supplier(ProxyRuntimeState state,
                                                          UpstreamEndpointHealthRegistry healthRegistry) {
        return supplier(state, healthRegistry, new StaticUpstreamDiscoveryRegistry());
    }

    private GatePilotServiceInstanceListSupplier supplier(ProxyRuntimeState state,
                                                          UpstreamEndpointHealthRegistry healthRegistry,
                                                          UpstreamDiscoveryRegistry discoveryRegistry) {
        return new GatePilotServiceInstanceListSupplier(
                GatePilotLoadBalancerServiceIds.fromUpstreamName("admin-upstream"),
                state,
                healthRegistry,
                discoveryRegistry
        );
    }

    private CompiledUpstream upstream(ProxyRuntimeState state) {
        return state.current().orElseThrow().getUpstreamsByName().get("admin-upstream");
    }

    private ProxyRuntimeState runtime(String loadBalance, PublishedConfig.PublishedEndpoint... endpoints) {
        PublishedConfig config = new PublishedConfig();
        PublishedConfig.PublishedUpstream upstream = new PublishedConfig.PublishedUpstream();
        upstream.setName("admin-upstream");
        upstream.setProtocol(Protocol.HTTP);
        upstream.setLoadBalance(loadBalance);
        upstream.getEndpoints().addAll(List.of(endpoints));
        config.getSpec().getUpstreams().add(upstream);
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(config));
        return state;
    }

    private PublishedConfig.PublishedEndpoint endpoint(String host, int port, int weight) {
        PublishedConfig.PublishedEndpoint endpoint = new PublishedConfig.PublishedEndpoint();
        endpoint.setHost(host);
        endpoint.setPort(port);
        endpoint.setWeight(weight);
        return endpoint;
    }

    /**
     * 测试用固定服务发现注册表
     */
    private static class FixedDiscoveryRegistry implements UpstreamDiscoveryRegistry {

        /**
         * 固定端点。
         */
        private final PublishedConfig.PublishedEndpoint endpoint;

        FixedDiscoveryRegistry(PublishedConfig.PublishedEndpoint endpoint) {
            this.endpoint = endpoint;
        }

        /**
         * 根据新运行态刷新订阅。
         *
         * @param runtime 新运行态
         */
        @Override
        public void refresh(CompiledProxyRuntime runtime) {
            // 测试不需要后台订阅
        }

        /**
         * 查询上游当前实例。
         *
         * @param upstream 已编译上游
         * @return 当前可用实例
         */
        @Override
        public List<CompiledUpstream.CompiledEndpoint> instances(CompiledUpstream upstream) {
            CompiledUpstream.CompiledEndpoint compiledEndpoint = new CompiledUpstream.CompiledEndpoint();
            compiledEndpoint.setHost(endpoint.getHost());
            compiledEndpoint.setPort(endpoint.getPort());
            compiledEndpoint.setWeight(endpoint.getWeight());
            return List.of(compiledEndpoint);
        }
    }

    /**
     * 测试用固定 ObjectProvider
     *
     * @param <T> 对象类型
     */
    private static class StaticObjectProvider<T> implements ObjectProvider<T> {

        /**
         * 固定对象
         */
        private final T value;

        StaticObjectProvider(T value) {
            this.value = value;
        }

        /**
         * 获取对象
         *
         * @return 固定对象
         * @throws BeansException Spring Bean 异常
         */
        @Override
        public T getObject() throws BeansException {
            return value;
        }

        /**
         * 根据参数获取对象
         *
         * @param args 参数
         * @return 固定对象
         * @throws BeansException Spring Bean 异常
         */
        @Override
        public T getObject(Object... args) throws BeansException {
            return value;
        }

        /**
         * 获取可用对象
         *
         * @return 固定对象
         * @throws BeansException Spring Bean 异常
         */
        @Override
        public T getIfAvailable() throws BeansException {
            return value;
        }

        /**
         * 获取唯一对象
         *
         * @return 固定对象
         * @throws BeansException Spring Bean 异常
         */
        @Override
        public T getIfUnique() throws BeansException {
            return value;
        }
    }
}
