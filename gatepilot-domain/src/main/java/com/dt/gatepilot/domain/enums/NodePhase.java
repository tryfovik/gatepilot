package com.dt.gatepilot.domain.enums;

/**
 * 网关节点状态。
 */
public enum NodePhase {

    /**
     * 已注册但还未准备好。
     */
    REGISTERED,

    /**
     * 节点健康且可接流量。
     */
    READY,

    /**
     * 节点未就绪。
     */
    NOT_READY,

    /**
     * 节点正在摘流。
     */
    DRAINING,

    /**
     * 节点离线。
     */
    OFFLINE
}
