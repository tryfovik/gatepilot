package com.dt.gatepilot.domain.enums;

/**
 * 网关可处理的入口或上游协议。
 */
public enum Protocol {

    /**
     * HTTP 协议。
     */
    HTTP,

    /**
     * HTTPS 协议。
     */
    HTTPS,

    /**
     * WebSocket 协议。
     */
    WS,

    /**
     * TLS WebSocket 协议。
     */
    WSS,

    /**
     * TCP 协议。
     */
    TCP
}
