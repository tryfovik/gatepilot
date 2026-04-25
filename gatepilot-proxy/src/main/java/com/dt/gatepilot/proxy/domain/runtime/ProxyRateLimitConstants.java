package com.dt.gatepilot.proxy.domain.runtime;

/**
 * proxy 限流运行常量。
 */
public final class ProxyRateLimitConstants {

    /**
     * 限流 key 分隔符。
     */
    public static final String LIMITER_KEY_SEPARATOR = ":";

    /**
     * 路由级限流标识。
     */
    public static final String ROUTE_SCOPE = "route";

    /**
     * 参数级限流标识。
     */
    public static final String PARAM_SCOPE = "param";

    /**
     * Header 参数来源。
     */
    public static final String SOURCE_HEADER = "header";

    /**
     * Query 参数来源。
     */
    public static final String SOURCE_QUERY = "query";

    /**
     * URL 参数来源。
     */
    public static final String SOURCE_URL_PARAM = "url-param";

    /**
     * Cookie 参数来源。
     */
    public static final String SOURCE_COOKIE = "cookie";

    /**
     * 客户端 IP 参数来源。
     */
    public static final String SOURCE_IP = "ip";

    /**
     * Sentinel 风格客户端 IP 参数来源。
     */
    public static final String SOURCE_CLIENT_IP = "client-ip";

    /**
     * Host 参数来源。
     */
    public static final String SOURCE_HOST = "host";

    /**
     * getboot-limiter 默认周期。
     */
    public static final long DEFAULT_INTERVAL = 1L;

    /**
     * getboot-limiter 默认周期单位。
     */
    public static final String DEFAULT_INTERVAL_UNIT = "SECONDS";

    /**
     * 单次请求默认许可数。
     */
    public static final long DEFAULT_PERMITS = 1L;

    /**
     * 默认限流等待时间。
     */
    public static final long DEFAULT_TIMEOUT = 0L;

    /**
     * 默认限流 HTTP 状态。
     */
    public static final int DEFAULT_REJECT_STATUS = 429;

    /**
     * 默认限流业务码。
     */
    public static final int DEFAULT_REJECT_CODE = 429;

    /**
     * 默认限流提示。
     */
    public static final String DEFAULT_REJECT_MESSAGE = "请求过于频繁，请稍后重试";

    /**
     * 默认限流响应类型。
     */
    public static final String DEFAULT_REJECT_CONTENT_TYPE = "application/json";

    /**
     * 限流组件不可用 HTTP 状态。
     */
    public static final int UNAVAILABLE_STATUS = 503;

    /**
     * 限流组件不可用业务码。
     */
    public static final int UNAVAILABLE_CODE = 503;

    /**
     * 限流组件不可用提示。
     */
    public static final String UNAVAILABLE_MESSAGE = "网关限流组件未就绪";

    /**
     * 隐藏工具类构造器。
     */
    private ProxyRateLimitConstants() {
        // proxy 限流常量不允许实例化
    }
}
