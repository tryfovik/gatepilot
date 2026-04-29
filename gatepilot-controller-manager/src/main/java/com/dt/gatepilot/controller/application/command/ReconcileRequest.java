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
     * 回滚目标版本。
     */
    private String targetVersion;

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
