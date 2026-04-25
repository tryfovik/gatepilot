package com.dt.gatepilot.apiserver.infrastructure.persistence.mybatisplus;

/**
 * MyBatis-Plus 资源存储常量。
 */
public final class MybatisPlusResourceStoreConstants {

    /**
     * 分页最大条数。
     */
    public static final int MAX_LIMIT = 500;

    /**
     * 资源表名。
     */
    public static final String TABLE_NAME = "gatepilot_resource";

    /**
     * 初始化脚本路径。
     */
    public static final String SCHEMA_LOCATION = "db/gatepilot/schema-mysql.sql";

    /**
     * 首次写入 generation。
     */
    public static final long FIRST_GENERATION = 1L;

    /**
     * 每次成功写入的 generation 增量。
     */
    public static final long GENERATION_STEP = 1L;

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

    private MybatisPlusResourceStoreConstants() {
        // MyBatis-Plus 资源存储常量不允许实例化
    }
}
