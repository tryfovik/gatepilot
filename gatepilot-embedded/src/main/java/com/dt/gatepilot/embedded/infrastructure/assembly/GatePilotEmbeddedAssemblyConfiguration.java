package com.dt.gatepilot.embedded.infrastructure.assembly;

import com.dt.gatepilot.agent.application.service.AgentRuntimeAuditReporter;
import com.dt.gatepilot.agent.domain.port.ProxyApplyClient;
import com.dt.gatepilot.agent.domain.port.ProxyRuntimeStatusReader;
import com.dt.gatepilot.agent.infrastructure.config.AgentRuntimeConstants;
import com.dt.gatepilot.domain.deployment.GatePilotDeploymentModeConstants;
import com.dt.gatepilot.proxy.domain.port.RuntimeAuditSink;
import com.dt.gatepilot.proxy.domain.port.UpstreamDiscoveryRegistry;
import com.dt.gatepilot.proxy.domain.runtime.ProxyConfigApplier;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.domain.runtime.UpstreamEndpointHealthRegistry;
import com.dt.gatepilot.proxy.infrastructure.config.ConditionalOnGatePilotProxyEnabled;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * GatePilot 单 JVM 嵌入式装配配置。
 */
@Configuration
@ConditionalOnProperty(prefix = GatePilotDeploymentModeConstants.CONFIG_PREFIX,
        name = GatePilotDeploymentModeConstants.MODE_PROPERTY,
        havingValue = GatePilotDeploymentModeConstants.MODE_STANDALONE)
public class GatePilotEmbeddedAssemblyConfiguration {

    /**
     * 创建进程内 proxy apply 客户端。
     *
     * @param proxyConfigApplier proxy 配置应用器
     * @return proxy apply 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnGatePilotProxyEnabled
    public ProxyApplyClient proxyApplyClient(ProxyConfigApplier proxyConfigApplier) {
        // embedded 只做端口桥接，不承载发布或转发业务逻辑
        return new InProcessProxyApplyClient(proxyConfigApplier);
    }

    /**
     * 创建进程内 proxy 运行状态读取器。
     *
     * @param runtimeState proxy 运行态
     * @param healthRegistry 上游端点健康状态表
     * @return proxy 运行状态读取器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnGatePilotProxyEnabled
    public ProxyRuntimeStatusReader proxyRuntimeStatusReader(ObjectProvider<ProxyRuntimeState> runtimeStateProvider,
                                                             ObjectProvider<UpstreamEndpointHealthRegistry>
                                                                     healthRegistryProvider,
                                                             ObjectProvider<UpstreamDiscoveryRegistry>
                                                                     discoveryRegistryProvider) {
        // embedded 只把 proxy 本机状态补进 agent 心跳
        ProxyRuntimeState runtimeState = runtimeStateProvider.getIfAvailable();
        UpstreamEndpointHealthRegistry healthRegistry = healthRegistryProvider.getIfAvailable();
        UpstreamDiscoveryRegistry discoveryRegistry = discoveryRegistryProvider.getIfAvailable();
        if (runtimeState == null || healthRegistry == null || discoveryRegistry == null) {
            return snapshot -> {
            };
        }
        return new InProcessProxyRuntimeStatusReader(runtimeState, healthRegistry, discoveryRegistry);
    }

    /**
     * 创建进程内运行审计采集器。
     *
     * @param runtimeAuditReporter agent 运行审计上报器
     * @return 运行审计采集器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnGatePilotProxyEnabled
    @ConditionalOnProperty(prefix = AgentRuntimeConstants.CONFIG_PREFIX,
            name = AgentRuntimeConstants.ENABLED_PROPERTY,
            havingValue = "true",
            matchIfMissing = true)
    public RuntimeAuditSink runtimeAuditSink(ObjectProvider<AgentRuntimeAuditReporter> runtimeAuditReporterProvider) {
        // 单体部署下 proxy 审计事件直接进入 agent 缓冲区
        AgentRuntimeAuditReporter runtimeAuditReporter = runtimeAuditReporterProvider.getIfAvailable();
        if (runtimeAuditReporter == null) {
            return event -> {
            };
        }
        return new InProcessRuntimeAuditSink(runtimeAuditReporter);
    }
}
