package com.dt.gatepilot.domain.enums;

/**
 * GatePilot 声明式资源类型。
 */
public enum ResourceKind {

    /**
     * 网关命名空间。
     */
    GATEWAY_NAMESPACE,

    /**
     * 平台团队。
     */
    PLATFORM_TEAM,

    /**
     * 平台环境。
     */
    PLATFORM_ENVIRONMENT,

    /**
     * 配置分片。
     */
    CONFIG_SHARD,

    /**
     * 隔离组。
     */
    ISOLATION_GROUP,

    /**
     * 控制面动态参数。
     */
    CONTROL_PLANE_SETTING,

    /**
     * 注册中心。
     */
    REGISTRY_CENTER,

    /**
     * 流量等级。
     */
    TRAFFIC_TIER,

    /**
     * 入口域名。
     */
    INGRESS_DOMAIN,

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
