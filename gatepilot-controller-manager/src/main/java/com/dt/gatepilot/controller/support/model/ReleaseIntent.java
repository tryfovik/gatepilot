package com.dt.gatepilot.controller.support.model;

import java.time.Instant;
import lombok.Data;

/**
 * 发布意图，controller-manager 从控制面资源中领取后推进为 PublishedConfig。
 */
@Data
public class ReleaseIntent {

    /**
     * 发布请求标识。
     */
    private String releaseId;

    /**
     * 发布意图所在命名空间。
     */
    private String namespace;

    /**
     * 项目名称。
     */
    private String projectName;

    /**
     * 目标发布版本。
     */
    private String version;

    /**
     * 配置分片键。
     */
    private String configShard;

    /**
     * 同一分片内单调递增序号。
     */
    private Long sequence;

    /**
     * 触发类型，例如 publish 或 rollback。
     */
    private String trigger;

    /**
     * 触发人。
     */
    private String requestedBy;

    /**
     * 发布说明。
     */
    private String description;

    /**
     * 触发时间。
     */
    private Instant requestedAt;

    /**
     * 来源事件资源名称。
     */
    private String sourceEventName;
}
