package com.dt.gatepilot.proxy.infrastructure.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * GatePilot proxy 启用条件
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(prefix = GatePilotProxyConstants.CONFIG_PREFIX,
        name = GatePilotProxyConstants.ENABLED_PROPERTY,
        havingValue = "true",
        matchIfMissing = true)
public @interface ConditionalOnGatePilotProxyEnabled {
}
