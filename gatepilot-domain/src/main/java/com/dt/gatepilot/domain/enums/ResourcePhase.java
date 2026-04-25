package com.dt.gatepilot.domain.enums;

/**
 * 声明式资源的通用生命周期状态。
 */
public enum ResourcePhase {

    /**
     * 等待控制面处理。
     */
    PENDING,

    /**
     * 当前资源已经生效。
     */
    ACTIVE,

    /**
     * 当前资源部分可用或存在告警。
     */
    DEGRADED,

    /**
     * 当前资源处理失败。
     */
    FAILED,

    /**
     * 当前资源已经标记删除。
     */
    DELETED
}
