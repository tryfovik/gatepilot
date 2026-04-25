package com.dt.gatepilot.api.enums;

/**
 * 网关事件严重级别。
 */
public enum EventSeverity {

    /**
     * 普通信息。
     */
    INFO,

    /**
     * 需要关注的告警。
     */
    WARNING,

    /**
     * 失败或错误事件。
     */
    ERROR
}
