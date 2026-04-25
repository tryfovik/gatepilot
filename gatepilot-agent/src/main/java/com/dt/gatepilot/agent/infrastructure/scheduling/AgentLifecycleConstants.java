package com.dt.gatepilot.agent.infrastructure.scheduling;

/**
 * agent 生命周期调度常量。
 */
public final class AgentLifecycleConstants {

    /**
     * 注册节点动作。
     */
    public static final String ACTION_REGISTER = "register";

    /**
     * last-good 启动动作。
     */
    public static final String ACTION_START_LAST_GOOD = "start-last-good";

    /**
     * 拉取并应用配置动作。
     */
    public static final String ACTION_PULL_AND_APPLY = "pull-and-apply";

    /**
     * 心跳动作。
     */
    public static final String ACTION_HEARTBEAT = "heartbeat";

    /**
     * 调度动作失败日志。
     */
    public static final String LOG_ACTION_FAILED = "GatePilot agent action failed, action={}, message={}";

    private AgentLifecycleConstants() {
        // agent 生命周期常量不允许实例化
    }
}
