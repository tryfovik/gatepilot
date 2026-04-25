package com.dt.gatepilot.apiserver.application.command;

import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * agent 拉取已发布配置请求。
 */
@Data
public class PullAgentConfigCommand {

    /**
     * 节点命名空间。
     */
    private String namespace = ResourceMetadataConstants.DEFAULT_NAMESPACE;

    /**
     * 节点标识。
     */
    @NotBlank
    private String nodeId;

    /**
     * 节点所在可用区或机房。
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
     * 节点当前配置版本。
     */
    private String currentVersion;

    /**
     * 节点当前配置序号。
     */
    private Long currentSequence;
}
