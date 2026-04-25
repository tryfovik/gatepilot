package com.dt.gatepilot.domain.resource.node;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.enums.NodePhase;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 网关节点实际状态，由 agent 上报、控制面汇总。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GatewayNodeStatus extends ResourceStatus {

    /**
     * 节点运行状态。
     */
    private NodePhase nodePhase;

    /**
     * 节点最近一次心跳时间。
     */
    private Instant lastHeartbeatAt;

    /**
     * 当前已应用配置版本。
     */
    private String currentConfigVersion;

    /**
     * 当前期望配置版本。
     */
    private String desiredConfigVersion;

    /**
     * 最近一次成功应用的 last-good 版本。
     */
    private String lastGoodConfigVersion;

    /**
     * 当前配置应用状态。
     */
    private ConfigApplyState applyState;

    /**
     * 最近一次应用结果。
     */
    private ApplyResult lastApplyResult = new ApplyResult();

    /**
     * 节点健康摘要。
     */
    private NodeHealth health = new NodeHealth();

    /**
     * 节点指标摘要。
     */
    private MetricsSummary metrics = new MetricsSummary();

    /**
     * 当前加载的路由数量。
     */
    private Integer loadedRouteCount;

    /**
     * 当前加载的上游数量。
     */
    private Integer loadedUpstreamCount;

    /**
     * 当前加载的策略数量。
     */
    private Integer loadedPolicyCount;

    /**
     * 上游健康摘要。
     */
    private List<UpstreamHealth> upstreamHealth = new ArrayList<>();

    /**
     * 配置应用结果。
     */
    @Data
    public static class ApplyResult {

        /**
         * 发布版本。
         */
        private String version;

        /**
         * 配置内容哈希。
         */
        private String configHash;

        /**
         * 应用状态。
         */
        private ConfigApplyState state;

        /**
         * 开始应用时间。
         */
        private Instant startedAt;

        /**
         * 结束应用时间。
         */
        private Instant finishedAt;

        /**
         * 失败原因码。
         */
        private String reason;

        /**
         * 失败或结果说明。
         */
        private String message;
    }

    /**
     * 节点健康摘要。
     */
    @Data
    public static class NodeHealth {

        /**
         * agent 是否健康。
         */
        private Boolean agentHealthy;

        /**
         * proxy 是否健康。
         */
        private Boolean proxyHealthy;

        /**
         * 控制面连接是否健康。
         */
        private Boolean controlPlaneConnected;

        /**
         * 健康说明。
         */
        private String message;
    }

    /**
     * 节点指标摘要。
     */
    @Data
    public static class MetricsSummary {

        /**
         * 每秒请求数。
         */
        private Double requestsPerSecond;

        /**
         * 最近窗口错误率。
         */
        private Double errorRate;

        /**
         * P95 延迟，单位毫秒。
         */
        private Double p95LatencyMillis;

        /**
         * 活跃连接数。
         */
        private Long activeConnections;

        /**
         * 自定义指标。
         */
        private Map<String, String> custom = new LinkedHashMap<>();
    }

    /**
     * 上游健康摘要。
     */
    @Data
    public static class UpstreamHealth {

        /**
         * 上游名称。
         */
        private String upstreamName;

        /**
         * 健康端点数量。
         */
        private Integer healthyEndpointCount;

        /**
         * 不健康端点数量。
         */
        private Integer unhealthyEndpointCount;

        /**
         * 最近检查时间。
         */
        private Instant checkedAt;
    }
}
