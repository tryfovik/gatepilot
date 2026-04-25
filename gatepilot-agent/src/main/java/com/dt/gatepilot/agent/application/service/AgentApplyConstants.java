package com.dt.gatepilot.agent.application.service;

/**
 * agent 应用配置结果常量。
 */
public final class AgentApplyConstants {

    /**
     * proxy apply 异常原因码。
     */
    public static final String REASON_PROXY_APPLY_FAILED = "ProxyApplyFailed";

    /**
     * proxy 空结果原因码。
     */
    public static final String REASON_PROXY_APPLY_EMPTY_RESULT = "ProxyApplyEmptyResult";

    /**
     * 缺少 apply 状态原因码。
     */
    public static final String REASON_MISSING_APPLY_STATE = "MissingApplyState";

    /**
     * proxy 空结果说明。
     */
    public static final String MESSAGE_PROXY_APPLY_EMPTY_RESULT = "proxy apply 未返回结果";

    /**
     * 缺少 apply 状态说明。
     */
    public static final String MESSAGE_MISSING_APPLY_STATE = "proxy apply 结果缺少状态";

    private AgentApplyConstants() {
        // agent apply 常量不允许实例化
    }
}
