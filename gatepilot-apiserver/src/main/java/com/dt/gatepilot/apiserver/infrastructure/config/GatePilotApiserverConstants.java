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
     * apiserver 启用开关配置名。
     */
    public static final String ENABLED_PROPERTY = "enabled";

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
     * 数据库存储类型。
     */
    public static final String STORE_TYPE_DATABASE = "database";

    private GatePilotApiserverConstants() {
        // apiserver 配置常量不允许实例化
    }
}
