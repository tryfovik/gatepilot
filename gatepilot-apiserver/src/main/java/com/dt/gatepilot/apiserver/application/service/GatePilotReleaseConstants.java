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
     * dry-run 版本片段。
     */
    public static final String DRY_RUN_VERSION_PART = "dry-run";

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

    private GatePilotReleaseConstants() {
        // 发布服务常量不允许实例化
    }
}
