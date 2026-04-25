package com.dt.gatepilot.proxy.domain.runtime;

/**
 * 限流许可申请结果。
 */
public enum RateLimitAcquireResult {

    /**
     * 已获取许可。
     */
    ALLOWED,

    /**
     * 请求被限流。
     */
    REJECTED,

    /**
     * 限流组件不可用。
     */
    UNAVAILABLE
}
