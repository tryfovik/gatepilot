package com.dt.gatepilot.agent.application.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * agent 运行审计批次。
 */
@Data
public class AgentRuntimeAuditBatch {

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 节点标识。
     */
    private String nodeId;

    /**
     * 审计事件列表。
     */
    private List<AgentRuntimeAuditEvent> events = new ArrayList<>();
}
