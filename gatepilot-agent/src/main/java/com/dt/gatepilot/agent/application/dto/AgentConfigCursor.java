package com.dt.gatepilot.agent.application.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * agent 拉取配置时携带的本地游标。
 */
@Data
public class AgentConfigCursor {

    /**
     * 节点命名空间。
     */
    private String namespace = "default";

    /**
     * 节点标识。
     */
    private String nodeId;

    /**
     * 节点所在可用区。
     */
    private String zone;

    /**
     * 节点隔离组。
     */
    private String isolationGroup;

    /**
     * 节点负责的配置分片。
     */
    private List<String> configShards = new ArrayList<>();

    /**
     * 当前配置版本。
     */
    private String currentVersion;

    /**
     * 当前配置序号。
     */
    private Long currentSequence;
}
