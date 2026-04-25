package com.dt.gatepilot.agent.infrastructure.config;

import com.dt.gatepilot.agent.application.service.AgentRuntimeAuditConstants;

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
     * 配置拉取周期配置名。
     */
    public static final String PULL_INTERVAL_PROPERTY = "pull-interval-ms";

    /**
     * 配置拉取初始延迟配置名。
     */
    public static final String PULL_INITIAL_DELAY_PROPERTY = "pull-initial-delay-ms";

    /**
     * 心跳周期配置名。
     */
    public static final String HEARTBEAT_INTERVAL_PROPERTY = "heartbeat-interval-ms";

    /**
     * 心跳初始延迟配置名。
     */
    public static final String HEARTBEAT_INITIAL_DELAY_PROPERTY = "heartbeat-initial-delay-ms";

    /**
     * 审计配置前缀。
     */
    public static final String AUDIT_CONFIG_PREFIX = CONFIG_PREFIX + ".audit";

    /**
     * 审计上报周期配置名。
     */
    public static final String AUDIT_FLUSH_INTERVAL_PROPERTY = "flush-interval-ms";

    /**
     * 审计上报初始延迟配置名。
     */
    public static final String AUDIT_FLUSH_INITIAL_DELAY_PROPERTY = "flush-initial-delay-ms";

    /**
     * 文件本地配置存储类型。
     */
    public static final String LOCAL_CONFIG_STORE_TYPE_FILE = "file";

    /**
     * 内存本地配置存储类型。
     */
    public static final String LOCAL_CONFIG_STORE_TYPE_MEMORY = "memory";

    /**
     * 默认本地配置目录。
     */
    public static final String DEFAULT_LOCAL_CONFIG_DIRECTORY = "./data/gatepilot/agent/local-config";

    /**
     * 不支持的本地配置存储类型提示。
     */
    public static final String MESSAGE_UNSUPPORTED_LOCAL_CONFIG_STORE_TYPE = "不支持的 agent 本地配置存储类型";

    /**
     * 默认 apiserver 地址。
     */
    public static final String DEFAULT_APISERVER_BASE_URL = "http://127.0.0.1:18080";

    /**
     * 本地默认节点标识。
     */
    public static final String DEFAULT_NODE_ID = "local-node";

    /**
     * 默认配置拉取周期。
     */
    public static final long DEFAULT_PULL_INTERVAL_MS = 5_000L;

    /**
     * 默认配置拉取初始延迟。
     */
    public static final long DEFAULT_PULL_INITIAL_DELAY_MS = 1_000L;

    /**
     * 默认心跳周期。
     */
    public static final long DEFAULT_HEARTBEAT_INTERVAL_MS = 10_000L;

    /**
     * 默认心跳初始延迟。
     */
    public static final long DEFAULT_HEARTBEAT_INITIAL_DELAY_MS = 2_000L;

    /**
     * 配置拉取调度周期占位符。
     */
    public static final String PULL_INTERVAL_PLACEHOLDER =
            "${" + CONFIG_PREFIX + "." + PULL_INTERVAL_PROPERTY + ":" + DEFAULT_PULL_INTERVAL_MS + "}";

    /**
     * 配置拉取调度初始延迟占位符。
     */
    public static final String PULL_INITIAL_DELAY_PLACEHOLDER =
            "${" + CONFIG_PREFIX + "." + PULL_INITIAL_DELAY_PROPERTY + ":"
                    + DEFAULT_PULL_INITIAL_DELAY_MS + "}";

    /**
     * 心跳调度周期占位符。
     */
    public static final String HEARTBEAT_INTERVAL_PLACEHOLDER =
            "${" + CONFIG_PREFIX + "." + HEARTBEAT_INTERVAL_PROPERTY + ":"
                    + DEFAULT_HEARTBEAT_INTERVAL_MS + "}";

    /**
     * 心跳调度初始延迟占位符。
     */
    public static final String HEARTBEAT_INITIAL_DELAY_PLACEHOLDER =
            "${" + CONFIG_PREFIX + "." + HEARTBEAT_INITIAL_DELAY_PROPERTY + ":"
                    + DEFAULT_HEARTBEAT_INITIAL_DELAY_MS + "}";

    /**
     * 审计上报调度周期占位符。
     */
    public static final String AUDIT_FLUSH_INTERVAL_PLACEHOLDER =
            "${" + AUDIT_CONFIG_PREFIX + "." + AUDIT_FLUSH_INTERVAL_PROPERTY + ":"
                    + AgentRuntimeAuditConstants.DEFAULT_FLUSH_INTERVAL_MS + "}";

    /**
     * 审计上报调度初始延迟占位符。
     */
    public static final String AUDIT_FLUSH_INITIAL_DELAY_PLACEHOLDER =
            "${" + AUDIT_CONFIG_PREFIX + "." + AUDIT_FLUSH_INITIAL_DELAY_PROPERTY + ":"
                    + AgentRuntimeAuditConstants.DEFAULT_FLUSH_INITIAL_DELAY_MS + "}";

    private AgentRuntimeConstants() {
        // agent 配置常量不允许实例化
    }
}
