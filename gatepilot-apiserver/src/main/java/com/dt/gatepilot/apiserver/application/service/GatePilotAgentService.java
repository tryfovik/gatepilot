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
package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.apiserver.application.dto.AgentApplyResultReportRequest;
import com.dt.gatepilot.apiserver.application.dto.AgentConfigPullRequest;
import com.dt.gatepilot.apiserver.application.dto.AgentConfigPullResponse;
import com.dt.gatepilot.apiserver.application.dto.AgentHeartbeatRequest;
import com.dt.gatepilot.apiserver.application.dto.RegisterAgentRequest;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourcePaths;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceType;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.enums.NodePhase;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.node.GatewayNodeStatus;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * agent 协议服务。
 */
@Service
@ConditionalOnGatePilotApiserverEnabled
public class GatePilotAgentService {

    private final GatePilotResourceService resourceService;

    /**
     * 创建 agent 协议服务。
     *
     * @param resourceService 资源服务
     */
    public GatePilotAgentService(GatePilotResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 注册节点。
     *
     * @param request 注册请求
     * @return 节点资源
     */
    public GatewayNode register(RegisterAgentRequest request) {
        GatewayNode node = new GatewayNode();
        // 节点资源名和 nodeId 保持一致，便于控制台排查
        node.getMetadata().setName(request.getNodeId());
        node.getMetadata().setNamespace(request.getNamespace());
        node.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_NODE_ID, request.getNodeId());
        node.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_ROLE, request.getRole().name());
        node.getSpec().setNodeId(request.getNodeId());
        node.getSpec().setRole(request.getRole());
        node.getSpec().setZone(request.getZone());
        node.getSpec().setIsolationGroup(request.getIsolationGroup());
        node.getSpec().setAddress(request.getAddress());
        node.getSpec().setAgentVersion(request.getAgentVersion());
        node.getSpec().setProxyVersion(request.getProxyVersion());
        node.getSpec().setConfigShards(request.getConfigShards());
        node.getSpec().setProjectSelector(request.getProjectSelector());
        node.getSpec().setCapabilities(request.getCapabilities());
        node.getStatus().setNodePhase(NodePhase.REGISTERED);
        node.getStatus().setLastHeartbeatAt(Instant.now());
        GatePilotResourceType nodeType = resourceService.requireResourceType(ResourceKind.GATEWAY_NODE);
        return (GatewayNode) resourceService.save(nodeType, request.getNamespace(), request.getNodeId(), node);
    }

    /**
     * 处理节点心跳。
     *
     * @param request 心跳请求
     * @return 节点资源
     */
    public GatewayNode heartbeat(AgentHeartbeatRequest request) {
        GatewayNode node = loadOrCreateNode(request.getNamespace(), request.getNodeId());
        GatewayNodeStatus status = node.getStatus();
        // 心跳以最新快照覆盖节点状态
        status.setNodePhase(Objects.requireNonNullElse(request.getNodePhase(), NodePhase.READY));
        status.setLastHeartbeatAt(Instant.now());
        status.setCurrentConfigVersion(request.getCurrentConfigVersion());
        status.setDesiredConfigVersion(request.getDesiredConfigVersion());
        status.setLastGoodConfigVersion(request.getLastGoodConfigVersion());
        status.setApplyState(request.getApplyState());
        status.setLoadedRouteCount(request.getLoadedRouteCount());
        status.setLoadedUpstreamCount(request.getLoadedUpstreamCount());
        status.setLoadedPolicyCount(request.getLoadedPolicyCount());
        if (request.getHealth() != null) {
            status.setHealth(request.getHealth());
        }
        if (request.getMetricsSummary() != null) {
            status.setMetrics(request.getMetricsSummary());
        }
        status.getMetrics().getCustom().putAll(request.getMetrics());
        status.setUpstreamHealth(request.getUpstreamHealth());
        GatePilotResourceType nodeType = resourceService.requireResourceType(ResourceKind.GATEWAY_NODE);
        return (GatewayNode) resourceService.save(nodeType, request.getNamespace(), request.getNodeId(), node);
    }

    /**
     * 拉取最新已发布配置。
     *
     * @param request 拉取请求
     * @return 拉取响应
     */
    public AgentConfigPullResponse pullConfig(AgentConfigPullRequest request) {
        PublishedConfig latest = listPublishedConfigs(request.getNamespace())
                .stream()
                .filter(config -> request.getConfigShards().isEmpty()
                        || request.getConfigShards().contains(config.getSpec().getConfigShard()))
                .filter(config -> isolationGroupMatches(config, request))
                .max(Comparator.comparing(config -> Objects.requireNonNullElse(config.getSpec().getSequence(), 0L)))
                .orElse(null);
        AgentConfigPullResponse response = new AgentConfigPullResponse();
        if (latest == null) {
            // 没有目标配置时让 agent 保持当前 last-good
            response.setChanged(false);
            response.setMessage(GatePilotAgentConstants.MESSAGE_NO_PUBLISHED_CONFIG);
            return response;
        }
        boolean changed = !Objects.equals(latest.getSpec().getVersion(), request.getCurrentVersion())
                || Objects.requireNonNullElse(latest.getSpec().getSequence(), 0L)
                > Objects.requireNonNullElse(request.getCurrentSequence(), 0L);
        response.setChanged(changed);
        response.setPublishedConfig(changed ? latest : null);
        response.setMessage(changed
                ? GatePilotAgentConstants.MESSAGE_CONFIG_CHANGED
                : GatePilotAgentConstants.MESSAGE_CONFIG_NOT_CHANGED);
        return response;
    }

    /**
     * 上报配置应用结果。
     *
     * @param request 上报请求
     * @return 节点资源
     */
    public GatewayNode reportApplyResult(AgentApplyResultReportRequest request) {
        GatewayNode node = loadOrCreateNode(request.getNamespace(), request.getNodeId());
        GatewayNodeStatus status = node.getStatus();
        // agent 上报的 apply 结果是节点状态的事实来源
        status.setApplyState(Objects.requireNonNullElse(request.getState(), ConfigApplyState.APPLIED));
        status.setDesiredConfigVersion(request.getVersion());
        status.setLastHeartbeatAt(Instant.now());
        GatewayNodeStatus.ApplyResult result = status.getLastApplyResult();
        result.setVersion(request.getVersion());
        result.setConfigHash(request.getConfigHash());
        result.setState(status.getApplyState());
        result.setStartedAt(request.getStartedAt());
        result.setFinishedAt(request.getFinishedAt());
        result.setReason(request.getReason());
        result.setMessage(request.getMessage());
        if (status.getApplyState() == ConfigApplyState.APPLIED) {
            status.setCurrentConfigVersion(request.getVersion());
            status.setLastGoodConfigVersion(request.getVersion());
        }
        GatePilotResourceType nodeType = resourceService.requireResourceType(ResourceKind.GATEWAY_NODE);
        return (GatewayNode) resourceService.save(nodeType, request.getNamespace(), request.getNodeId(), node);
    }

    private List<PublishedConfig> listPublishedConfigs(String namespace) {
        List<PublishedConfig> configs = new ArrayList<>();
        String cursor = null;
        do {
            CursorPage<Object> page = resourceService.list(GatePilotResourcePaths.PUBLISHED_CONFIGS,
                    namespace, cursor, GatePilotAgentConstants.PUBLISHED_CONFIG_PAGE_LIMIT);
            // agent 拉取必须跨页扫描，避免 1000 项目后漏掉后半段发布配置
            page.getItems()
                    .stream()
                    .map(PublishedConfig.class::cast)
                    .forEach(configs::add);
            cursor = page.getNextCursor();
        } while (StringUtils.hasText(cursor));
        return configs;
    }

    private boolean isolationGroupMatches(PublishedConfig config, AgentConfigPullRequest request) {
        String targetIsolationGroup = config.getSpec().getIsolationGroup();
        return !StringUtils.hasText(targetIsolationGroup)
                || Objects.equals(targetIsolationGroup, request.getIsolationGroup());
    }

    private GatewayNode loadOrCreateNode(String namespace, String nodeId) {
        try {
            return (GatewayNode) resourceService.get(GatePilotResourcePaths.NODES, namespace, nodeId);
        } catch (RuntimeException exception) {
            // 心跳先到时自动补节点，避免部署顺序影响状态上报
            RegisterAgentRequest request = new RegisterAgentRequest();
            request.setNamespace(namespace);
            request.setNodeId(nodeId);
            return register(request);
        }
    }
}
