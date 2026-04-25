package com.dt.gatepilot.agent.infrastructure.config;

import com.dt.gatepilot.domain.enums.NodeRole;
import com.dt.gatepilot.domain.resource.meta.LabelSelector;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
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
}
