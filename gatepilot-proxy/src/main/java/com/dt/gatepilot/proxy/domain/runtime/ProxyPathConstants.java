package com.dt.gatepilot.proxy.domain.runtime;

/**
 * proxy 路径处理常量。
 */
public final class ProxyPathConstants {

    /**
     * 根路径。
     */
    public static final String ROOT_PATH = "/";

    /**
     * 路径分隔符。
     */
    public static final String PATH_SEPARATOR = "/";

    /**
     * 查询串分隔符。
     */
    public static final String QUERY_SEPARATOR = "?";

    /**
     * host 端口分隔符。
     */
    public static final char HOST_PORT_SEPARATOR = ':';

    /**
     * 通配 host。
     */
    public static final String WILDCARD_HOST = "*";

    /**
     * 资源名分隔符。
     */
    public static final String NAME_SEPARATOR = "-";

    /**
     * 资源名安全字符替换正则。
     */
    public static final String SAFE_NAME_REGEX = "[^a-zA-Z0-9._-]";

    /**
     * 重复路由错误前缀。
     */
    public static final String ERROR_DUPLICATE_ROUTE_MATCH_KEY = "duplicate route match key: ";

    /**
     * 路由 path 缺失错误。
     */
    public static final String ERROR_ROUTE_PATH_BLANK = "route path must not be blank";

    private ProxyPathConstants() {
        // proxy 路径常量不允许实例化
    }
}
