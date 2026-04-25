package com.dt.gatepilot.infrastructure.web;

/**
 * Console Web 装配常量。
 */
public final class ConsoleWebConstants {

    /**
     * 首页资源路径。
     */
    public static final String INDEX_HTML_PATH = "static/index.html";

    /**
     * 根路由。
     */
    public static final String ROOT_ROUTE = "/";

    /**
     * 一级前端路由 fallback。
     */
    public static final String TOP_LEVEL_FALLBACK_ROUTE = "/{path:^(?!api|assets|actuator).*$}";

    /**
     * 多级前端路由 fallback。
     */
    public static final String NESTED_FALLBACK_ROUTE = "/{path:^(?!api|assets|actuator).*$}/**";

    private ConsoleWebConstants() {
        // Console Web 常量不允许实例化
    }
}
