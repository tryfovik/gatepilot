package com.dt.gatepilot.controller.infrastructure.config;

/**
 * controller-manager 配置常量。
 */
public final class ControllerManagerConstants {

    /**
     * controller-manager 配置前缀。
     */
    public static final String CONFIG_PREFIX = "gatepilot.controller-manager";

    /**
     * 默认 controller 标识。
     */
    public static final String DEFAULT_CONTROLLER_ID = "local-controller";

    private ControllerManagerConstants() {
        // controller-manager 配置常量不允许实例化
    }
}
