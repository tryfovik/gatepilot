package com.dt.gatepilot.api.enums;

/**
 * 网关认证类型。
 */
public enum AuthType {

    /**
     * 不启用认证。
     */
    NONE,

    /**
     * API Key 认证。
     */
    API_KEY,

    /**
     * JWT 认证。
     */
    JWT,

    /**
     * OAuth2 认证。
     */
    OAUTH2,

    /**
     * Basic 认证。
     */
    BASIC,

    /**
     * 双向 TLS 认证。
     */
    MTLS
}
