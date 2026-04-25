package com.dt.gatepilot.apiserver.application.dto;

import java.time.Instant;
import lombok.Data;

/**
 * 发布请求响应。
 */
@Data
public class ReleaseResult {

    /**
     * 发布请求标识。
     */
    private String releaseId;

    /**
     * 目标发布版本。
     */
    private String version;

    /**
     * 发布状态。
     */
    private String phase;

    /**
     * 配置分片键。
     */
    private String configShard;

    /**
     * 创建时间。
     */
    private Instant createdAt;
}
