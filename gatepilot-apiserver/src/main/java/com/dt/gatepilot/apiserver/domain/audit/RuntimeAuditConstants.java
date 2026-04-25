package com.dt.gatepilot.apiserver.domain.audit;

/**
 * 运行审计常量。
 */
public final class RuntimeAuditConstants {

    /**
     * 默认分页条数。
     */
    public static final int DEFAULT_LIMIT = 50;

    /**
     * 默认分页条数字符串。
     */
    public static final String DEFAULT_LIMIT_TEXT = "50";

    /**
     * 最大分页条数。
     */
    public static final int MAX_LIMIT = 500;

    /**
     * 空游标。
     */
    public static final long EMPTY_CURSOR = 0L;

    /**
     * 审计上报成功提示。
     */
    public static final String MESSAGE_AUDIT_ACCEPTED = "运行审计已接收";

    private RuntimeAuditConstants() {
        // 运行审计常量不允许实例化
    }
}
