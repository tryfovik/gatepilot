package com.dt.gatepilot.proxy.infrastructure.metrics;

/**
 * proxy Micrometer 指标常量。
 */
public final class ProxyMetricsConstants {

    /**
     * 路由请求总量指标。
     */
    public static final String ROUTE_REQUESTS_METRIC = "gatepilot.proxy.route.requests";

    /**
     * 路由请求延迟指标。
     */
    public static final String ROUTE_LATENCY_METRIC = "gatepilot.proxy.route.latency";

    /**
     * 路由请求总量说明。
     */
    public static final String ROUTE_REQUESTS_DESCRIPTION = "GatePilot proxy route request count";

    /**
     * 路由请求延迟说明。
     */
    public static final String ROUTE_LATENCY_DESCRIPTION = "GatePilot proxy route request latency";

    /**
     * 路由标识标签。
     */
    public static final String TAG_ROUTE_ID = "route_id";

    /**
     * 状态码标签。
     */
    public static final String TAG_STATUS = "status";

    /**
     * 状态码分组标签。
     */
    public static final String TAG_STATUS_CLASS = "status_class";

    /**
     * 未知标签值。
     */
    public static final String TAG_UNKNOWN = "unknown";

    /**
     * 状态码分组后缀。
     */
    public static final String STATUS_CLASS_SUFFIX = "xx";

    /**
     * 最小 HTTP 状态码。
     */
    public static final int MIN_HTTP_STATUS = 100;

    /**
     * HTTP 状态码分组除数。
     */
    public static final int STATUS_CLASS_DIVISOR = 100;

    private ProxyMetricsConstants() {
        // proxy 指标常量不允许实例化
    }
}
