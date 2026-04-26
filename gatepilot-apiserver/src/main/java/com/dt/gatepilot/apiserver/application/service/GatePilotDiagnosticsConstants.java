package com.dt.gatepilot.apiserver.application.service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * GatePilot 诊断常量。
 */
public final class GatePilotDiagnosticsConstants {

    /**
     * 默认命名空间。
     */
    public static final String DEFAULT_NAMESPACE = "default";

    /**
     * 默认 HTTP 方法。
     */
    public static final String DEFAULT_METHOD = "GET";

    /**
     * Host Header。
     */
    public static final String HOST_HEADER = "Host";

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
     * Host 端口分隔符。
     */
    public static final String HOST_PORT_SEPARATOR = ":";

    /**
     * 默认染色 Header。
     */
    public static final String DEFAULT_COLOR_HEADER = "X-Traffic-Color";

    /**
     * 默认颜色。
     */
    public static final String DEFAULT_COLOR = "stable";

    /**
     * Header 来源。
     */
    public static final String SOURCE_HEADER = "header";

    /**
     * Cookie 来源。
     */
    public static final String SOURCE_COOKIE = "cookie";

    /**
     * Query 来源。
     */
    public static final String SOURCE_QUERY = "query";

    /**
     * IP 来源。
     */
    public static final String SOURCE_IP = "ip";

    /**
     * client-ip 来源。
     */
    public static final String SOURCE_CLIENT_IP = "client-ip";

    /**
     * client_ip 来源。
     */
    public static final String SOURCE_CLIENT_IP_UNDERSCORE = "client_ip";

    /**
     * remote-address 来源。
     */
    public static final String SOURCE_REMOTE_ADDRESS = "remote-address";

    /**
     * remote_address 来源。
     */
    public static final String SOURCE_REMOTE_ADDRESS_UNDERSCORE = "remote_address";

    /**
     * remoteaddress 来源。
     */
    public static final String SOURCE_REMOTE_ADDRESS_COMPACT = "remoteaddress";

    /**
     * 远端地址字段。
     */
    public static final String FIELD_REMOTE_ADDRESS = "remoteAddress";

    /**
     * 精确匹配。
     */
    public static final String MATCH_EXACT = "exact";

    /**
     * 前缀匹配。
     */
    public static final String MATCH_PREFIX = "prefix";

    /**
     * 包含匹配。
     */
    public static final String MATCH_CONTAINS = "contains";

    /**
     * 正则匹配。
     */
    public static final String MATCH_REGEX = "regex";

    /**
     * Header 颜色来源。
     */
    public static final String COLOR_SOURCE_HEADER = "HEADER";

    /**
     * 规则颜色来源。
     */
    public static final String COLOR_SOURCE_RULE = "RULE";

    /**
     * 权重颜色来源。
     */
    public static final String COLOR_SOURCE_WEIGHT = "WEIGHT";

    /**
     * 默认颜色来源。
     */
    public static final String COLOR_SOURCE_DEFAULT = "DEFAULT";

    /**
     * 权重 hash Header 默认顺序。
     */
    public static final List<String> DEFAULT_WEIGHT_HASH_HEADERS =
            List.of("X-User-Id", "X-Tenant-Id", "X-Trace-Id");

    /**
     * 权重桶总数。
     */
    public static final int WEIGHT_BUCKET_SIZE = 100;

    /**
     * Header key value 分隔符。
     */
    public static final String HEADER_VALUE_SEPARATOR = "=";

    /**
     * hash key 分隔符。
     */
    public static final String HASH_KEY_SEPARATOR = "|";

    /**
     * 单页查询上限。
     */
    public static final int PAGE_LIMIT = 100;

    /**
     * 诊断最多扫描配置数。
     */
    public static final int MAX_SCAN_ITEMS = 5000;

    /**
     * 颜色名安全格式。
     */
    public static final Pattern COLOR_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}$");

    /**
     * 未找到发布配置。
     */
    public static final String MESSAGE_PUBLISHED_CONFIG_NOT_FOUND = "未找到已发布配置";

    /**
     * 未命中路由提示。
     */
    public static final String WARNING_ROUTE_NOT_MATCHED = "没有命中任何已发布路由";

    /**
     * 方法不允许提示。
     */
    public static final String WARNING_METHOD_NOT_ALLOWED = "HTTP 方法不在路由白名单内";

    /**
     * 认证提示。
     */
    public static final String WARNING_AUTH_REQUIRED = "命中路由需要认证";

    /**
     * 上游缺失提示。
     */
    public static final String WARNING_UPSTREAM_MISSING = "命中路由的上游不存在";

    /**
     * 上游端点缺失提示。
     */
    public static final String WARNING_UPSTREAM_ENDPOINT_EMPTY = "命中路由的上游没有可用端点";

    private GatePilotDiagnosticsConstants() {
        // 诊断常量不允许实例化
    }
}
