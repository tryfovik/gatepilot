package com.dt.gatepilot.domain.resource.policy;

import com.dt.gatepilot.domain.enums.TrafficColorSource;
import com.dt.gatepilot.domain.resource.meta.LabelSelector;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流量治理策略，承载限流、熔断、重试、超时和流量染色等规则。
 */
@Data
public class TrafficPolicy {

    /**
     * 资源元信息。
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态。
     */
    private TrafficPolicySpec spec = new TrafficPolicySpec();

    /**
     * 当前状态。
     */
    private TrafficPolicyStatus status = new TrafficPolicyStatus();

    /**
     * 流量治理期望状态。
     */
    @Data
    public static class TrafficPolicySpec {

        /**
         * 所属项目。
         */
        private ResourceReference projectRef;

        /**
         * 显式绑定的目标资源。
         */
        private List<ResourceReference> targetRefs = new ArrayList<>();

        /**
         * 按标签绑定的目标资源。
         */
        private LabelSelector targetSelector;

        /**
         * 超时配置。
         */
        private TimeoutPolicy timeout = new TimeoutPolicy();

        /**
         * 重试配置。
         */
        private RetryPolicy retry = new RetryPolicy();

        /**
         * 熔断配置。
         */
        private CircuitBreakerPolicy circuitBreaker = new CircuitBreakerPolicy();

        /**
         * 限流配置。
         */
        private RateLimitPolicy rateLimit = new RateLimitPolicy();

        /**
         * 流量染色规则。
         */
        private List<TrafficColorRule> colorRules = new ArrayList<>();
    }

    /**
     * 超时配置。
     */
    @Data
    public static class TimeoutPolicy {

        /**
         * 连接超时时间。
         */
        private Duration connectTimeout;

        /**
         * 响应超时时间。
         */
        private Duration responseTimeout;
    }

    /**
     * 重试配置。
     */
    @Data
    public static class RetryPolicy {

        /**
         * 是否启用重试。
         */
        private Boolean enabled;

        /**
         * 最大重试次数。
         */
        private Integer maxAttempts;

        /**
         * 可重试状态码。
         */
        private List<Integer> statuses = new ArrayList<>();

        /**
         * 初始退避时间。
         */
        private Duration firstBackoff;

        /**
         * 最大退避时间。
         */
        private Duration maxBackoff;
    }

    /**
     * 熔断配置。
     */
    @Data
    public static class CircuitBreakerPolicy {

        /**
         * 是否启用熔断。
         */
        private Boolean enabled;

        /**
         * 滑动窗口大小。
         */
        private Integer slidingWindowSize;

        /**
         * 熔断窗口内最小请求数。
         */
        private Integer minimumNumberOfCalls;

        /**
         * 失败率阈值。
         */
        private Integer failureRateThreshold;

        /**
         * 慢调用率阈值。
         */
        private Integer slowCallRateThreshold;

        /**
         * 慢调用阈值。
         */
        private Duration slowCallDurationThreshold;

        /**
         * 熔断打开后的等待时间。
         */
        private Duration waitDurationInOpenState;

        /**
         * 半开状态允许的探测请求数。
         */
        private Integer permittedNumberOfCallsInHalfOpenState;

        /**
         * 视为失败的 HTTP 状态码。
         */
        private List<Integer> statusCodes = new ArrayList<>();

        /**
         * fallback HTTP 状态。
         */
        private Integer fallbackStatus;

        /**
         * fallback 业务码。
         */
        private Integer fallbackCode;

        /**
         * fallback 提示。
         */
        private String fallbackMessage;

        /**
         * fallback 响应类型。
         */
        private String contentType;
    }

    /**
     * 限流配置。
     */
    @Data
    public static class RateLimitPolicy {

        /**
         * 是否启用限流。
         */
        private Boolean enabled;

        /**
         * 每秒允许请求数。
         */
        private Integer requestsPerSecond;

        /**
         * 突发容量。
         */
        private Integer burstCapacity;

        /**
         * 参数级限流规则。
         */
        private List<ParamLimitRule> paramRules = new ArrayList<>();
    }

    /**
     * 参数级限流规则。
     */
    @Data
    public static class ParamLimitRule {

        /**
         * 参数来源，例如 header、query、cookie。
         */
        private String source;

        /**
         * 参数名称。
         */
        private String name;

        /**
         * 参数值。
         */
        private String value;

        /**
         * 每秒允许请求数。
         */
        private Integer requestsPerSecond;
    }

    /**
     * 流量染色规则。
     */
    @Data
    public static class TrafficColorRule {

        /**
         * 染色来源。
         */
        private TrafficColorSource source;

        /**
         * 来源字段名。
         */
        private String key;

        /**
         * 匹配表达式。
         */
        private String match;

        /**
         * 写入网关上下文的颜色值。
         */
        private String color;

        /**
         * 传递给上游的请求头。
         */
        private Map<String, String> propagateHeaders = new LinkedHashMap<>();
    }

    /**
     * 流量治理实际状态。
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TrafficPolicyStatus extends ResourceStatus {

        /**
         * 当前绑定到的资源数量。
         */
        private Integer boundResourceCount;

        /**
         * 当前生效的发布版本。
         */
        private String currentPublishedVersion;
    }
}
