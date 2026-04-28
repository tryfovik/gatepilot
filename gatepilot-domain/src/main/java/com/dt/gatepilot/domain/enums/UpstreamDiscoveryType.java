package com.dt.gatepilot.domain.enums;

/**
 * 上游实例发现方式。
 */
public enum UpstreamDiscoveryType {

    /**
     * 固定地址。
     */
    STATIC,

    /**
     * Nacos 服务发现。
     */
    NACOS
}
