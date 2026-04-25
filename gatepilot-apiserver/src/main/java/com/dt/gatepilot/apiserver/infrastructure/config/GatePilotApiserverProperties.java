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
         * 存储类型，开发测试使用 memory，生产使用 jdbc。
         */
        private String type = GatePilotApiserverConstants.STORE_TYPE_MEMORY;

        /**
         * JDBC 存储配置。
         */
        private Jdbc jdbc = new Jdbc();
    }

    /**
     * JDBC 资源存储配置。
     */
    @Data
    public static class Jdbc {

        /**
         * 资源表名。
         */
        private String tableName = GatePilotApiserverConstants.DEFAULT_RESOURCE_TABLE;

        /**
         * 是否启动时初始化资源表结构。
         */
        private boolean initializeSchema = false;
    }
}
