package com.dt.gatepilot.controller.application.service;

/**
 * PublishedConfig 组装常量。
 */
public final class PublishedConfigAssemblerConstants {

    /**
     * 名称分隔符。
     */
    public static final String NAME_SEPARATOR = "-";

    /**
     * 资源名安全字符替换正则。
     */
    public static final String SAFE_NAME_REGEX = "[^a-zA-Z0-9._-]";

    /**
     * hash 字段分隔符。
     */
    public static final String HASH_FIELD_SEPARATOR = "|";

    /**
     * hash 列表分隔符。
     */
    public static final String HASH_LIST_SEPARATOR = ",";

    /**
     * 空 hash 片段。
     */
    public static final String EMPTY_HASH_PART = "";

    /**
     * SHA-256 摘要算法。
     */
    public static final String DIGEST_SHA_256 = "SHA-256";

    /**
     * 不支持负载均衡策略错误前缀。
     */
    public static final String ERROR_UNSUPPORTED_LOAD_BALANCE_PREFIX = "unsupported load balance strategy: ";

    /**
     * 错误详情分隔符。
     */
    public static final String ERROR_DETAIL_SEPARATOR = " -> ";

    private PublishedConfigAssemblerConstants() {
        // PublishedConfig 组装常量不允许实例化
    }
}
