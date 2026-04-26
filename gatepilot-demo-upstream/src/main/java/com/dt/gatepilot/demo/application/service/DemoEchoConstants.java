package com.dt.gatepilot.demo.application.service;

import java.util.List;

/**
 * 示例上游回显常量
 */
public final class DemoEchoConstants {

    /**
     * 默认服务名
     */
    public static final String DEFAULT_SERVICE_NAME = "mock-shop";

    /**
     * 默认版本
     */
    public static final String DEFAULT_VERSION = "stable-v1";

    /**
     * 默认流量颜色
     */
    public static final String DEFAULT_COLOR = "stable";

    /**
     * 默认实例标识
     */
    public static final String DEFAULT_INSTANCE_ID = "mock-shop-stable-1";

    /**
     * 默认部署区域
     */
    public static final String DEFAULT_ZONE = "local-a";

    /**
     * 默认提示
     */
    public static final String DEFAULT_MESSAGE = "GatePilot demo upstream";

    /**
     * 无查询串占位
     */
    public static final String EMPTY_QUERY = "";

    /**
     * GatePilot 颜色请求头
     */
    public static final String HEADER_GATEPILOT_COLOR = "X-GatePilot-Color";

    /**
     * 数据面透传颜色请求头
     */
    public static final String HEADER_TRAFFIC_COLOR = "X-Traffic-Color";

    /**
     * Trace 请求头
     */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * 用户标识请求头
     */
    public static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 租户标识请求头
     */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /**
     * Host 请求头
     */
    public static final String HEADER_HOST = "Host";

    /**
     * 转发来源请求头
     */
    public static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * 转发域名请求头
     */
    public static final String HEADER_FORWARDED_HOST = "X-Forwarded-Host";

    /**
     * 转发协议请求头
     */
    public static final String HEADER_FORWARDED_PROTO = "X-Forwarded-Proto";

    /**
     * 需要回显的请求头
     */
    public static final List<String> ECHO_HEADER_NAMES = List.of(
            HEADER_TRACE_ID,
            HEADER_GATEPILOT_COLOR,
            HEADER_TRAFFIC_COLOR,
            HEADER_USER_ID,
            HEADER_TENANT_ID,
            HEADER_HOST,
            HEADER_FORWARDED_FOR,
            HEADER_FORWARDED_HOST,
            HEADER_FORWARDED_PROTO
    );

    /**
     * 禁止实例化
     */
    private DemoEchoConstants() {
        // 示例常量不允许实例化
    }
}
