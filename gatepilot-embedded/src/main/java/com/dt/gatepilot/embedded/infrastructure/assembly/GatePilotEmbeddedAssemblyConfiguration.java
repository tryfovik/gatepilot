package com.dt.gatepilot.embedded.infrastructure.assembly;

import com.dt.gatepilot.agent.application.service.AgentRuntimeAuditReporter;
import com.dt.gatepilot.agent.domain.port.ProxyApplyClient;
import com.dt.gatepilot.proxy.domain.port.RuntimeAuditSink;
import com.dt.gatepilot.proxy.domain.runtime.ProxyConfigApplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * GatePilot 单 JVM 嵌入式装配配置。
 */
@Configuration
public class GatePilotEmbeddedAssemblyConfiguration {

    /**
     * 创建进程内 proxy apply 客户端。
     *
     * @param proxyConfigApplier proxy 配置应用器
     * @return proxy apply 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ProxyConfigApplier.class)
    public ProxyApplyClient proxyApplyClient(ProxyConfigApplier proxyConfigApplier) {
        // embedded 只做端口桥接，不承载发布或转发业务逻辑
        return new InProcessProxyApplyClient(proxyConfigApplier);
    }

    /**
     * 创建进程内运行审计采集器。
     *
     * @param runtimeAuditReporter agent 运行审计上报器
     * @return 运行审计采集器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AgentRuntimeAuditReporter.class)
    public RuntimeAuditSink runtimeAuditSink(AgentRuntimeAuditReporter runtimeAuditReporter) {
        // 单体部署下 proxy 审计事件直接进入 agent 缓冲区
        return new InProcessRuntimeAuditSink(runtimeAuditReporter);
    }
}
