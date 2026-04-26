package com.dt.gatepilot.apiserver.application.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 路由诊断响应。
 */
@Data
public class RouteDiagnosticsResponse {

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 项目名称。
     */
    private String projectName;

    /**
     * 发布版本。
     */
    private String version;

    /**
     * 配置分片。
     */
    private String configShard;

    /**
     * 配置哈希。
     */
    private String configHash;

    /**
     * 配置生成时间。
     */
    private Instant generatedAt;

    /**
     * 是否命中路由。
     */
    private boolean matched;

    /**
     * HTTP 方法。
     */
    private String method;

    /**
     * 请求 Host。
     */
    private String host;

    /**
     * 请求路径。
     */
    private String path;

    /**
     * 命中的路由。
     */
    private RouteView route;

    /**
     * 访问判断。
     */
    private AccessView access;

    /**
     * 染色判断。
     */
    private TrafficView traffic;

    /**
     * 上游判断。
     */
    private UpstreamView upstream;

    /**
     * 治理策略摘要。
     */
    private GovernanceView governance;

    /**
     * 诊断提示。
     */
    private List<String> warnings = new ArrayList<>();

    /**
     * 路由视图。
     */
    @Data
    public static class RouteView {

        /**
         * 路由标识。
         */
        private String routeId;

        /**
         * 路由资源名称。
         */
        private String name;

        /**
         * Host 列表。
         */
        private List<String> hosts = new ArrayList<>();

        /**
         * 路由路径。
         */
        private String path;

        /**
         * 上游名称。
         */
        private String upstreamName;

        /**
         * 策略名称列表。
         */
        private List<String> policyNames = new ArrayList<>();
    }

    /**
     * 访问视图。
     */
    @Data
    public static class AccessView {

        /**
         * 方法是否允许。
         */
        private boolean methodAllowed;

        /**
         * 允许的方法。
         */
        private List<String> allowedMethods = new ArrayList<>();

        /**
         * 是否需要认证。
         */
        private boolean authenticationRequired;

        /**
         * 是否允许匿名。
         */
        private boolean anonymousAllowed;

        /**
         * 认证策略名称。
         */
        private List<String> authPolicyNames = new ArrayList<>();
    }

    /**
     * 染色视图。
     */
    @Data
    public static class TrafficView {

        /**
         * 流量颜色。
         */
        private String color;

        /**
         * 颜色来源。
         */
        private String source;

        /**
         * 染色 Header。
         */
        private String headerName;

        /**
         * 命中的发布分流目标。
         */
        private String releaseTarget;
    }

    /**
     * 上游视图。
     */
    @Data
    public static class UpstreamView {

        /**
         * 上游名称。
         */
        private String name;

        /**
         * 协议。
         */
        private String protocol;

        /**
         * 负载均衡策略。
         */
        private String loadBalance;

        /**
         * 是否存在。
         */
        private boolean available;

        /**
         * 端点数量。
         */
        private int endpointCount;

        /**
         * 是否启用健康检查。
         */
        private Boolean healthCheckEnabled;
    }

    /**
     * 治理视图。
     */
    @Data
    public static class GovernanceView {

        /**
         * 重试配置。
         */
        private RetryView retry = new RetryView();

        /**
         * 限流配置。
         */
        private RateLimitView rateLimit = new RateLimitView();

        /**
         * 熔断配置。
         */
        private CircuitBreakerView circuitBreaker = new CircuitBreakerView();
    }

    /**
     * 重试视图。
     */
    @Data
    public static class RetryView {

        /**
         * 是否启用。
         */
        private boolean enabled;

        /**
         * 最大尝试次数。
         */
        private Integer maxAttempts;

        /**
         * 可重试状态码。
         */
        private List<Integer> statuses = new ArrayList<>();
    }

    /**
     * 限流视图。
     */
    @Data
    public static class RateLimitView {

        /**
         * 是否启用。
         */
        private boolean enabled;

        /**
         * 每秒请求数。
         */
        private Integer requestsPerSecond;

        /**
         * 突发容量。
         */
        private Integer burstCapacity;

        /**
         * 参数限流规则数。
         */
        private int paramRuleCount;
    }

    /**
     * 熔断视图。
     */
    @Data
    public static class CircuitBreakerView {

        /**
         * 是否启用。
         */
        private boolean enabled;

        /**
         * 滑动窗口大小。
         */
        private Integer slidingWindowSize;

        /**
         * 失败率阈值。
         */
        private Integer failureRateThreshold;

        /**
         * fallback 状态码。
         */
        private Integer fallbackStatus;

        /**
         * fallback 提示。
         */
        private String fallbackMessage;
    }
}
