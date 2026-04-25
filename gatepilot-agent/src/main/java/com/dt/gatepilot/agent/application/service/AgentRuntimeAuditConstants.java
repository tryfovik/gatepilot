package com.dt.gatepilot.agent.application.service;

/**
 * agent 运行审计常量。
 */
public final class AgentRuntimeAuditConstants {

    /**
     * 默认审计缓冲容量。
     */
    public static final int DEFAULT_BUFFER_CAPACITY = 10_000;

    /**
     * 最小审计缓冲容量。
     */
    public static final int MIN_BUFFER_CAPACITY = 1;

    /**
     * 默认批量上报条数。
     */
    public static final int DEFAULT_FLUSH_BATCH_SIZE = 200;

    /**
     * 最小批量上报条数。
     */
    public static final int MIN_FLUSH_BATCH_SIZE = 1;

    /**
     * 默认上报周期。
     */
    public static final long DEFAULT_FLUSH_INTERVAL_MS = 5_000L;

    /**
     * 默认上报初始延迟。
     */
    public static final long DEFAULT_FLUSH_INITIAL_DELAY_MS = 2_000L;

    /**
     * 审计 flush 动作。
     */
    public static final String ACTION_FLUSH_AUDITS = "flush-audits";

    private AgentRuntimeAuditConstants() {
        // agent 运行审计常量不允许实例化
    }
}
