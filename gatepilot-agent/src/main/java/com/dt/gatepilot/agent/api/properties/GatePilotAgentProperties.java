package com.dt.gatepilot.agent.api.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GatePilot agent 配置。
 */
@Data
@ConfigurationProperties(prefix = "gatepilot.agent")
public class GatePilotAgentProperties {

    /**
     * 是否启用 agent 客户端能力。
     */
    private boolean enabled = true;

    /**
     * apiserver 基础地址。
     */
    private String apiserverBaseUrl = "http://127.0.0.1:18080";
}
