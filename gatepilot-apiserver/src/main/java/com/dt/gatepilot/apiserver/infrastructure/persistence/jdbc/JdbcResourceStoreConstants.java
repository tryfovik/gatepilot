package com.dt.gatepilot.apiserver.infrastructure.persistence.jdbc;

/**
 * JDBC 资源存储常量。
 */
public final class JdbcResourceStoreConstants {

    /**
     * 分页最大条数。
     */
    public static final int MAX_LIMIT = 500;

    /**
     * 首次写入 generation。
     */
    public static final long FIRST_GENERATION = 1L;

    /**
     * 每次成功写入的 generation 增量。
     */
    public static final long GENERATION_STEP = 1L;

    /**
     * 表名白名单正则。
     */
    public static final String TABLE_NAME_PATTERN = "[a-zA-Z0-9_]+";

    /**
     * 资源写入冲突提示。
     */
    public static final String MESSAGE_RESOURCE_WRITE_CONFLICT = "资源版本已变化，请刷新后重试";

    /**
     * 资源 JSON 反序列化失败提示。
     */
    public static final String MESSAGE_JSON_DESERIALIZATION_FAILED = "resource json deserialization failed";

    /**
     * 资源 JSON 序列化失败提示。
     */
    public static final String MESSAGE_JSON_SERIALIZATION_FAILED = "resource json serialization failed";

    /**
     * 无效资源表名提示。
     */
    public static final String MESSAGE_INVALID_TABLE_NAME = "invalid gatepilot resource table name";

    private JdbcResourceStoreConstants() {
        // JDBC 资源存储常量不允许实例化
    }
}
