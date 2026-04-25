package com.dt.platform.gateway.infrastructure.audit;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关访问审计配置。
 */
@Configuration
public class GatewayAccessAuditConfiguration {

    /**
     * 注册最近审计事件仓库。
     *
     * @param properties 网关配置
     * @return 审计事件仓库
     */
    @Bean
    public GatewayAccessAuditRepository gatewayAccessAuditRepository(GatewayProperties properties) {
        return new GatewayAccessAuditRepository(properties.getAudit());
    }
}
