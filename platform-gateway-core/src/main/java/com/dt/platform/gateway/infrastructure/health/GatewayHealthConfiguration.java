package com.dt.platform.gateway.infrastructure.health;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 网关健康检查配置。
 */
@Configuration
public class GatewayHealthConfiguration {

    /**
     * 注册上游应用健康检查器。
     *
     * @param webClientBuilder WebClient 构建器
     * @param properties 网关配置
     * @param routeDefinitionLocator 路由定义定位器
     * @return 上游健康检查器
     */
    @Bean("gatewayUpstreams")
    public ReactiveHealthIndicator gatewayUpstreamsHealthIndicator(WebClient.Builder webClientBuilder,
                                                                   GatewayProperties properties,
                                                                   GatewayRouteDefinitionLocator routeDefinitionLocator) {
        return new UpstreamHealthIndicator(webClientBuilder.build(), properties, routeDefinitionLocator);
    }
}
