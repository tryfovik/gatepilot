package com.dt.gatepilot.apiserver.interfaces.rest;

/**
 * apiserver REST 路径常量。
 */
public final class GatePilotApiPaths {

    /**
     * v1 API 前缀。
     */
    public static final String API_V1_PREFIX = "/api/gatepilot/v1";

    /**
     * 资源 API 路径。
     */
    public static final String RESOURCES = API_V1_PREFIX + "/resources";

    /**
     * 发布 API 路径。
     */
    public static final String RELEASES = API_V1_PREFIX + "/releases";

    /**
     * 配置快照 API 路径。
     */
    public static final String CONFIG_SNAPSHOTS = API_V1_PREFIX + "/config-snapshots";

    /**
     * agent API 路径。
     */
    public static final String AGENTS = API_V1_PREFIX + "/agents";

    /**
     * 运行审计查询路径。
     */
    public static final String AUDITS = API_V1_PREFIX + "/audits";

    /**
     * 诊断查询路径。
     */
    public static final String DIAGNOSTICS = API_V1_PREFIX + "/diagnostics";

    /**
     * 模板 API 路径。
     */
    public static final String TEMPLATES = API_V1_PREFIX + "/templates";

    /**
     * agent 注册路径。
     */
    public static final String AGENT_REGISTER = "/register";

    /**
     * agent 心跳路径。
     */
    public static final String AGENT_HEARTBEAT = "/heartbeat";

    /**
     * agent 配置拉取路径。
     */
    public static final String AGENT_CONFIG_PULL = "/configs/pull";

    /**
     * agent 应用结果上报路径。
     */
    public static final String AGENT_APPLY_RESULTS = "/apply-results";

    /**
     * agent 运行审计上报路径。
     */
    public static final String AGENT_AUDITS = "/audits";

    /**
     * 发布 dry-run 路径。
     */
    public static final String RELEASE_DRY_RUN = "/dry-run";

    /**
     * 回滚发布路径。
     */
    public static final String RELEASE_ROLLBACK = "/rollback";

    /**
     * 配置快照 diff 路径。
     */
    public static final String CONFIG_SNAPSHOT_DIFF = "/diff";

    /**
     * 路由目录路径。
     */
    public static final String DIAGNOSTICS_ROUTE_CATALOG = "/route-catalog";

    /**
     * 路由诊断路径。
     */
    public static final String DIAGNOSTICS_ROUTE = "/route";

    /**
     * 项目模板预览路径。
     */
    public static final String TEMPLATE_PROJECT_PREVIEW = "/projects/preview";

    /**
     * 项目模板 dry-run 路径。
     */
    public static final String TEMPLATE_PROJECT_DRY_RUN = "/projects/dry-run";

    /**
     * 项目模板保存路径。
     */
    public static final String TEMPLATE_PROJECT_APPLY = "/projects/apply";

    private GatePilotApiPaths() {
        // REST 路径常量不允许实例化
    }
}
