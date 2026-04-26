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
     * controller-manager 启用开关配置名
     */
    public static final String ENABLED_PROPERTY = "enabled";

    /**
     * 默认 controller 标识。
     */
    public static final String DEFAULT_CONTROLLER_ID = "local-controller";

    /**
     * 发布 reconcile 分布式锁场景。
     */
    public static final String RECONCILE_LOCK_SCENE = "gatepilot-controller-manager";

    /**
     * 发布 reconcile 分布式锁键。
     */
    public static final String RECONCILE_LOCK_KEY = "release-reconcile";

    /**
     * PublishedConfig 状态刷新分布式锁键
     */
    public static final String STATUS_REFRESH_LOCK_KEY = "published-config-status-refresh";

    /**
     * 发布 reconcile 锁等待时间。
     */
    public static final int RECONCILE_LOCK_WAIT_TIME_MS = 0;

    /**
     * 发布 reconcile 调度间隔占位符
     */
    public static final String RECONCILE_INTERVAL_PLACEHOLDER =
            "${gatepilot.controller-manager.reconcile-interval-ms:5000}";

    /**
     * PublishedConfig 状态刷新调度间隔占位符
     */
    public static final String STATUS_REFRESH_INTERVAL_PLACEHOLDER =
            "${gatepilot.controller-manager.status-refresh-interval-ms:5000}";

    /**
     * 发布 reconcile 锁占用提示。
     */
    public static final String MESSAGE_RECONCILE_LOCK_BUSY = "其他 controller-manager 正在推进发布，跳过本轮";

    /**
     * PublishedConfig 状态刷新锁占用提示
     */
    public static final String MESSAGE_STATUS_REFRESH_LOCK_BUSY =
            "其他 controller-manager 正在刷新发布状态，跳过本轮";

    private ControllerManagerConstants() {
        // controller-manager 配置常量不允许实例化
    }
}
