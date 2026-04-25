package com.dt.gatepilot.apiserver.application.command;

import com.dt.gatepilot.domain.resource.common.LabelSelector;
import com.dt.gatepilot.domain.resource.common.ResourceReference;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 控制面发布请求。
 */
@Data
public class CreateReleaseCommand {

    /**
     * 命名空间。
     */
    @NotBlank
    private String namespace;

    /**
     * 项目名称。
     */
    @NotBlank
    private String projectName;

    /**
     * 配置分片键。
     */
    private String configShard;

    /**
     * 发布说明。
     */
    private String description;

    /**
     * 创建人。
     */
    private String createdBy;

    /**
     * 本次发布涉及的资源。
     */
    private List<ResourceReference> resourceRefs = new ArrayList<>();

    /**
     * 目标节点选择器。
     */
    private LabelSelector targetNodeSelector;
}
