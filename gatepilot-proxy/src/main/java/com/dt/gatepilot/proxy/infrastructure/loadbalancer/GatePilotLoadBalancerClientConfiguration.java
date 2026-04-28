package com.dt.gatepilot.proxy.infrastructure.loadbalancer;

import com.dt.gatepilot.proxy.domain.port.UpstreamDiscoveryRegistry;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.infrastructure.config.ConditionalOnGatePilotProxyEnabled;
import com.dt.gatepilot.proxy.domain.runtime.UpstreamEndpointHealthRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * GatePilot Spring Cloud LoadBalancer 客户端配置
 */
@ConditionalOnGatePilotProxyEnabled
public class GatePilotLoadBalancerClientConfiguration {

    /**
     * 创建 GatePilot 上游实例列表提供器
     *
     * @param environment LoadBalancer 子上下文环境
     * @param runtimeState proxy 当前运行态
     * @param healthRegistry 上游端点健康状态表
     * @param upstreamDiscoveryRegistry 上游服务发现注册表
     * @return 上游实例列表提供器
     */
    @Bean
    @ConditionalOnMissingBean
    public ServiceInstanceListSupplier gatePilotServiceInstanceListSupplier(
            Environment environment,
            ProxyRuntimeState runtimeState,
            UpstreamEndpointHealthRegistry healthRegistry,
            UpstreamDiscoveryRegistry upstreamDiscoveryRegistry) {
        // serviceId 来自 LoadBalancer 子上下文，不从请求里临时解析
        return new GatePilotServiceInstanceListSupplier(
                LoadBalancerClientFactory.getName(environment),
                runtimeState,
                healthRegistry,
                upstreamDiscoveryRegistry
        );
    }

    /**
     * 创建 GatePilot 上游负载均衡器
     *
     * @param environment LoadBalancer 子上下文环境
     * @param loadBalancerClientFactory Spring Cloud LoadBalancer 客户端工厂
     * @param runtimeState proxy 当前运行态
     * @return 上游负载均衡器
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactorServiceInstanceLoadBalancer gatePilotReactorServiceInstanceLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory,
            ProxyRuntimeState runtimeState) {
        String serviceId = LoadBalancerClientFactory.getName(environment);
        ObjectProvider<ServiceInstanceListSupplier> supplierProvider =
                loadBalancerClientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class);
        // 具体轮询、随机能力委托给 Spring Cloud LoadBalancer
        return new GatePilotReactorServiceInstanceLoadBalancer(serviceId, supplierProvider, runtimeState);
    }
}
