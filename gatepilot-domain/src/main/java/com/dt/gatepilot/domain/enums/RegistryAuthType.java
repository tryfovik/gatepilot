package com.dt.gatepilot.domain.enums;

/**
 * 注册中心认证类型。
 */
public enum RegistryAuthType {

    /**
     * 不启用认证。
     */
    NONE,

    /**
     * 用户名密码认证。
     */
    USERNAME_PASSWORD,

    /**
     * AccessKey / SecretKey 认证。
     */
    AKSK
}
