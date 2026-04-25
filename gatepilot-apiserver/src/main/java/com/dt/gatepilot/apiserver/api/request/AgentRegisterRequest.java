package com.dt.gatepilot.apiserver.api.request;

import com.dt.gatepilot.api.enums.NodeRole;
import com.dt.gatepilot.api.resource.common.LabelSelector;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * agent 节点注册请求。
 */
@Data
public class AgentRegisterRequest {

    /**
     * 节点命名空间。
     */
    private String namespace = "default";

    /**
     * 节点标识。
     */
    @NotBlank
    private String nodeId;

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
