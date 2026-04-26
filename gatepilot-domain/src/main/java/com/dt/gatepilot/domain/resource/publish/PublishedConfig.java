package com.dt.gatepilot.domain.resource.publish;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.enums.HttpMethod;
import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.domain.resource.meta.LabelSelector;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 已发布配置，是 agent 和 proxy 唯一消费的配置产物。
 */
@Data
public class PublishedConfig {

    /**
     * 资源元信息。
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 发布配置内容。
     */
    private PublishedConfigSpec spec = new PublishedConfigSpec();

    /**
     * 发布配置状态。
     */
    private PublishedConfigStatus status = new PublishedConfigStatus();

    /**
     * 已发布配置内容。
     */
    @Data
    public static class PublishedConfigSpec {

        /**
         * 所属项目。
         */
        private ResourceReference projectRef;

        /**
         * 发布版本。
         */
        private String version;

        /**
         * 配置内容哈希。
         */
        private String configHash;

        /**
         * 配置分片键，用于避免所有节点消费全量配置。
         */
        private String configShard;

        /**
         * 隔离组，用于约束高流量项目只下发到对应网关副本池
         */
        private String isolationGroup;

        /**
         * 配置序号，同一分片内单调递增。
         */
        private Long sequence;

        /**
         * 是否为全量快照，false 表示基于 baseVersion 的增量补丁。
         */
        private Boolean fullSnapshot;

        /**
         * 增量补丁依赖的基础版本。
         */
        private String baseVersion;

        /**
         * 生成时间。
         */
        private Instant generatedAt;

        /**
         * 最低兼容 agent 版本。
         */
        private String minAgentVersion;

        /**
         * 最低兼容 proxy 版本。
         */
        private String minProxyVersion;

        /**
         * 本次发布包含的路由。
         */
        private List<PublishedRoute> routes = new ArrayList<>();

        /**
         * 本次发布包含的上游。
         */
        private List<PublishedUpstream> upstreams = new ArrayList<>();

        /**
         * 本次发布包含的策略快照。
         */
        private List<PublishedPolicy> policies = new ArrayList<>();

        /**
         * 本次发布目标节点引用，为空时由 targetNodeSelector 决定。
         */
        private List<ResourceReference> targetNodeRefs = new ArrayList<>();

        /**
         * 本次发布目标节点选择器。
         */
        private LabelSelector targetNodeSelector;

        /**
         * 扩展载荷，留给后续 filter 或 adapter 消费。
         */
        private Map<String, Object> extensions = new LinkedHashMap<>();
    }

    /**
     * 发布后的路由快照。
     */
    @Data
    public static class PublishedRoute {

        /**
         * 路由标识。
         */
        private String routeId;

        /**
         * 来源路由资源。
         */
        private ResourceReference sourceRef;

        /**
         * 入口协议。
         */
        private List<Protocol> protocols = new ArrayList<>();

        /**
         * 入口域名。
         */
        private List<String> hosts = new ArrayList<>();

        /**
         * 路径匹配表达式。
         */
        private String path;

        /**
         * 允许的 HTTP 方法，为空表示不限制。
         */
        private List<HttpMethod> methods = new ArrayList<>();

        /**
         * 是否剥离入口匹配前缀。
         */
        private Boolean stripPrefix;

        /**
         * 转发到上游前改写的路径前缀。
         */
        private String rewritePathPrefix;

        /**
         * 转发时追加的请求头。
         */
        private Map<String, String> addHeaders = new LinkedHashMap<>();

        /**
         * 转发时移除的请求头。
         */
        private List<String> removeHeaders = new ArrayList<>();

        /**
         * 目标上游名称。
         */
        private String upstreamName;

        /**
         * 应用到该路由的策略名称。
         */
        private List<String> policyNames = new ArrayList<>();
    }

    /**
     * 发布后的上游快照。
     */
    @Data
    public static class PublishedUpstream {

        /**
         * 上游名称。
         */
        private String name;

        /**
         * 来源上游资源。
         */
        private ResourceReference sourceRef;

        /**
         * 上游协议。
         */
        private Protocol protocol;

        /**
         * 负载均衡策略。
         */
        private String loadBalance;

        /**
         * 端点列表。
         */
        private List<PublishedEndpoint> endpoints = new ArrayList<>();

        /**
         * 主动健康检查配置。
         */
        private PublishedHealthCheck healthCheck = new PublishedHealthCheck();
    }

    /**
     * 发布后的上游端点。
     */
    @Data
    public static class PublishedEndpoint {

        /**
         * 端点地址。
         */
        private String host;

        /**
         * 端点端口。
         */
        private Integer port;

        /**
         * 端点权重。
         */
        private Integer weight;
    }

    /**
     * 发布后的主动健康检查配置。
     */
    @Data
    public static class PublishedHealthCheck {

        /**
         * 是否启用健康检查。
         */
        private Boolean enabled;

        /**
         * 健康检查路径。
         */
        private String path;

        /**
         * 健康检查间隔。
         */
        private Duration interval;

        /**
         * 请求超时时间。
         */
        private Duration timeout;

        /**
         * 连续成功阈值。
         */
        private Integer healthyThreshold;

        /**
         * 连续失败阈值。
         */
        private Integer unhealthyThreshold;
    }

    /**
     * 发布后的策略快照。
     */
    @Data
    public static class PublishedPolicy {

        /**
         * 策略名称。
         */
        private String name;

        /**
         * 来源策略资源。
         */
        private ResourceReference sourceRef;

        /**
         * 策略类型。
         */
        private String type;

        /**
         * 策略配置。
         */
        private Map<String, Object> config = new LinkedHashMap<>();
    }

    /**
     * 已发布配置实际状态。
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PublishedConfigStatus extends ResourceStatus {

        /**
         * 期望应用节点数。
         */
        private Integer desiredNodeCount;

        /**
         * 已应用节点数。
         */
        private Integer appliedNodeCount;

        /**
         * 应用失败节点数。
         */
        private Integer failedNodeCount;

        /**
         * 整体应用状态。
         */
        private ConfigApplyState applyState;

        /**
         * 每个节点的应用结果，用于多副本发布排障。
         */
        private List<NodeApplyResult> nodeApplyResults = new ArrayList<>();
    }

    /**
     * 单个节点的配置应用结果。
     */
    @Data
    public static class NodeApplyResult {

        /**
         * 节点标识。
         */
        private String nodeId;

        /**
         * 节点资源引用。
         */
        private ResourceReference nodeRef;

        /**
         * 节点所在可用区或机房。
         */
        private String zone;

        /**
         * 应用状态。
         */
        private ConfigApplyState state;

        /**
         * 节点应用到的配置版本。
         */
        private String appliedVersion;

        /**
         * 节点应用到的配置哈希。
         */
        private String configHash;

        /**
         * 应用完成时间。
         */
        private Instant appliedAt;

        /**
         * 失败原因码。
         */
        private String reason;

        /**
         * 失败或状态说明。
         */
        private String message;
    }
}
