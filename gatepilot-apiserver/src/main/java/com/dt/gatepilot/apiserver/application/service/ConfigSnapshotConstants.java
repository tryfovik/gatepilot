package com.dt.gatepilot.apiserver.application.service;

/**
 * 配置快照常量。
 */
public final class ConfigSnapshotConstants {

    /**
     * 摘要列表默认页大小。
     */
    public static final int DEFAULT_SUMMARY_LIMIT = 50;

    /**
     * 摘要列表最大页大小。
     */
    public static final int MAX_SUMMARY_LIMIT = 500;

    /**
     * 快照扫描页大小。
     */
    public static final int SNAPSHOT_SCAN_LIMIT = 500;

    /**
     * 快照复制失败提示。
     */
    public static final String MESSAGE_SNAPSHOT_COPY_FAILED = "配置快照复制失败";

    private ConfigSnapshotConstants() {
        // 配置快照常量不允许实例化
    }
}
