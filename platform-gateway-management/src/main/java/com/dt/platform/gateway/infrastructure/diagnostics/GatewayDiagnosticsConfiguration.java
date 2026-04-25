package com.dt.platform.gateway.infrastructure.diagnostics;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关诊断能力配置。
 */
@Configuration
public class GatewayDiagnosticsConfiguration {

    /**
     * 注册路由诊断服务。
     *
     * @param properties 网关配置
     * @param routeDefinitionLocator 路由定义定位器
     * @return 路由诊断服务
     */
    @Bean
    public GatewayDiagnosticsService gatewayDiagnosticsService(GatewayProperties properties,
                                                               GatewayRouteDefinitionLocator routeDefinitionLocator) {
        return new GatewayDiagnosticsService(properties, routeDefinitionLocator);
    }
}
