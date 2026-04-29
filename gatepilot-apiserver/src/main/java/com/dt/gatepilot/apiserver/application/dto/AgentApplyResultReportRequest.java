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

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import lombok.Data;

/**
 * agent 上报配置应用结果请求。
 */
@Data
public class AgentApplyResultReportRequest {

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
