package com.dt.gatepilot.apiserver.infrastructure.config;

/**
 * apiserver 配置常量。
 */
public final class GatePilotApiserverConstants {

    /**
     * apiserver 配置前缀。
     */
    public static final String CONFIG_PREFIX = "gatepilot.apiserver";

    /**
     * 资源存储配置前缀。
     */
    public static final String STORE_CONFIG_PREFIX = CONFIG_PREFIX + ".store";

    /**
     * 存储类型配置名。
     */
    public static final String STORE_TYPE_PROPERTY = "type";

    /**
     * 内存存储类型。
     */
    public static final String STORE_TYPE_MEMORY = "memory";

    /**
     * JDBC 存储类型。
     */
    public static final String STORE_TYPE_JDBC = "jdbc";

    /**
     * 默认资源表名。
     */
    public static final String DEFAULT_RESOURCE_TABLE = "gatepilot_resource";

    private GatePilotApiserverConstants() {
        // apiserver 配置常量不允许实例化
    }
}
