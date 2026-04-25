package com.dt.gatepilot.apiserver.infrastructure.persistence.mybatisplus;

import com.dt.gatepilot.apiserver.infrastructure.config.GatePilotApiserverConstants;
import com.dt.gatepilot.apiserver.infrastructure.config.GatePilotApiserverProperties;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * GatePilot 资源表初始化器。
 */
@Component
@ConditionalOnProperty(prefix = GatePilotApiserverConstants.STORE_CONFIG_PREFIX,
        name = GatePilotApiserverConstants.STORE_TYPE_PROPERTY,
        havingValue = GatePilotApiserverConstants.STORE_TYPE_DATABASE)
public class GatePilotResourceSchemaInitializer {

    private final DataSource dataSource;

    private final GatePilotApiserverProperties properties;

    /**
     * 创建资源表初始化器。
     *
     * @param dataSource 数据源
     * @param properties apiserver 配置
     */
    public GatePilotResourceSchemaInitializer(DataSource dataSource, GatePilotApiserverProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
    }

    /**
     * 按配置初始化资源表。
     */
    @PostConstruct
    public void initializeSchema() {
        if (!properties.getStore().getDatabase().isInitializeSchema()) {
            return;
        }
        // 建表 SQL 只从 schema 文件读取，Java 代码不拼 SQL
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource(MybatisPlusResourceStoreConstants.SCHEMA_LOCATION));
        populator.execute(dataSource);
    }
}
