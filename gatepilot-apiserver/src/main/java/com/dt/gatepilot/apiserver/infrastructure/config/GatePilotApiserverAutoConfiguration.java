package com.dt.gatepilot.apiserver.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GatePilot apiserver 自动配置。
 */
@Configuration
@ConditionalOnGatePilotApiserverEnabled
@EnableConfigurationProperties(GatePilotApiserverProperties.class)
public class GatePilotApiserverAutoConfiguration {
}
