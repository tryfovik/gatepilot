package com.dt.gatepilot.agent.application.dto;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import java.time.Instant;
import lombok.Data;

/**
 * agent 配置应用结果。
 */
@Data
public class AgentApplyResult {

    /**
     * 节点命名空间。
     */
    private String namespace = "default";

    /**
     * 节点标识。
     */
    private String nodeId;

    /**
     * 发布版本。
     */
    private String version;

    /**
     * 配置内容哈希。
     */
    private String configHash;

    /**
     * 应用状态。
     */
    private ConfigApplyState state;

    /**
     * 开始应用时间。
     */
    private Instant startedAt;

    /**
     * 完成应用时间。
     */
    private Instant finishedAt;

    /**
     * 失败原因码。
     */
    private String reason;

    /**
     * 失败或结果说明。
     */
    private String message;
}
