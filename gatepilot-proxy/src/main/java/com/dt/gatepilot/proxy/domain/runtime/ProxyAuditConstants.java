package com.dt.gatepilot.proxy.domain.runtime;

/**
 * proxy 运行审计常量。
 */
public final class ProxyAuditConstants {

    /**
     * 默认 Trace 请求头名称。
     */
    public static final String DEFAULT_TRACE_HEADER_NAME = "X-Trace-Id";

    /**
     * 成功结果。
     */
    public static final String OUTCOME_SUCCESS = "success";

    /**
     * 阻断结果。
     */
    public static final String OUTCOME_BLOCKED = "blocked";

    /**
     * 异常结果。
     */
    public static final String OUTCOME_ERROR = "error";

    /**
     * fallback 结果。
     */
    public static final String OUTCOME_FALLBACK = "fallback";

    /**
     * 运行态未就绪。
     */
    public static final String REASON_RUNTIME_EMPTY = "runtime_empty";

    /**
     * 未命中路由。
     */
    public static final String REASON_ROUTE_NOT_MATCHED = "route_not_matched";

    /**
     * 方法不允许。
     */
    public static final String REASON_METHOD_NOT_ALLOWED = "method_not_allowed";

    /**
     * 需要认证。
     */
    public static final String REASON_AUTHENTICATION_REQUIRED = "authentication_required";

    /**
     * 上游缺失。
     */
    public static final String REASON_UPSTREAM_MISSING = "upstream_missing";

    /**
     * 上游响应。
     */
    public static final String REASON_UPSTREAM_RESPONSE = "upstream_response";

    /**
     * 内部入口访问拒绝。
     */
    public static final String REASON_INTERNAL_ACCESS_DENIED = "internal_access_denied";

    /**
     * 上游异常。
     */
    public static final String REASON_UPSTREAM_ERROR = "upstream_error";

    /**
     * 限流拒绝。
     */
    public static final String REASON_RATE_LIMITED = "rate_limited";

    /**
     * 限流组件不可用。
     */
    public static final String REASON_RATE_LIMITER_UNAVAILABLE = "rate_limiter_unavailable";

    /**
     * 熔断器已打开。
     */
    public static final String REASON_CIRCUIT_BREAKER_OPEN = "circuit_breaker_open";

    /**
     * 默认错误状态。
     */
    public static final int DEFAULT_ERROR_STATUS = 500;

    /**
     * 纳秒到毫秒的换算值。
     */
    public static final long NANOS_PER_MILLISECOND = 1_000_000L;

    private ProxyAuditConstants() {
        // 运行审计常量不允许实例化
    }
}
