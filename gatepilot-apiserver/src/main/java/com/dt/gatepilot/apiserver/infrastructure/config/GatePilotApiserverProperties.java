package com.dt.gatepilot.apiserver.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GatePilot apiserver 配置。
 */
@Data
@ConfigurationProperties(prefix = GatePilotApiserverConstants.CONFIG_PREFIX)
public class GatePilotApiserverProperties {

    /**
     * 资源存储配置。
     */
    private Store store = new Store();

    /**
     * 资源存储配置。
     */
    @Data
    public static class Store {

        /**
         * 存储类型，开发测试使用 memory，生产使用 database。
         */
        private String type = GatePilotApiserverConstants.STORE_TYPE_MEMORY;

        /**
         * 数据库存储配置。
         */
        private Database database = new Database();
    }

    /**
     * 数据库资源存储配置。
     */
    @Data
    public static class Database {

        /**
         * 是否启动时初始化资源表结构。
         */
        private boolean initializeSchema = false;
    }
}
