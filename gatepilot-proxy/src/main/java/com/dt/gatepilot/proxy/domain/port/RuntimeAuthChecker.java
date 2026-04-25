package com.dt.gatepilot.proxy.domain.port;

/**
 * proxy 运行时认证校验端口。
 */
public interface RuntimeAuthChecker {

    /**
     * 执行当前请求认证校验。
     *
     * @return 认证结果
     */
    RuntimeAuthResult check();
}
