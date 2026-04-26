package com.dt.gatepilot.apiserver.application.dto;

import java.time.Instant;
import lombok.Data;

/**
 * 配置版本快照摘要响应。
 */
@Data
public class ConfigSnapshotSummaryResponse {

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 项目名称。
     */
    private String projectName;

    /**
     * 发布版本。
     */
    private String version;

    /**
     * 配置哈希。
     */
    private String configHash;

    /**
     * 配置分片。
     */
    private String configShard;

    /**
     * 配置序号。
     */
    private Long sequence;

    /**
     * 发布请求标识。
     */
    private String releaseId;

    /**
     * 路由数量。
     */
    private int routeCount;

    /**
     * 上游数量。
     */
    private int upstreamCount;

    /**
     * 策略数量。
     */
    private int policyCount;

    /**
     * 目标节点数量。
     */
    private int targetNodeCount;

    /**
     * 快照采集时间。
     */
    private Instant capturedAt;

    /**
     * 快照创建人。
     */
    private String capturedBy;

    /**
     * 快照说明。
     */
    private String description;

    /**
     * 是否允许回滚。
     */
    private Boolean rollbackAllowed;

    /**
     * 最近一次回滚时间。
     */
    private Instant lastRollbackAt;
}
