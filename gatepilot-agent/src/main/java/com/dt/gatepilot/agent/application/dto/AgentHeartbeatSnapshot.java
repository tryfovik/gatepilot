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

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.enums.NodePhase;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import com.dt.gatepilot.domain.resource.node.GatewayNodeStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * agent 心跳快照。
 */
@Data
public class AgentHeartbeatSnapshot {

    /**
     * 节点命名空间。
     */
    private String namespace = ResourceMetadataConstants.DEFAULT_NAMESPACE;

    /**
     * 节点标识。
     */
    private String nodeId;

    /**
     * 节点状态。
     */
    private NodePhase nodePhase;

    /**
     * 当前配置版本。
     */
    private String currentConfigVersion;

    /**
     * 期望配置版本。
     */
    private String desiredConfigVersion;

    /**
     * last-good 配置版本。
     */
    private String lastGoodConfigVersion;

    /**
     * 配置应用状态。
     */
    private ConfigApplyState applyState;

    /**
     * 当前加载路由数量。
     */
    private Integer loadedRouteCount;

    /**
     * 当前加载上游数量。
     */
    private Integer loadedUpstreamCount;

    /**
     * 当前加载策略数量。
     */
    private Integer loadedPolicyCount;

    /**
     * 节点健康摘要。
     */
    private GatewayNodeStatus.NodeHealth health = new GatewayNodeStatus.NodeHealth();

    /**
     * 节点指标摘要。
     */
    private GatewayNodeStatus.MetricsSummary metricsSummary = new GatewayNodeStatus.MetricsSummary();

    /**
     * 上游健康摘要。
     */
    private List<GatewayNodeStatus.UpstreamHealth> upstreamHealth = new ArrayList<>();

    /**
     * 自定义指标摘要。
     */
    private Map<String, String> metrics = new LinkedHashMap<>();
}
