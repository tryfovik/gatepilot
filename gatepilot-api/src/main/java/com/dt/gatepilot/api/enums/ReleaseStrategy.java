package com.dt.gatepilot.api.enums;

/**
 * 发布策略类型。
 */
public enum ReleaseStrategy {

    /**
     * 蓝绿发布。
     */
    BLUE_GREEN,

    /**
     * 灰度发布。
     */
    CANARY,

    /**
     * 固定权重分流。
     */
    TRAFFIC_SPLIT,

    /**
     * 影子流量发布。
     */
    SHADOW
}
