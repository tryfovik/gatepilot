package com.dt.platform.gateway.admin;

import com.dt.platform.gateway.infrastructure.audit.GatewayAccessAuditController;
import com.dt.platform.gateway.infrastructure.audit.GatewayAccessAuditRepository;
import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.diagnostics.GatewayDiagnosticsConfiguration;
import com.dt.platform.gateway.infrastructure.diagnostics.GatewayDiagnosticsController;
import com.dt.platform.gateway.infrastructure.management.GatewayManagementConfiguration;
import com.dt.platform.gateway.infrastructure.management.GatewayManagementController;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 平台网关管理面后端应用。
 */
@SpringBootApplication(excludeName = {
        "org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration",
        "org.springframework.cloud.gateway.config.GatewayAutoConfiguration",
        "org.springframework.cloud.gateway.config.GatewayResilience4JCircuitBreakerAutoConfiguration",
        "org.springframework.cloud.gateway.config.GatewayNoLoadBalancerClientAutoConfiguration",
        "org.springframework.cloud.gateway.config.GatewayFunctionAutoConfiguration",
        "org.springframework.cloud.gateway.config.GatewayMetricsAutoConfiguration",
        "org.springframework.cloud.gateway.config.GatewayRedisAutoConfiguration",
        "org.springframework.cloud.gateway.config.GatewayStreamAutoConfiguration",
        "org.springframework.cloud.gateway.discovery.GatewayDiscoveryClientAutoConfiguration",
        "org.springframework.cloud.gateway.config.SimpleUrlHandlerMappingGlobalCorsAutoConfiguration",
        "org.springframework.cloud.gateway.config.GatewayReactiveLoadBalancerClientAutoConfiguration",
        "org.springframework.cloud.gateway.config.GatewayReactiveOAuth2AutoConfiguration",
        "org.springframework.cloud.gateway.config.LocalResponseCacheAutoConfiguration"
})
@EnableConfigurationProperties(GatewayProperties.class)
@Import({
        GatewayAccessAuditController.class,
        GatewayDiagnosticsConfiguration.class,
        GatewayDiagnosticsController.class,
        GatewayManagementConfiguration.class,
        GatewayManagementController.class
})
public class GatewayAdminApplication {

    /**
     * 启动管理面后端应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayAdminApplication.class, args);
    }

    /**
     * 注册管理面只读路由定义定位器。
     *
     * @param properties 网关配置
     * @return 路由定义定位器
     */
    @Bean
    public GatewayRouteDefinitionLocator gatewayRouteDefinitionLocator(GatewayProperties properties) {
        return new GatewayRouteDefinitionLocator(properties);
    }

    /**
     * 注册管理面访问审计查询仓库。
     *
     * <p>独立管理面进程下，这里只提供 API 形态；生产审计查询应接持久化日志或事件存储。</p>
     *
     * @param properties 网关配置
     * @return 审计仓库
     */
    @Bean
    public GatewayAccessAuditRepository gatewayAccessAuditRepository(GatewayProperties properties) {
        return new GatewayAccessAuditRepository(properties.getAudit());
    }
}
