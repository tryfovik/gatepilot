package com.dt.gatepilot.agent.infrastructure.config;

import com.dt.gatepilot.agent.infrastructure.config.GatePilotAgentProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GatePilot agent 自动配置。
 */
@Configuration
@EnableConfigurationProperties(GatePilotAgentProperties.class)
public class GatePilotAgentAutoConfiguration {
}
