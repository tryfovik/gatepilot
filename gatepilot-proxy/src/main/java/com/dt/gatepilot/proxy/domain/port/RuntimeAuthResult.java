package com.dt.gatepilot.proxy.domain.port;

/**
 * proxy 运行时认证结果。
 */
public record RuntimeAuthResult(boolean allowed,
                                int status,
                                int code,
                                String message,
                                String reason,
                                Throwable error) {

    /**
     * 创建认证通过结果。
     *
     * @return 认证结果
     */
    public static RuntimeAuthResult pass() {
        return new RuntimeAuthResult(true, 0, 0, null, null, null);
    }

    /**
     * 创建认证拒绝结果。
     *
     * @param status HTTP 状态码
     * @param code 业务码
     * @param message 提示
     * @param reason 原因码
     * @param error 异常
     * @return 认证结果
     */
    public static RuntimeAuthResult denied(int status,
                                           int code,
                                           String message,
                                           String reason,
                                           Throwable error) {
        return new RuntimeAuthResult(false, status, code, message, reason, error);
    }
}
