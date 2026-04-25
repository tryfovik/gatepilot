package com.dt.platform.gateway.infrastructure.management;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteCatalogEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关管理面配置。
 */
@Configuration
public class GatewayManagementConfiguration {

    /**
     * 注册配置快照仓库。
     *
     * @return 配置快照仓库
     */
    @Bean
    public GatewayConfigSnapshotRepository gatewayConfigSnapshotRepository() {
        return new GatewayConfigSnapshotRepository();
    }

    /**
     * 注册管理端配置存储。
     *
     * @param properties 初始网关配置
     * @return 配置存储
     */
    @Bean
    public GatewayConfigStore gatewayConfigStore(GatewayProperties properties) {
        return new GatewayConfigStore(properties);
    }

    /**
     * 注册管理端路由目录端点。
     *
     * @param configStore 配置存储
     * @return 路由目录端点
     */
    @Bean
    public GatewayRouteCatalogEndpoint gatewayRouteCatalogEndpoint(GatewayConfigStore configStore) {
        return new GatewayRouteCatalogEndpoint(configStore);
    }

    /**
     * 注册配置治理服务。
     *
     * @param configStore 管理端配置存储
     * @param configSnapshotRepository 配置快照仓库
     * @return 配置治理服务
     */
    @Bean
    public GatewayManagementService gatewayManagementService(GatewayConfigStore configStore,
                                                             GatewayConfigSnapshotRepository configSnapshotRepository) {
        return new GatewayManagementService(configStore, configSnapshotRepository);
    }
}
