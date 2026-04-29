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
package com.dt.gatepilot.apiserver.application.dto;

import java.time.Instant;
import lombok.Data;

/**
 * 配置版本快照摘要响应。
 */
@Data
public class ConfigSnapshotSummaryResponse {

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 项目名称。
     */
    private String projectName;

    /**
     * 发布版本。
     */
    private String version;

    /**
     * 配置哈希。
     */
    private String configHash;

    /**
     * 配置分片。
     */
    private String configShard;

    /**
     * 配置序号。
     */
    private Long sequence;

    /**
     * 发布请求标识。
     */
    private String releaseId;

    /**
     * 路由数量。
     */
    private int routeCount;

    /**
     * 上游数量。
     */
    private int upstreamCount;

    /**
     * 策略数量。
     */
    private int policyCount;

    /**
     * 目标节点数量。
     */
    private int targetNodeCount;

    /**
     * 快照采集时间。
     */
    private Instant capturedAt;

    /**
     * 快照创建人。
     */
    private String capturedBy;

    /**
     * 快照说明。
     */
    private String description;

    /**
     * 是否允许回滚。
     */
    private Boolean rollbackAllowed;

    /**
     * 最近一次回滚时间。
     */
    private Instant lastRollbackAt;
}
