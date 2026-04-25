package com.dt.gatepilot.domain.enums;

/**
 * 流量染色来源。
 */
public enum TrafficColorSource {

    /**
     * 请求头。
     */
    HEADER,

    /**
     * Cookie。
     */
    COOKIE,

    /**
     * 查询参数。
     */
    QUERY,

    /**
     * JWT Claim。
     */
    JWT_CLAIM,

    /**
     * 网关运行上下文。
     */
    CONTEXT
}
