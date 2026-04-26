package com.dt.gatepilot.apiserver.application.service;

/**
 * agent 协议服务常量
 */
public final class GatePilotAgentConstants {

    /**
     * PublishedConfig 拉取分页大小
     */
    public static final int PUBLISHED_CONFIG_PAGE_LIMIT = 500;

    /**
     * 没有可拉取配置提示
     */
    public static final String MESSAGE_NO_PUBLISHED_CONFIG = "暂无可拉取的 PublishedConfig";

    /**
     * 发现新配置提示
     */
    public static final String MESSAGE_CONFIG_CHANGED = "发现新配置";

    /**
     * 当前配置已是最新提示
     */
    public static final String MESSAGE_CONFIG_NOT_CHANGED = "当前配置已是最新";

    private GatePilotAgentConstants() {
        // agent 协议服务常量不允许实例化
    }
}
