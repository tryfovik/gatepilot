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
package com.dt.gatepilot.agent.application.dto;

import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
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
    private String namespace = ResourceMetadataConstants.DEFAULT_NAMESPACE;

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
