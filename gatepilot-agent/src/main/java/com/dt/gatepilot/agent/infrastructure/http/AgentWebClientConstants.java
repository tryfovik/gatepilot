package com.dt.gatepilot.agent.infrastructure.http;

/**
 * agent WebClient 常量
 */
public final class AgentWebClientConstants {

    /**
     * Spring Cloud LoadBalancer 包名前缀
     */
    public static final String SPRING_CLOUD_LOADBALANCER_PACKAGE =
            "org.springframework.cloud.client.loadbalancer";

    /**
     * LoadBalancer filter 类名片段
     */
    public static final String LOAD_BALANCER_FILTER_CLASS_FRAGMENT = "LoadBalancer";

    /**
     * 禁止实例化
     */
    private AgentWebClientConstants() {
        // agent WebClient 常量不允许实例化
    }
}
