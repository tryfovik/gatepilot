package com.dt.gatepilot.api.resource.node;

import com.dt.gatepilot.api.enums.NodeRole;
import com.dt.gatepilot.api.resource.common.LabelSelector;
import com.dt.gatepilot.api.resource.common.ResourceMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 * 网关节点资源，用于描述 agent / proxy 节点身份和能力。
 */
@Data
public class GatewayNode {

    /**
     * 资源元信息。
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态。
     */
    private GatewayNodeSpec spec = new GatewayNodeSpec();

    /**
     * 当前状态。
     */
    private GatewayNodeStatus status = new GatewayNodeStatus();

    /**
     * 网关节点期望状态。
     */
    @Data
    public static class GatewayNodeSpec {

        /**
         * 节点标识，由 agent 注册时提交。
         */
        private String nodeId;

        /**
         * 节点角色。
         */
        private NodeRole role;

        /**
         * 所属可用区或机房。
         */
        private String zone;

        /**
         * 节点所属隔离组，用于大流量项目定向调度。
         */
        private String isolationGroup;

        /**
         * 节点负责消费的配置分片。
         */
        private List<String> configShards = new ArrayList<>();

        /**
         * 节点可服务的项目选择器。
         */
        private LabelSelector projectSelector;

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
         * 节点能力，例如支持的协议、filter、配置版本。
         */
        private Map<String, String> capabilities = new LinkedHashMap<>();
    }
}
