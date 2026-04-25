package com.dt.gatepilot.apiserver.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 控制面回滚请求。
 */
@Data
public class CreateRollbackCommand {

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
     * 要回滚到的目标版本。
     */
    @NotBlank
    private String targetVersion;

    /**
     * 配置分片键。
     */
    private String configShard;

    /**
     * 回滚说明。
     */
    private String description;

    /**
     * 操作人。
     */
    private String createdBy;
}
