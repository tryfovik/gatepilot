package com.dt.gatepilot.api.enums;

/**
 * GatePilot 声明式资源类型。
 */
public enum ResourceKind {

    /**
     * 网关项目。
     */
    GATEWAY_PROJECT,

    /**
     * 网关路由。
     */
    GATEWAY_ROUTE,

    /**
     * 流量治理策略。
     */
    TRAFFIC_POLICY,

    /**
     * 发布策略。
     */
    RELEASE_POLICY,

    /**
     * 认证策略。
     */
    AUTH_POLICY,

    /**
     * 上游服务。
     */
    UPSTREAM,

    /**
     * 已发布配置。
     */
    PUBLISHED_CONFIG,

    /**
     * 配置版本快照。
     */
    CONFIG_SNAPSHOT,

    /**
     * 网关节点。
     */
    GATEWAY_NODE,

    /**
     * 网关事件。
     */
    GATEWAY_EVENT
}
