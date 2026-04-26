package com.dt.gatepilot.agent.infrastructure.config;

import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.application.service.AgentRuntimeAuditReporter;
import com.dt.gatepilot.agent.application.service.AgentRuntimeCoordinator;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.agent.domain.port.LocalConfigStore;
import com.dt.gatepilot.agent.domain.port.PublishedConfigSyncAdapter;
import com.dt.gatepilot.agent.domain.port.ProxyApplyClient;
import com.dt.gatepilot.agent.domain.port.ProxyRuntimeStatusReader;
import com.dt.gatepilot.agent.infrastructure.apiserver.HttpPullPublishedConfigSyncAdapter;
import com.dt.gatepilot.agent.infrastructure.persistence.file.FileLocalConfigStore;
import com.dt.gatepilot.agent.infrastructure.persistence.memory.InMemoryLocalConfigStore;
import com.dt.gatepilot.agent.infrastructure.scheduling.AgentLifecycleManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

/**
 * GatePilot agent 自动配置。
 */
@Configuration
@EnableScheduling
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
        // 节点身份只从网关配置装配，避免同一职责出现多套来源
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
     * @param properties agent 配置
     * @param objectMapper JSON 映射器
     * @return 本地配置存储
     */
    @Bean
    @ConditionalOnMissingBean
    public LocalConfigStore localConfigStore(GatePilotAgentProperties properties, ObjectMapper objectMapper) {
        String storeType = localConfigStoreType(properties);
        if (AgentRuntimeConstants.LOCAL_CONFIG_STORE_TYPE_MEMORY.equals(storeType)) {
            // memory 仅用于测试或临时开发，生产应使用 file 并挂载持久卷
            return new InMemoryLocalConfigStore();
        }
        if (AgentRuntimeConstants.LOCAL_CONFIG_STORE_TYPE_FILE.equals(storeType)) {
            // file store 负责 staged 和 last-good 的本地持久化
            return new FileLocalConfigStore(objectMapper, properties.getLocalConfig().getDirectory());
        }
        throw new IllegalArgumentException(AgentRuntimeConstants.MESSAGE_UNSUPPORTED_LOCAL_CONFIG_STORE_TYPE
                + ": " + storeType);
    }

    /**
     * 创建默认 JSON 映射器。
     *
     * @return JSON 映射器
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册 JavaTime 等模块，保证 PublishedConfig 可直接落盘
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    private String localConfigStoreType(GatePilotAgentProperties properties) {
        String storeType = properties.getLocalConfig().getStoreType();
        if (!StringUtils.hasText(storeType)) {
            return AgentRuntimeConstants.LOCAL_CONFIG_STORE_TYPE_FILE;
        }
        // 配置项统一小写，避免部署时大小写差异导致启动失败
        return storeType.trim().toLowerCase();
    }

    /**
     * 创建已发布配置同步适配器。
     *
     * @param controlPlaneClient 控制面客户端
     * @return 配置同步适配器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AgentControlPlaneClient.class)
    public PublishedConfigSyncAdapter publishedConfigSyncAdapter(AgentControlPlaneClient controlPlaneClient) {
        // 默认只提供 HTTP pull，Nacos watch 后续通过替换这个 bean 接入
        return new HttpPullPublishedConfigSyncAdapter(controlPlaneClient);
    }

    /**
     * 创建 agent 运行编排器。
     *
     * @param nodeProfile 节点身份
     * @param controlPlaneClient 控制面客户端
     * @param configSyncAdapter 配置同步适配器
     * @param localConfigStore 本地配置存储
     * @param proxyApplyClient proxy apply 客户端
     * @param proxyRuntimeStatusReaderProvider proxy 运行状态读取器提供器
     * @return agent 运行编排器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({AgentControlPlaneClient.class, PublishedConfigSyncAdapter.class,
            LocalConfigStore.class, ProxyApplyClient.class})
    public AgentRuntimeCoordinator agentRuntimeCoordinator(AgentNodeProfile nodeProfile,
                                                           AgentControlPlaneClient controlPlaneClient,
                                                           PublishedConfigSyncAdapter configSyncAdapter,
                                                           LocalConfigStore localConfigStore,
                                                           ProxyApplyClient proxyApplyClient,
                                                           ObjectProvider<ProxyRuntimeStatusReader>
                                                                   proxyRuntimeStatusReaderProvider) {
        // 编排器只拼端口，不关心同步背后是 HTTP pull 还是 watch
        return new AgentRuntimeCoordinator(nodeProfile, controlPlaneClient, configSyncAdapter,
                localConfigStore, proxyApplyClient, proxyRuntimeStatusReaderProvider.getIfAvailable());
    }

    /**
     * 创建运行审计上报器。
     *
     * @param nodeProfile 节点身份
     * @param controlPlaneClient 控制面客户端
     * @param properties agent 配置
     * @return 运行审计上报器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AgentControlPlaneClient.class)
    public AgentRuntimeAuditReporter agentRuntimeAuditReporter(AgentNodeProfile nodeProfile,
                                                               AgentControlPlaneClient controlPlaneClient,
                                                               GatePilotAgentProperties properties) {
        // agent 只负责把本机运行事件批量送回控制面
        return new AgentRuntimeAuditReporter(nodeProfile, controlPlaneClient, properties);
    }

    /**
     * 创建 agent 生命周期调度器。
     *
     * @param coordinator agent 运行编排器
     * @param runtimeAuditReporterProvider 运行审计上报器提供器
     * @param properties agent 配置
     * @return agent 生命周期调度器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AgentRuntimeCoordinator.class)
    public AgentLifecycleManager agentLifecycleManager(AgentRuntimeCoordinator coordinator,
                                                       ObjectProvider<AgentRuntimeAuditReporter>
                                                               runtimeAuditReporterProvider,
                                                       GatePilotAgentProperties properties) {
        // 生命周期调度只触发编排器，不承载配置发布和转发逻辑
        return new AgentLifecycleManager(coordinator, runtimeAuditReporterProvider.getIfAvailable(), properties);
    }
}
