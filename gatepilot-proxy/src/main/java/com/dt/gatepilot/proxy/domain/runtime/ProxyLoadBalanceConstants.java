package com.dt.gatepilot.proxy.domain.runtime;

/**
 * proxy 上游负载均衡常量。
 */
public final class ProxyLoadBalanceConstants {

    /**
     * 默认负载均衡策略。
     */
    public static final String DEFAULT_STRATEGY = "ROUND_ROBIN";

    /**
     * 加权轮询策略。
     */
    public static final String STRATEGY_WEIGHTED_ROUND_ROBIN = "WEIGHTED_ROUND_ROBIN";

    /**
     * 随机策略。
     */
    public static final String STRATEGY_RANDOM = "RANDOM";

    /**
     * 最小端点权重。
     */
    public static final int MIN_ENDPOINT_WEIGHT = 1;

    /**
     * 隐藏工具类构造器。
     */
    private ProxyLoadBalanceConstants() {
        // proxy 负载均衡常量不允许实例化
    }
}
