package com.dt.gatepilot.controller.application.service;

/**
 * 发布事件工厂常量。
 */
public final class ReleaseEventFactoryConstants {

    /**
     * 发布事件名称前缀。
     */
    public static final String EVENT_NAME_PREFIX = "event-";

    /**
     * PublishedConfig 生成消息。
     */
    public static final String MESSAGE_PUBLISHED_CONFIG_GENERATED = "controller-manager 已生成 PublishedConfig";

    /**
     * 回滚 PublishedConfig 生成消息。
     */
    public static final String MESSAGE_ROLLBACK_CONFIG_GENERATED = "controller-manager 已生成回滚 PublishedConfig";

    private ReleaseEventFactoryConstants() {
        // 发布事件工厂常量不允许实例化
    }
}
