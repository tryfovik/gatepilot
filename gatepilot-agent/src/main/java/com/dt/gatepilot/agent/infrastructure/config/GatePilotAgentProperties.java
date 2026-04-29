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
package com.dt.gatepilot.agent.infrastructure.config;

import com.dt.gatepilot.agent.application.service.AgentRuntimeAuditConstants;
import com.dt.gatepilot.domain.enums.NodeRole;
import com.dt.gatepilot.domain.resource.meta.LabelSelector;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GatePilot agent 配置。
 */
@Data
@ConfigurationProperties(prefix = AgentRuntimeConstants.CONFIG_PREFIX)
public class GatePilotAgentProperties {

    /**
     * 是否启用 agent 客户端能力。
     */
    private boolean enabled = true;

    /**
     * apiserver 基础地址。
     */
    private String apiserverBaseUrl = AgentRuntimeConstants.DEFAULT_APISERVER_BASE_URL;

    /**
     * proxy runtime control 基础地址
     */
    private String proxyBaseUrl = AgentRuntimeConstants.DEFAULT_PROXY_BASE_URL;

    /**
     * 节点命名空间。
     */
    private String namespace = ResourceMetadataConstants.DEFAULT_NAMESPACE;

    /**
     * 节点标识。
     */
    private String nodeId = AgentRuntimeConstants.DEFAULT_NODE_ID;

    /**
     * 节点角色。
     */
    private NodeRole role = NodeRole.COMBINED;

    /**
     * 可用区或机房。
     */
    private String zone;

    /**
     * 隔离组。
     */
    private String isolationGroup;

    /**
     * 节点地址。
     */
    private String address;

    /**
     * agent 版本。
     */
    private String agentVersion;

    /**
     * proxy 版本。
     */
    private String proxyVersion;

    /**
     * 节点负责消费的配置分片。
     */
    private List<String> configShards = new ArrayList<>();

    /**
     * 节点可服务的项目选择器。
     */
    private LabelSelector projectSelector;

    /**
     * 节点能力。
     */
    private Map<String, String> capabilities = new LinkedHashMap<>();

    /**
     * 本地配置存储配置。
     */
    private LocalConfig localConfig = new LocalConfig();

    /**
     * 是否启用 agent 生命周期调度。
     */
    private boolean lifecycleEnabled = true;

    /**
     * 启动时是否注册节点。
     */
    private boolean registerOnStartup = true;

    /**
     * 启动时是否尝试使用 last-good 配置启动 proxy。
     */
    private boolean startWithLastGoodOnStartup = true;

    /**
     * 是否启用配置定时拉取。
     */
    private boolean pullEnabled = true;

    /**
     * 配置拉取周期，单位毫秒。
     */
    private long pullIntervalMs = AgentRuntimeConstants.DEFAULT_PULL_INTERVAL_MS;

    /**
     * 配置拉取初始延迟，单位毫秒。
     */
    private long pullInitialDelayMs = AgentRuntimeConstants.DEFAULT_PULL_INITIAL_DELAY_MS;

    /**
     * 是否启用心跳上报。
     */
    private boolean heartbeatEnabled = true;

    /**
     * 心跳上报周期，单位毫秒。
     */
    private long heartbeatIntervalMs = AgentRuntimeConstants.DEFAULT_HEARTBEAT_INTERVAL_MS;

    /**
     * 心跳上报初始延迟，单位毫秒。
     */
    private long heartbeatInitialDelayMs = AgentRuntimeConstants.DEFAULT_HEARTBEAT_INITIAL_DELAY_MS;

    /**
     * 运行审计上报配置。
     */
    private Audit audit = new Audit();

    /**
     * 运行审计上报配置。
     */
    @Data
    public static class Audit {

        /**
         * 是否启用运行审计上报。
         */
        private boolean enabled = true;

        /**
         * 审计事件缓冲容量。
         */
        private int bufferCapacity = AgentRuntimeAuditConstants.DEFAULT_BUFFER_CAPACITY;

        /**
         * 单批上报条数。
         */
        private int flushBatchSize = AgentRuntimeAuditConstants.DEFAULT_FLUSH_BATCH_SIZE;

        /**
         * 审计上报周期，单位毫秒。
         */
        private long flushIntervalMs = AgentRuntimeAuditConstants.DEFAULT_FLUSH_INTERVAL_MS;

        /**
         * 审计上报初始延迟，单位毫秒。
         */
        private long flushInitialDelayMs = AgentRuntimeAuditConstants.DEFAULT_FLUSH_INITIAL_DELAY_MS;
    }

    /**
     * 本地配置存储配置。
     */
    @Data
    public static class LocalConfig {

        /**
         * 本地配置存储类型。
         */
        private String storeType = AgentRuntimeConstants.LOCAL_CONFIG_STORE_TYPE_FILE;

        /**
         * 本地配置存储目录。
         */
        private Path directory = Path.of(AgentRuntimeConstants.DEFAULT_LOCAL_CONFIG_DIRECTORY);
    }
}
