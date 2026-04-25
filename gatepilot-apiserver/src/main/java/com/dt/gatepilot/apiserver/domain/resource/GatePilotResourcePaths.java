package com.dt.gatepilot.apiserver.domain.resource;

/**
 * apiserver 资源路径常量。
 */
public final class GatePilotResourcePaths {

    /**
     * 项目资源路径。
     */
    public static final String PROJECTS = "projects";

    /**
     * 路由资源路径。
     */
    public static final String ROUTES = "routes";

    /**
     * 流量策略资源路径。
     */
    public static final String TRAFFIC_POLICIES = "traffic-policies";

    /**
     * 发布策略资源路径。
     */
    public static final String RELEASE_POLICIES = "release-policies";

    /**
     * 认证策略资源路径。
     */
    public static final String AUTH_POLICIES = "auth-policies";

    /**
     * 上游资源路径。
     */
    public static final String UPSTREAMS = "upstreams";

    /**
     * 已发布配置资源路径。
     */
    public static final String PUBLISHED_CONFIGS = "published-configs";

    /**
     * 配置快照资源路径。
     */
    public static final String CONFIG_SNAPSHOTS = "config-snapshots";

    /**
     * 节点资源路径。
     */
    public static final String NODES = "nodes";

    /**
     * 事件资源路径。
     */
    public static final String EVENTS = "events";

    private GatePilotResourcePaths() {
        // 资源路径常量不允许实例化
    }
}
