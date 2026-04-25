package com.dt.gatepilot.api.resource.upstream;

import com.dt.gatepilot.api.enums.LoadBalanceStrategy;
import com.dt.gatepilot.api.enums.Protocol;
import com.dt.gatepilot.api.resource.common.ResourceMetadata;
import com.dt.gatepilot.api.resource.common.ResourceReference;
import com.dt.gatepilot.api.resource.common.ResourceStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 上游服务资源，用于描述 proxy 可转发的后端服务集合。
 */
@Data
public class Upstream {

    /**
     * 资源元信息。
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态。
     */
    private UpstreamSpec spec = new UpstreamSpec();

    /**
     * 当前状态。
     */
    private UpstreamStatus status = new UpstreamStatus();

    /**
     * 上游服务期望状态。
     */
    @Data
    public static class UpstreamSpec {

        /**
         * 所属项目。
         */
        private ResourceReference projectRef;

        /**
         * 上游协议。
         */
        private Protocol protocol;

        /**
         * 负载均衡策略。
         */
        private LoadBalanceStrategy loadBalance;

        /**
         * 上游端点列表。
         */
        private List<UpstreamEndpoint> endpoints = new ArrayList<>();

        /**
         * 主动健康检查配置。
         */
        private HealthCheckSpec healthCheck = new HealthCheckSpec();

        /**
         * 连接池配置。
         */
        private ConnectionPoolSpec connectionPool = new ConnectionPoolSpec();
    }

    /**
     * 上游端点。
     */
    @Data
    public static class UpstreamEndpoint {

        /**
         * 端点主机名或 IP。
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

        /**
         * 端点标签。
         */
        private Map<String, String> labels = new LinkedHashMap<>();
    }

    /**
     * 主动健康检查配置。
     */
    @Data
    public static class HealthCheckSpec {

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
     * 上游连接池配置。
     */
    @Data
    public static class ConnectionPoolSpec {

        /**
         * 最大连接数。
         */
        private Integer maxConnections;

        /**
         * 最大空闲时间。
         */
        private Duration maxIdleTime;

        /**
         * 获取连接等待时间。
         */
        private Duration pendingAcquireTimeout;
    }

    /**
     * 上游实际状态。
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UpstreamStatus extends ResourceStatus {

        /**
         * 健康端点数量。
         */
        private Integer healthyEndpointCount;

        /**
         * 不健康端点数量。
         */
        private Integer unhealthyEndpointCount;

        /**
         * 当前生效的发布版本。
         */
        private String currentPublishedVersion;
    }
}
