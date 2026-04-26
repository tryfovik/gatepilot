package com.dt.gatepilot.controller.application.service;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.node.GatewayNodeStatus;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.Objects;

/**
 * 节点应用状态聚合器。
 */
public class NodeApplyStatusAggregator {

    /**
     * 根据节点状态更新 PublishedConfig 应用结果。
     *
     * @param publishedConfig 已发布配置
     * @param nodes 目标节点
     */
    public void aggregate(PublishedConfig publishedConfig, Iterable<GatewayNode> nodes) {
        int desired = 0;
        int applied = 0;
        int failed = 0;
        publishedConfig.getStatus().getNodeApplyResults().clear();
        for (GatewayNode node : nodes) {
            desired++;
            PublishedConfig.NodeApplyResult result = nodeResult(node);
            result.setState(nodeApplyState(publishedConfig, node));
            if (result.getState() == ConfigApplyState.APPLIED) {
                applied++;
            }
            if (result.getState() == ConfigApplyState.FAILED) {
                failed++;
            }
            publishedConfig.getStatus().getNodeApplyResults().add(result);
        }
        publishedConfig.getStatus().setDesiredNodeCount(desired);
        publishedConfig.getStatus().setAppliedNodeCount(applied);
        publishedConfig.getStatus().setFailedNodeCount(failed);
        if (failed > 0) {
            publishedConfig.getStatus().setApplyState(ConfigApplyState.FAILED);
        } else if (desired > 0 && applied == desired) {
            publishedConfig.getStatus().setApplyState(ConfigApplyState.APPLIED);
        } else {
            publishedConfig.getStatus().setApplyState(ConfigApplyState.PENDING);
        }
    }

    private PublishedConfig.NodeApplyResult nodeResult(GatewayNode node) {
        PublishedConfig.NodeApplyResult result = new PublishedConfig.NodeApplyResult();
        result.setNodeId(node.getSpec().getNodeId());
        result.setNodeRef(nodeRef(node));
        result.setZone(node.getSpec().getZone());
        result.setAppliedVersion(node.getStatus().getCurrentConfigVersion());
        result.setConfigHash(node.getStatus().getLastApplyResult().getConfigHash());
        result.setAppliedAt(node.getStatus().getLastApplyResult().getFinishedAt());
        result.setReason(node.getStatus().getLastApplyResult().getReason());
        result.setMessage(node.getStatus().getLastApplyResult().getMessage());
        return result;
    }

    /**
     * 计算节点相对当前 PublishedConfig 的应用状态
     *
     * @param publishedConfig 已发布配置
     * @param node 网关节点
     * @return 应用状态
     */
    private ConfigApplyState nodeApplyState(PublishedConfig publishedConfig, GatewayNode node) {
        String targetVersion = publishedConfig.getSpec().getVersion();
        GatewayNodeStatus status = node.getStatus();
        GatewayNodeStatus.ApplyResult applyResult = status.getLastApplyResult();
        if (Objects.equals(targetVersion, status.getCurrentConfigVersion())
                && status.getApplyState() == ConfigApplyState.APPLIED) {
            return ConfigApplyState.APPLIED;
        }
        if (Objects.equals(targetVersion, applyResult.getVersion())) {
            ConfigApplyState state = applyResult.getState() == null ? status.getApplyState() : applyResult.getState();
            return Objects.requireNonNullElse(state, ConfigApplyState.PENDING);
        }
        // 节点还停留在旧版本时，当前发布仍然是等待应用
        return ConfigApplyState.PENDING;
    }

    private ResourceReference nodeRef(GatewayNode node) {
        ResourceReference reference = new ResourceReference();
        reference.setKind(com.dt.gatepilot.domain.enums.ResourceKind.GATEWAY_NODE);
        reference.setNamespace(node.getMetadata().getNamespace());
        reference.setName(node.getMetadata().getName());
        reference.setUid(node.getMetadata().getUid());
        return reference;
    }
}
