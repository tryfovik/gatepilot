package com.dt.gatepilot.api.enums;

/**
 * 已发布配置在节点上的应用状态。
 */
public enum ConfigApplyState {

    /**
     * 等待应用。
     */
    PENDING,

    /**
     * 已写入暂存区。
     */
    STAGED,

    /**
     * 已应用到 proxy。
     */
    APPLIED,

    /**
     * 应用失败。
     */
    FAILED,

    /**
     * 已回滚到 last-good。
     */
    ROLLED_BACK
}
