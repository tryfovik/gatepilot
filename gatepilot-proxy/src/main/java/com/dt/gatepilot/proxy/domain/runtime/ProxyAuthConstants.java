package com.dt.gatepilot.proxy.domain.runtime;

/**
 * proxy 认证执行常量。
 */
public final class ProxyAuthConstants {

    /**
     * 未认证 HTTP 状态码。
     */
    public static final int UNAUTHORIZED_STATUS = 401;

    /**
     * 无权限 HTTP 状态码。
     */
    public static final int FORBIDDEN_STATUS = 403;

    /**
     * 认证组件不可用 HTTP 状态码。
     */
    public static final int UNAVAILABLE_STATUS = 503;

    /**
     * 未认证业务码。
     */
    public static final int UNAUTHORIZED_CODE = 401;

    /**
     * 无权限业务码。
     */
    public static final int FORBIDDEN_CODE = 403;

    /**
     * 认证组件不可用业务码。
     */
    public static final int UNAVAILABLE_CODE = 90003;

    /**
     * 未认证提示。
     */
    public static final String UNAUTHORIZED_MESSAGE = "认证失败";

    /**
     * 无权限提示。
     */
    public static final String FORBIDDEN_MESSAGE = "无权访问";

    /**
     * 认证组件不可用提示。
     */
    public static final String UNAVAILABLE_MESSAGE = "网关认证组件未就绪";

    /**
     * 未认证原因码。
     */
    public static final String REASON_UNAUTHORIZED = "auth_unauthorized";

    /**
     * 无权限原因码。
     */
    public static final String REASON_FORBIDDEN = "auth_forbidden";

    /**
     * 认证组件不可用原因码。
     */
    public static final String REASON_AUTH_CHECKER_UNAVAILABLE = "auth_checker_unavailable";

    /**
     * 隐藏工具类构造器。
     */
    private ProxyAuthConstants() {
        // proxy 认证常量不允许实例化
    }
}
