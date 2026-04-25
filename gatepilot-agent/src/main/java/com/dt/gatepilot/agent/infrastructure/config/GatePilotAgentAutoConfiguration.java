package com.dt.gatepilot.agent.infrastructure.config;

import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.application.service.AgentRuntimeCoordinator;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.agent.domain.port.LocalConfigStore;
import com.dt.gatepilot.agent.domain.port.ProxyApplyClient;
import com.dt.gatepilot.agent.infrastructure.persistence.memory.InMemoryLocalConfigStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * GatePilot agent 自动配置。
 */
@Configuration
@EnableConfigurationProperties(GatePilotAgentProperties.class)
@ConditionalOnProperty(prefix = AgentRuntimeConstants.CONFIG_PREFIX,
        name = AgentRuntimeConstants.ENABLED_PROPERTY,
        havingValue = "true",
        matchIfMissing = true)
public class GatePilotAgentAutoConfiguration {

    /**
     * 创建 agent 节点身份。
     *
     * @param properties agent 配置
     * @return agent 节点身份
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentNodeProfile agentNodeProfile(GatePilotAgentProperties properties) {
        AgentNodeProfile profile = new AgentNodeProfile();
        // 节点身份只从配置装配，不在运行链路里猜测
        profile.setNamespace(properties.getNamespace());
        profile.setNodeId(properties.getNodeId());
        profile.setRole(properties.getRole());
        profile.setZone(properties.getZone());
        profile.setIsolationGroup(properties.getIsolationGroup());
        profile.setAddress(properties.getAddress());
        profile.setAgentVersion(properties.getAgentVersion());
        profile.setProxyVersion(properties.getProxyVersion());
        profile.setConfigShards(properties.getConfigShards());
        profile.setProjectSelector(properties.getProjectSelector());
        profile.setCapabilities(properties.getCapabilities());
        return profile;
    }

    /**
     * 创建本地配置存储。
     *
     * @return 本地配置存储
     */
    @Bean
    @ConditionalOnMissingBean
    public LocalConfigStore localConfigStore() {
        // 先用内存实现跑通主链路，磁盘持久化后续替换这个 Bean
        return new InMemoryLocalConfigStore();
    }

    /**
     * 创建 agent 运行编排器。
     *
     * @param nodeProfile 节点身份
     * @param controlPlaneClient 控制面客户端
     * @param localConfigStore 本地配置存储
     * @param proxyApplyClient proxy apply 客户端
     * @return agent 运行编排器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({AgentControlPlaneClient.class, LocalConfigStore.class, ProxyApplyClient.class})
    public AgentRuntimeCoordinator agentRuntimeCoordinator(AgentNodeProfile nodeProfile,
                                                           AgentControlPlaneClient controlPlaneClient,
                                                           LocalConfigStore localConfigStore,
                                                           ProxyApplyClient proxyApplyClient) {
        // 编排器只拼端口，不关心端口背后是 HTTP 还是进程内
        return new AgentRuntimeCoordinator(nodeProfile, controlPlaneClient, localConfigStore, proxyApplyClient);
    }
}
