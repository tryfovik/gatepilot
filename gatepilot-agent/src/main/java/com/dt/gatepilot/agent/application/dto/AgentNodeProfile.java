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

import com.dt.gatepilot.domain.enums.NodeRole;
import com.dt.gatepilot.domain.resource.meta.LabelSelector;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * agent 节点身份信息。
 */
@Data
public class AgentNodeProfile {

    /**
     * 节点命名空间。
     */
    private String namespace = ResourceMetadataConstants.DEFAULT_NAMESPACE;

    /**
     * 节点标识。
     */
    private String nodeId;

    /**
     * 节点角色。
     */
    private NodeRole role = NodeRole.COMBINED;

    /**
     * 可用区或机房。
     */
    private String zone;

    /**
     * 隔离组。
     */
    private String isolationGroup;

    /**
     * 节点地址。
     */
    private String address;

    /**
     * agent 版本。
     */
    private String agentVersion;

    /**
     * proxy 版本。
     */
    private String proxyVersion;

    /**
     * 节点负责消费的配置分片。
     */
    private List<String> configShards = new ArrayList<>();

    /**
     * 节点可服务的项目选择器。
     */
    private LabelSelector projectSelector;

    /**
     * 节点能力。
     */
    private Map<String, String> capabilities = new LinkedHashMap<>();
}
