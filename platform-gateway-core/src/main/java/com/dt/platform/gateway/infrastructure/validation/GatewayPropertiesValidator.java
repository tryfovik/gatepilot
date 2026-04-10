package com.dt.platform.gateway.infrastructure.validation;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.util.StringUtils;

/**
 * 网关配置启动校验器。
 */
public class GatewayPropertiesValidator {

    /**
     * 创建网关配置校验器。
     *
     * @param properties 网关配置
     */
    public GatewayPropertiesValidator(GatewayProperties properties) {
        validatePrefixes(properties);
        validateCors(properties);
        validateContextHeaders(properties);
    }

    private void validatePrefixes(GatewayProperties properties) {
        String apiPrefix = normalizePrefix(properties.getApiPrefix());
        String internalPrefix = normalizePrefix(properties.getInternalPrefix());
        if (StringUtils.hasText(apiPrefix) && apiPrefix.equals(internalPrefix)) {
            throw new IllegalArgumentException("gateway api prefix and internal prefix must not be identical");
        }
    }

    private void validateCors(GatewayProperties properties) {
        GatewayProperties.CorsProperties cors = properties.getCors();
        if (!cors.isEnabled()) {
            return;
        }
        if (isBlankCollection(cors.getAllowedOriginPatterns())) {
            throw new IllegalArgumentException("gateway cors allowed origin patterns must not be empty");
        }
        if (isBlankCollection(cors.getAllowedMethods())) {
            throw new IllegalArgumentException("gateway cors allowed methods must not be empty");
        }
        if (isBlankCollection(cors.getAllowedHeaders())) {
            throw new IllegalArgumentException("gateway cors allowed headers must not be empty");
        }
    }

    private void validateContextHeaders(GatewayProperties properties) {
        GatewayProperties.ContextHeadersProperties contextHeaders = properties.getContextHeaders();
        if (!contextHeaders.isEnabled()) {
            return;
        }
        requireText(contextHeaders.getProjectHeaderName(), "gateway project header name must not be blank");
        requireText(contextHeaders.getRouteHeaderName(), "gateway route header name must not be blank");
        if (contextHeaders.getProjectHeaderName().equalsIgnoreCase(contextHeaders.getRouteHeaderName())) {
            throw new IllegalArgumentException("gateway project header name and route header name must be different");
        }
    }

    private boolean isBlankCollection(Iterable<String> values) {
        if (values == null) {
            return true;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return false;
            }
        }
        return true;
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
