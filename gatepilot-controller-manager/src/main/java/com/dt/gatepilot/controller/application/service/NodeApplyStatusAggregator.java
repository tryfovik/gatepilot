package com.dt.gatepilot.controller.application.service;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.resource.common.ResourceReference;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
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
        result.setState(Objects.requireNonNullElse(node.getStatus().getApplyState(), ConfigApplyState.PENDING));
        result.setAppliedVersion(node.getStatus().getCurrentConfigVersion());
        result.setConfigHash(node.getStatus().getLastApplyResult().getConfigHash());
        result.setAppliedAt(node.getStatus().getLastApplyResult().getFinishedAt());
        result.setReason(node.getStatus().getLastApplyResult().getReason());
        result.setMessage(node.getStatus().getLastApplyResult().getMessage());
        return result;
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
