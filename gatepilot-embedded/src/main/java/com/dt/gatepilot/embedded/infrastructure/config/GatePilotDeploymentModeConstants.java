package com.dt.gatepilot.embedded.infrastructure.config;

/**
 * GatePilot 部署模式配置常量。
 */
public final class GatePilotDeploymentModeConstants {

    /**
     * GatePilot 配置前缀。
     */
    public static final String CONFIG_PREFIX = "gatepilot";

    /**
     * 部署模式配置项。
     */
    public static final String MODE_PROPERTY = "mode";

    /**
     * 单体合包模式。
     */
    public static final String MODE_STANDALONE = "standalone";

    /**
     * 集群分服务模式。
     */
    public static final String MODE_CLUSTER = "cluster";

    private GatePilotDeploymentModeConstants() {
        // 部署模式配置常量不允许实例化
    }
}
