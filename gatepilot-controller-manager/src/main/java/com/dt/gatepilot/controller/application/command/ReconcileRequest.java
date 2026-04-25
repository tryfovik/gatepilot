package com.dt.gatepilot.controller.application.command;

import java.time.Instant;
import lombok.Data;

/**
 * 发布 reconcile 请求。
 */
@Data
public class ReconcileRequest {

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 项目名称。
     */
    private String projectName;

    /**
     * 配置分片键。
     */
    private String configShard;

    /**
     * 目标发布版本。
     */
    private String version;

    /**
     * 同一分片内单调递增序号。
     */
    private Long sequence;

    /**
     * 触发来源，例如 publish、rollback、resync。
     */
    private String trigger;

    /**
     * 触发人。
     */
    private String requestedBy;

    /**
     * 触发时间。
     */
    private Instant requestedAt;
}
