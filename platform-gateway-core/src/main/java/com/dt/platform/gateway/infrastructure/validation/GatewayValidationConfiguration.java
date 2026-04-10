package com.dt.platform.gateway.infrastructure.validation;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关配置校验配置。
 */
@Configuration
public class GatewayValidationConfiguration {

    /**
     * 注册网关配置校验器。
     *
     * @param properties 网关配置
     * @return 配置校验器
     */
    @Bean
    public GatewayPropertiesValidator gatewayPropertiesValidator(GatewayProperties properties) {
        return new GatewayPropertiesValidator(properties);
    }
}
