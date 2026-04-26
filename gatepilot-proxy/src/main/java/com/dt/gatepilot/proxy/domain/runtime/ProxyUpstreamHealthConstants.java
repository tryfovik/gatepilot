package com.dt.gatepilot.proxy.domain.runtime;

import java.time.Duration;

/**
 * proxy 上游健康检查常量。
 */
public final class ProxyUpstreamHealthConstants {

    /**
     * 默认健康检查路径。
     */
    public static final String DEFAULT_HEALTH_PATH = "/actuator/health";

    /**
     * 默认健康检查超时。
     */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2);

    /**
     * 默认连续成功阈值。
     */
    public static final int DEFAULT_HEALTHY_THRESHOLD = 1;

    /**
     * 默认连续失败阈值。
     */
    public static final int DEFAULT_UNHEALTHY_THRESHOLD = 2;

    /**
     * 端点 key 分隔符。
     */
    public static final String ENDPOINT_KEY_SEPARATOR = ":";

    /**
     * HTTP 协议。
     */
    public static final String SCHEME_HTTP = "http";

    /**
     * HTTPS 协议。
     */
    public static final String SCHEME_HTTPS = "https";

    /**
     * 调度开关配置前缀。
     */
    public static final String CONFIG_PREFIX = "gatepilot.proxy.health-check";

    /**
     * 调度开关配置名。
     */
    public static final String ENABLED_PROPERTY = "enabled";

    /**
     * 配置开启值。
     */
    public static final String ENABLED_VALUE = "true";

    /**
     * 探测调度间隔占位符。
     */
    public static final String INTERVAL_PLACEHOLDER = "${gatepilot.proxy.health-check.interval-ms:30000}";

    /**
     * 探测初始延迟占位符。
     */
    public static final String INITIAL_DELAY_PLACEHOLDER =
            "${gatepilot.proxy.health-check.initial-delay-ms:10000}";

    /**
     * 隐藏工具类构造器。
     */
    private ProxyUpstreamHealthConstants() {
        // proxy 上游健康检查常量不允许实例化
    }
}
