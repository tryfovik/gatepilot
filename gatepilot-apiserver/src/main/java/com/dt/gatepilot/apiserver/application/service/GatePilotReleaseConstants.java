package com.dt.gatepilot.apiserver.application.service;

/**
 * 发布请求服务常量。
 */
public final class GatePilotReleaseConstants {

    /**
     * 发布单前缀。
     */
    public static final String RELEASE_ID_PREFIX = "rel-";

    /**
     * 回滚单前缀。
     */
    public static final String ROLLBACK_ID_PREFIX = "rb-";

    /**
     * 版本分隔符。
     */
    public static final String VERSION_SEPARATOR = "-";

    /**
     * 空版本片段。
     */
    public static final String EMPTY_VERSION_PART = "";

    /**
     * 版本唯一后缀长度。
     */
    public static final int VERSION_ID_SUFFIX_LENGTH = 32;

    /**
     * dry-run 单页扫描大小。
     */
    public static final int DRY_RUN_LOOKUP_LIMIT = 500;

    /**
     * 引用关系展示分隔符。
     */
    public static final String REFERENCE_SEPARATOR = " -> ";

    /**
     * 空引用展示值。
     */
    public static final String EMPTY_REFERENCE_VALUE = "";

    /**
     * dry-run 版本片段。
     */
    public static final String DRY_RUN_VERSION_PART = "dry-run";

    /**
     * 发布版本片段。
     */
    public static final String RELEASE_VERSION_PART = "release";

    /**
     * 回滚版本片段。
     */
    public static final String ROLLBACK_VERSION_PART = "rollback";

    /**
     * 待处理阶段。
     */
    public static final String PHASE_PENDING = "PENDING";

    /**
     * dry-run 错误等级。
     */
    public static final String DRY_RUN_LEVEL_ERROR = "ERROR";

    /**
     * dry-run 警告等级。
     */
    public static final String DRY_RUN_LEVEL_WARN = "WARN";

    /**
     * 项目不存在原因码。
     */
    public static final String REASON_PROJECT_NOT_FOUND = "ProjectNotFound";

    /**
     * 无路由原因码。
     */
    public static final String REASON_NO_ROUTE = "NoRoute";

    /**
     * 无上游原因码。
     */
    public static final String REASON_NO_UPSTREAM = "NoUpstream";

    /**
     * 路由路径缺失原因码。
     */
    public static final String REASON_ROUTE_PATH_MISSING = "RoutePathMissing";

    /**
     * 路由域名缺失原因码。
     */
    public static final String REASON_ROUTE_HOST_MISSING = "RouteHostMissing";

    /**
     * 路由上游缺失原因码。
     */
    public static final String REASON_ROUTE_UPSTREAM_MISSING = "RouteUpstreamMissing";

    /**
     * 路由策略缺失原因码。
     */
    public static final String REASON_ROUTE_POLICY_MISSING = "RoutePolicyMissing";

    /**
     * 上游端点缺失原因码。
     */
    public static final String REASON_UPSTREAM_ENDPOINT_MISSING = "UpstreamEndpointMissing";

    /**
     * 上游负载均衡策略不支持原因码。
     */
    public static final String REASON_UPSTREAM_LOAD_BALANCE_UNSUPPORTED = "UpstreamLoadBalanceUnsupported";

    /**
     * 发布策略上游缺失原因码。
     */
    public static final String REASON_RELEASE_UPSTREAM_MISSING = "ReleaseUpstreamMissing";

    /**
     * 项目不存在提示。
     */
    public static final String MESSAGE_PROJECT_NOT_FOUND = "项目资源不存在，不能发布";

    /**
     * 目标快照不存在提示。
     */
    public static final String MESSAGE_TARGET_SNAPSHOT_NOT_FOUND = "目标快照不存在";

    /**
     * 发布请求已创建提示。
     */
    public static final String MESSAGE_RELEASE_REQUEST_CREATED = "发布请求已创建，等待 controller-manager 推进";

    /**
     * 回滚请求已创建提示。
     */
    public static final String MESSAGE_ROLLBACK_REQUEST_CREATED = "回滚请求已创建，等待 controller-manager 推进";

    /**
     * 无路由提示。
     */
    public static final String MESSAGE_NO_ROUTE = "当前项目没有路由资源，本次发布不会产生业务入口";

    /**
     * 无上游提示。
     */
    public static final String MESSAGE_NO_UPSTREAM = "当前项目没有上游资源，路由可能无法转发";

    /**
     * 路由路径缺失提示前缀。
     */
    public static final String MESSAGE_ROUTE_PATH_MISSING_PREFIX = "路由未配置入口路径: ";

    /**
     * 路由域名缺失提示前缀。
     */
    public static final String MESSAGE_ROUTE_HOST_MISSING_PREFIX = "路由未配置入口域名: ";

    /**
     * 路由上游缺失提示前缀。
     */
    public static final String MESSAGE_ROUTE_UPSTREAM_MISSING_PREFIX = "路由引用的上游不存在: ";

    /**
     * 路由策略缺失提示前缀。
     */
    public static final String MESSAGE_ROUTE_POLICY_MISSING_PREFIX = "路由引用的策略不存在: ";

    /**
     * 上游端点缺失提示前缀。
     */
    public static final String MESSAGE_UPSTREAM_ENDPOINT_MISSING_PREFIX = "上游没有可用端点: ";

    /**
     * 上游负载均衡策略不支持提示前缀。
     */
    public static final String MESSAGE_UPSTREAM_LOAD_BALANCE_UNSUPPORTED_PREFIX =
            "上游负载均衡策略当前没有成熟组件承接: ";

    /**
     * 发布策略上游缺失提示前缀。
     */
    public static final String MESSAGE_RELEASE_UPSTREAM_MISSING_PREFIX = "发布策略引用的上游不存在: ";

    private GatePilotReleaseConstants() {
        // 发布服务常量不允许实例化
    }
}
