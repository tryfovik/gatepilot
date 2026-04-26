package com.dt.gatepilot.proxy.interfaces.control;

/**
 * proxy runtime control API 路径常量
 */
public final class ProxyRuntimeApiPaths {

    /**
     * proxy runtime control API 前缀
     */
    public static final String PROXY = "/api/gatepilot/v1/proxy";

    /**
     * 配置应用路径
     */
    public static final String APPLY_CONFIG = "/configs/apply";

    /**
     * 配置应用完成提示
     */
    public static final String MESSAGE_APPLY_CONFIG_FINISHED = "proxy 配置应用完成";

    private ProxyRuntimeApiPaths() {
        // proxy runtime control API 路径常量不允许实例化
    }
}
