package com.dt.platform.gateway.infrastructure.security;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;

import java.util.List;

/**
 * 网关安全配置。
 */
@Configuration
public class GatewaySecurityConfiguration {

    /**
     * 注册内部运维入口保护过滤器。
     *
     * @param properties 网关配置
     * @return WebFlux 过滤器
     */
    @Bean
    public WebFilter internalRouteAccessFilter(GatewayProperties properties) {
        return new InternalRouteAccessFilter(properties);
    }

    /**
     * 注册网关统一跨域过滤器。
     *
     * @param properties 网关配置
     * @return 跨域过滤器
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnProperty(prefix = "platform.gateway.cors", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    public CorsWebFilter gatewayCorsWebFilter(GatewayProperties properties) {
        GatewayProperties.CorsProperties cors = properties.getCors();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(sanitizeValues(cors.getAllowedOriginPatterns()));
        configuration.setAllowedMethods(sanitizeValues(cors.getAllowedMethods()));
        configuration.setAllowedHeaders(sanitizeValues(cors.getAllowedHeaders()));
        configuration.setExposedHeaders(sanitizeValues(cors.getExposedHeaders()));
        configuration.setAllowCredentials(cors.isAllowCredentials());
        configuration.setMaxAge(cors.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsWebFilter(source);
    }

    /**
     * 清理列表中的空白配置项。
     *
     * @param values 原始列表
     * @return 清理后的列表
     */
    private List<String> sanitizeValues(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(org.springframework.util.StringUtils::hasText)
                .map(String::trim)
                .toList();
    }
}
