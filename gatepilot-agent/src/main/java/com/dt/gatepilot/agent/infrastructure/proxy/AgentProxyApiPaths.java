package com.dt.gatepilot.agent.infrastructure.proxy;

/**
 * agent 访问 proxy runtime control 的协议路径常量
 */
public final class AgentProxyApiPaths {

    /**
     * proxy runtime control API 前缀
     */
    public static final String API_PREFIX = "/api/gatepilot/v1/proxy";

    /**
     * 配置应用路径
     */
    public static final String APPLY_CONFIG = "/configs/apply";

    private AgentProxyApiPaths() {
        // proxy 协议路径常量不允许实例化
    }
}
