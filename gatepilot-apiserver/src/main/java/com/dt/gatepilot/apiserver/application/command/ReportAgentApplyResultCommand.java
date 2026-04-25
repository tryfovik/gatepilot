package com.dt.gatepilot.apiserver.application.command;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import lombok.Data;

/**
 * agent 上报配置应用结果请求。
 */
@Data
public class ReportAgentApplyResultCommand {

    /**
     * 节点命名空间。
     */
    private String namespace = "default";

    /**
     * 节点标识。
     */
    @NotBlank
    private String nodeId;

    /**
     * 发布版本。
     */
    @NotBlank
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
