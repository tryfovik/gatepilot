/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
