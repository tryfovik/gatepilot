package com.dt.gatepilot.domain.enums;

/**
 * 上游负载均衡策略。
 */
public enum LoadBalanceStrategy {

    /**
     * 轮询。
     */
    ROUND_ROBIN,

    /**
     * 加权轮询。
     */
    WEIGHTED_ROUND_ROBIN,

    /**
     * 最少连接。
     */
    LEAST_CONNECTIONS,

    /**
     * 随机。
     */
    RANDOM,

    /**
     * 一致性哈希。
     */
    CONSISTENT_HASH
}
