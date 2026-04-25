package com.dt.gatepilot.agent.infrastructure.apiserver;

/**
 * agent 访问 apiserver 的协议路径常量。
 */
public final class AgentControlPlaneApiPaths {

    /**
     * agent API 前缀。
     */
    public static final String API_PREFIX = "/api/gatepilot/v1/agents";

    /**
     * 注册路径。
     */
    public static final String REGISTER = "/register";

    /**
     * 心跳路径。
     */
    public static final String HEARTBEAT = "/heartbeat";

    /**
     * 配置拉取路径。
     */
    public static final String CONFIG_PULL = "/configs/pull";

    /**
     * 应用结果上报路径。
     */
    public static final String APPLY_RESULTS = "/apply-results";

    private AgentControlPlaneApiPaths() {
        // agent 协议路径常量不允许实例化
    }
}
