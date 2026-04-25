package com.dt.gatepilot.agent.infrastructure.config;

/**
 * agent 运行配置常量。
 */
public final class AgentRuntimeConstants {

    /**
     * agent 配置前缀。
     */
    public static final String CONFIG_PREFIX = "gatepilot.agent";

    /**
     * agent 启用开关配置名。
     */
    public static final String ENABLED_PROPERTY = "enabled";

    /**
     * 默认 apiserver 地址。
     */
    public static final String DEFAULT_APISERVER_BASE_URL = "http://127.0.0.1:18080";

    /**
     * 本地默认节点标识。
     */
    public static final String DEFAULT_NODE_ID = "local-node";

    private AgentRuntimeConstants() {
        // agent 配置常量不允许实例化
    }
}
