package com.dt.gatepilot.proxy.domain.runtime;

import java.util.List;

/**
 * 流量染色运行常量。
 */
public final class TrafficColorConstants {

    /**
     * 默认流量颜色。
     */
    public static final String DEFAULT_COLOR = "stable";

    /**
     * 默认染色 Header。
     */
    public static final String DEFAULT_HEADER_NAME = "X-Traffic-Color";

    /**
     * 默认权重 hash Header。
     */
    public static final List<String> DEFAULT_WEIGHT_HASH_HEADERS =
            List.of("X-User-Id", "X-Tenant-Id", "X-Trace-Id");

    /**
     * 权重桶总数。
     */
    public static final int WEIGHT_BUCKET_SIZE = 100;

    /**
     * 查询串分隔符。
     */
    public static final String QUERY_SEPARATOR = "?";

    /**
     * hash key 分隔符。
     */
    public static final String HASH_KEY_SEPARATOR = "|";

    /**
     * Header key value 分隔符。
     */
    public static final String HEADER_VALUE_SEPARATOR = "=";

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
     * 远程地址字段。
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

    private TrafficColorConstants() {
        // 流量染色常量不允许实例化
    }
}
